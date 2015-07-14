package hu.rycus.rpiomxremote.manager.v2;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGetHC4;
import org.apache.http.client.methods.HttpPostHC4;
import org.apache.http.client.methods.HttpRequestBaseHC4;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntityHC4;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import hu.rycus.rpiomxremote.RemoteService;
import hu.rycus.rpiomxremote.blocks.FileList;
import hu.rycus.rpiomxremote.blocks.Setting;
import hu.rycus.rpiomxremote.blocks.SubtitleItem;
import hu.rycus.rpiomxremote.blocks.SubtitleMetadata;
import hu.rycus.rpiomxremote.manager.PlayerState;
import hu.rycus.rpiomxremote.manager.RemoteManager;
import hu.rycus.rpiomxremote.ui.NotificationHelper;
import hu.rycus.rpiomxremote.util.Intents;


@SuppressWarnings("deprecation")
public class RestRemoteManager implements RemoteManager, Runnable {

    private static final String LOG_TAG = RestRemoteManager.class.getSimpleName();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String NETWORK_SECRET = "RPi::omxremote|v2";

    private static RestRemoteManager INSTANCE = null;

    private static final String MCAST_GROUP = "224.1.1.7";
    private static final int MCAST_PORT = 42002;

    private final RemoteService service;

    /** The UDP/Multicast socket used for communication. */
    private MulticastSocket socket;
    /** The target HTTP host for remote calls. */
    private HttpHost httpHost;

    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private final AtomicReference<PlayerState> playerState = new AtomicReference<>();

    private Thread playerStateRefreshThread = null;

    private RestRemoteManager(final RemoteService service) {
        this.service = service;
    }

    public static synchronized RemoteManager start(final RemoteService service) {
        if (INSTANCE != null) {
            INSTANCE.shutdown();
        }

        INSTANCE = new RestRemoteManager(service);
        INSTANCE.startDiscovery();
        return INSTANCE;
    }

    private void startDiscovery() {
        final Thread discoveryThread = new Thread(this, "Discovery");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    @Override
    public void run() {
        if (!initMulticastSocket()) {
            Log.e(LOG_TAG, "Failed to initialize multicast socket");
            return;
        }

        while (enabled.get()) {
            final long start = System.nanoTime();

            try {
                final byte[] message = NETWORK_SECRET.getBytes();
                final InetSocketAddress mcastAddress = new InetSocketAddress(MCAST_GROUP, MCAST_PORT);
                final DatagramPacket sendPacket = new DatagramPacket(message, message.length, mcastAddress);
                socket.send(sendPacket);

                final DatagramPacket receivePacket = new DatagramPacket(new byte[5], 5);
                socket.receive(receivePacket);

                final String response = new String(receivePacket.getData(), 0, 5);
                final String host = receivePacket.getAddress().getHostAddress();
                final int port = Integer.parseInt(response);

                httpHost = new HttpHost(host, port);

                setConnected(true);
                continue;
            } catch(SocketTimeoutException ex) {
                Log.d(LOG_TAG, "Datagram socket timeout");
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to send datagram packet", ex);
            } finally {
                synchronized (this) {
                    try {
                        final long end = System.nanoTime();
                        final long wait =
                                TimeUnit.SECONDS.toMillis(5) - TimeUnit.NANOSECONDS.toMillis(end - start);

                        wait(Math.max(1, wait));
                    } catch (InterruptedException ex) {
                        Log.d(LOG_TAG, "Sleep interrupted", ex);
                    }
                }
            }

            setConnected(false);
        }
    }

    private boolean initMulticastSocket() {
        try {
            socket = new MulticastSocket(MCAST_PORT);
            socket.setTimeToLive(4);
            socket.setSoTimeout(5000);
            socket.setLoopbackMode(true);
        } catch(Exception ex) {
            Log.e(LOG_TAG, "Failed to start multicast socket on port " + MCAST_PORT, ex);
        }

        if(socket != null) {
            try {
                socket.joinGroup(InetAddress.getByName(MCAST_GROUP));
                return true;
            } catch(Exception ex) {
                Log.e(LOG_TAG, "Failed to join multicast group at " + MCAST_GROUP, ex);
            }
        }

        return false;
    }

    private void setConnected(final boolean connected) {
        if (this.connected.compareAndSet(!connected, connected)) {
            Intent intent = new Intent(Intents.ACTION_CALLBACK);
            intent.putExtra(Intents.EXTRA_CONNECTION_STATE, connected);
            LocalBroadcastManager.getInstance(service).sendBroadcast(intent);

            if (connected) {
                checkPlayerState(false);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void shutdown() {
        enabled.set(false);
    }

    @Override
    public PlayerState getPlayerState() {
        return playerState.get();
    }

    @Override
    public void listFiles(final String path) {
        new RestTask<FileList>() {
            @Override
            protected HttpRequestBaseHC4 prepareRequest() throws Exception {
                final String uri = httpHost.toURI().concat("/list/files");
                final HttpPostHC4 request = new HttpPostHC4(uri);
                request.setEntity(new StringEntityHC4(
                        Json.start("path", path).build(),
                        ContentType.APPLICATION_JSON));
                return request;
            }

            @Override
            protected FileList parse(final HttpResponse response) throws Exception {
                return MAPPER.readValue(response.getEntity().getContent(), FileList.class);
            }

            @Override
            protected void onPostExecute(final FileList fileList) {
                Intent response = new Intent(Intents.ACTION_CALLBACK);
                response.putExtra(Intents.EXTRA_FILE_LIST, fileList);
                LocalBroadcastManager.getInstance(service).sendBroadcast(response);
            }
        }.execute();
    }

    @Override
    public void startPlayer(final String videoPath, final String subtitlePath) {
        new RestTask<PlayerState>() {
            @Override
            protected HttpRequestBaseHC4 prepareRequest() throws Exception {
                final String uri = httpHost.toURI().concat("/player/start");
                final HttpPostHC4 request = new HttpPostHC4(uri);
                request.setEntity(new StringEntityHC4(
                        Json.start("video", videoPath)
                                .put("subtitle", subtitlePath)
                                .build(),
                        ContentType.APPLICATION_JSON));
                return request;
            }

            @Override
            protected PlayerState parse(final HttpResponse response) throws Exception {
                final PlayerState state =
                        MAPPER.readValue(response.getEntity().getContent(), PlayerState.class);

                state.processExtras(service);

                return state;
            }

            @Override
            protected void onPostExecute(final PlayerState state) {
                if (state != null) {
                    onNewPlayerStarted(state);
                } else {
                    Log.e(LOG_TAG, String.format("Failed to start video at %s", videoPath));
                }
            }
        }.execute();
    }

    @Override
    public void stopPlayer() {
        new RestTask<Boolean>() {
            @Override
            protected HttpRequestBaseHC4 prepareRequest() throws Exception {
                final String uri = httpHost.toURI().concat("/player/stop");
                return new HttpPostHC4(uri);
            }

            @Override
            protected Boolean parse(final HttpResponse response) throws Exception {
                return response.getStatusLine().getStatusCode() / 100 == 2;
            }

            @Override
            protected void onPostExecute(final Boolean aBoolean) {
                onPlayerStopped();
            }
        }.execute();
    }

    private void onPlayerStopped() {
        final boolean wasPlaying = playerState.get() != null;

        playerState.set(null);

        if (wasPlaying) {
            NotificationHelper.cancel(service);

            Intent response = new Intent(Intents.ACTION_CALLBACK);
            response.putExtra(Intents.EXTRA_PLAYER_REPORT, Intents.EXTRA_PLAYER_REPORT_EXIT);
            LocalBroadcastManager.getInstance(service).sendBroadcast(response);
        }
    }

    @Override
    public void requestSettings() {
        new RestTask<Setting[]>() {
            @Override
            protected HttpRequestBaseHC4 prepareRequest() throws Exception {
                final String uri = httpHost.toURI().concat("/list/settings");
                return new HttpGetHC4(uri);
            }

            @Override
            protected Setting[] parse(final HttpResponse response) throws Exception {
                return MAPPER.readValue(response.getEntity().getContent(), Setting[].class);
            }

            @Override
            protected void onPostExecute(final Setting[] settings) {
                Intent response = new Intent(Intents.ACTION_CALLBACK);
                response.putExtra(Intents.EXTRA_SETTINGS_LIST, settings);
                LocalBroadcastManager.getInstance(service).sendBroadcast(response);
            }
        }.execute();
    }

    @Override
    public void setSetting(final String key, final String value) {
        new RestTask<Void>() {
            @Override
            protected HttpRequestBaseHC4 prepareRequest() throws Exception {
                final String uri = httpHost.toURI().concat("/set/setting");
                final HttpPostHC4 request = new HttpPostHC4(uri);
                request.setEntity(new StringEntityHC4(
                        Json.start("key", key)
                                .put("value", value)
                                .build(),
                        ContentType.APPLICATION_JSON));
                return request;
            }
        }.execute();
    }

    @Override
    public void ctrlPlayPause() {
        new ControlTask("play/pause").execute();
    }

    @Override
    public void ctrlVolume(final long value) {
        new ControlTask("volume", Long.toString(value)).execute();
    }

    @Override
    public void ctrlSeek(final long position) {
        new ControlTask("seek", Long.toString(position)).execute();
    }

    @Override
    public void ctrlIncreaseSpeed() {
        new ControlTask("speed/inc").execute();
    }

    @Override
    public void ctrlDecreaseSpeed() {
        new ControlTask("speed/dec").execute();
    }

    @Override
    public void ctrlIncreaseSubtitleDelay() {
        new ControlTask("subtitle/delay/inc").execute();
    }

    @Override
    public void ctrlDecreaseSubtitleDelay() {
        new ControlTask("subtitle/delay/dec").execute();
    }

    @Override
    public void ctrlToggleSubtitleVisibility() {
        new ControlTask("subtitle/toggle").execute();
    }

    @Override
    public void loadSubtitleMetadata(final String filename, final SubtitleMetadataCallback callback) {
        new RestTask<SubtitleMetadata>() {
            @Override
            protected HttpRequestBaseHC4 prepareRequest() throws Exception {
                final String uri = httpHost.toURI().concat("/subtitles/metadata");
                final HttpPostHC4 request = new HttpPostHC4(uri);
                request.setEntity(new StringEntityHC4(
                        Json.start("filename", filename).build(),
                        ContentType.APPLICATION_JSON));
                return request;
            }

            @Override
            protected SubtitleMetadata parse(final HttpResponse response) throws Exception {
                return MAPPER.readValue(response.getEntity().getContent(), SubtitleMetadata.class);
            }

            @Override
            protected void onPostExecute(final SubtitleMetadata metadata) {
                callback.onMetadataReceived(metadata);
            }
        }.execute();
    }

    @Override
    public void querySubtitles(final String provider, final String query,
                               final SubtitleQueryCallback callback) {
        new RestTask<SubtitleItem>() {
            @Override
            protected HttpRequestBaseHC4 prepareRequest() throws Exception {
                final String uri = httpHost.toURI().concat("/subtitles/query");
                final HttpPostHC4 request = new HttpPostHC4(uri);
                request.setEntity(new StringEntityHC4(
                        Json.start("provider", provider)
                                .put("query", query)
                                .build(),
                        ContentType.APPLICATION_JSON));
                return request;
            }

            @Override
            protected SubtitleItem parse(final HttpResponse response) throws Exception {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    try (final BufferedReader reader = wrapContent(response)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.trim().isEmpty()) {
                                final SubtitleItem item = MAPPER.readValue(line, SubtitleItem.class);
                                publishProgress(item);
                            }
                        }
                    }
                }

                return null;
            }

            private BufferedReader wrapContent(final HttpResponse response) throws IOException {
                return new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            }

            @Override
            protected void onProgressUpdate(final SubtitleItem... values) {
                for (final SubtitleItem item : values) {
                    callback.onItemReceived(item);
                }
            }
        }.execute();
    }

    @Override
    public void downloadSubtitle(final String provider, final String id, final String directory,
                                 final SubtitleDownloadCallback callback) {
        new RestTask<String>() {
            @Override
            protected HttpRequestBaseHC4 prepareRequest() throws Exception {
                final String uri = httpHost.toURI().concat("/subtitles/download");
                final HttpPostHC4 request = new HttpPostHC4(uri);
                request.setEntity(new StringEntityHC4(
                        Json.start("provider", provider)
                                .put("id", id)
                                .put("directory", directory)
                                .build(),
                        ContentType.APPLICATION_JSON));
                return request;
            }

            @Override
            protected String parse(final HttpResponse response) throws Exception {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    final JsonNode json = MAPPER.readTree(response.getEntity().getContent());
                    if (json != null) {
                        final JsonNode pathNode = json.get("path");
                        if (pathNode != null) {
                            return pathNode.asText();
                        }
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(final String result) {
                if (result != null) {
                    callback.onDownloaded(result);
                } else {
                    callback.onFailed();
                }
            }
        }.execute();
    }

    private PlayerState checkPlayerState(final boolean update) {
        try {
            return new RestTask<PlayerState>() {
                @Override
                protected HttpRequestBaseHC4 prepareRequest() throws Exception {
                    final String uri = httpHost.toURI().concat("/player/state");
                    return new HttpGetHC4(uri);
                }

                @Override
                protected PlayerState parse(final HttpResponse response) throws Exception {
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                        onPlayerStopped();
                    } else if (response.getStatusLine().getStatusCode() / 100 == 2) {
                        final PlayerState newState =
                                MAPPER.readValue(response.getEntity().getContent(), PlayerState.class);

                        if (!update) {
                            newState.processExtras(service);
                        }

                        return newState;
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(final PlayerState newState) {
                    if (newState != null) {
                        if (update) {
                            onPlayerStateUpdated(newState);
                        } else {
                            onNewPlayerStarted(newState);
                        }
                    }
                }

                @Override
                protected void onException(final Exception ex) {
                    onPlayerStopped();
                }
            }.execute().get(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            Log.w(LOG_TAG, "Failed to refresh player state", ex);
        }

        return null;
    }

    private void onNewPlayerStarted(final PlayerState newState) {
        if (playerState.compareAndSet(null, newState)) {
            final Intent response = new Intent(Intents.ACTION_CALLBACK);
            response.putExtra(Intents.EXTRA_PLAYER_REPORT, Intents.EXTRA_PLAYER_REPORT_INIT);
            LocalBroadcastManager.getInstance(service).sendBroadcast(response);

            NotificationHelper.postNotification(service, newState);

            startPlayerStateRefreshThread();
        }
    }

    private void onPlayerStateUpdated(final PlayerState newState) {
        final PlayerState state = playerState.get();

        final boolean stateUpdated = state.isPaused() != newState.isPaused();

        state.setPosition(newState.getPosition());
        state.setVolume(newState.getVolume());
        state.setPaused(newState.isPaused());

        Intent response = new Intent(Intents.ACTION_CALLBACK);
        response.putExtra(Intents.EXTRA_PLAYER_REPORT, Intents.EXTRA_PLAYER_REPORT_INFO);
        LocalBroadcastManager.getInstance(service).sendBroadcast(response);

        if (stateUpdated) {
            NotificationHelper.postNotification(service, state);
        }
    }

    private void startPlayerStateRefreshThread() {
        synchronized (RestRemoteManager.class) {
            if (playerStateRefreshThread != null) {
                Log.d(LOG_TAG, "Refresh thread is already running");
                return;
            }

            playerStateRefreshThread = new Thread("PlayerState") {
                @Override
                public void run() {
                    try {
                        refreshPlayerState();
                    } finally {
                        synchronized (RestRemoteManager.class) {
                            playerStateRefreshThread = null;
                        }
                    }
                }
            };
            playerStateRefreshThread.start();
        }
    }

    private void refreshPlayerState() {
        final PlayerState state = playerState.get();
        if (state == null) {
            return;
        }

        while (state == playerState.get()) {
            final long start = System.nanoTime();

            try {
                checkPlayerState(true);
            } finally {
                final long end = System.nanoTime();
                final long wait =
                        TimeUnit.SECONDS.toMillis(1) - TimeUnit.NANOSECONDS.toMillis(end - start);

                if (wait > 0) {
                    synchronized (this) {
                        try {
                            wait(wait);
                        } catch (InterruptedException ex) {
                            Log.d(LOG_TAG, "Waiting interrupted", ex);
                        }
                    }
                }
            }
        }
    }

    private abstract class RestTask<R> extends AsyncTask<Void, R, R> {

        protected abstract HttpRequestBaseHC4 prepareRequest() throws Exception;

        protected R parse(HttpResponse response) throws Exception {
            response.getEntity().consumeContent();
            return null;
        }

        protected void onException(Exception ex) { }

        @Override
        protected R doInBackground(final Void... params) {
            try {
                final HttpRequestBaseHC4 request = prepareRequest();
                request.setHeader("X-RPi-OmxPlayer", NETWORK_SECRET);

                final HttpContext context = new HttpClientContext();
                try (final CloseableHttpResponse response = httpClient.execute(request, context)) {
                    final R result = parse(response);
                    if (result != null) {
                        return result;
                    }
                }
            } catch (Exception ex) {
                Log.e(LOG_TAG, String.format("%s failed to load resource from server", getClass().getSimpleName()), ex);
                onException(ex);
            }

            return null;
        }

    }

    private class ControlTask extends RestTask<Void> {

        private final String command;
        private final String parameter;

        private ControlTask(final String command) {
            this(command, null);
        }

        private ControlTask(final String command, final String parameter) {
            this.command = command;
            this.parameter = parameter;
        }

        @Override
        protected HttpRequestBaseHC4 prepareRequest() throws Exception {
            final String uri = httpHost.toURI().concat(String.format("/player/ctrl/%s", command));
            final HttpPostHC4 request = new HttpPostHC4(uri);
            if (parameter != null) {
                request.setEntity(new StringEntityHC4(
                        Json.start("param", parameter).build(),
                        ContentType.APPLICATION_JSON));
            }
            return request;
        }

        @Override
        protected Void parse(final HttpResponse response) throws Exception {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                onPlayerStopped();
            }

            return null;
        }
    }

    private static class Json {

        private final Map<String, Object> values = new LinkedHashMap<>();

        private Json(final String key, final Object value) {
            put(key, value);
        }

        public static Json start(final String key, final Object value) {
            return new Json(key, value);
        }

        public Json put(final String key, final Object value) {
            values.put(key, value);
            return this;
        }

        public String build() throws JsonProcessingException {
            return MAPPER.writeValueAsString(values);
        }

    }

}
