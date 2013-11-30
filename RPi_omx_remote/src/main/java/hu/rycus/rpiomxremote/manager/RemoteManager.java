package hu.rycus.rpiomxremote.manager;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import hu.rycus.rpiomxremote.RemoteService;
import hu.rycus.rpiomxremote.blocks.FileList;
import hu.rycus.rpiomxremote.blocks.Setting;
import hu.rycus.rpiomxremote.ui.NotificationHelper;
import hu.rycus.rpiomxremote.util.Header;
import hu.rycus.rpiomxremote.util.Intents;

/**
 * Manager object responsible for controlling the remote server and handling responses from it.
 *
 * <br/>
 * Created by Viktor Adam on 10/30/13.
 *
 * @author rycus
 */
public class RemoteManager extends Thread {

    /** The current instance of the manager. */
    private static RemoteManager INSTANCE = null;

    /** Tag for logcat. */
    private static final String LOG_TAG = "RPiOMX|RM";

    /** The remote service that created this manager. */
    private final RemoteService remoteService;

    /** The network handler used for low-level communication with the server. */
    private NetworkHandler handler;

    /** The current player state if any. */
    private PlayerState playerState;

    /** Queue for commands to send to the server. */
    private final LinkedBlockingQueue<Command> queue = new LinkedBlockingQueue<Command>();

    /** Executor for various background tasks (like downloading posters). */
    private final ExecutorService executor;

    /** True until this manager instance is enabled. */
    private boolean enabled = true;
    /** True if this manager is connected and has an active session to the server. */
    private boolean connected = false;

    /**
     * Private constructor initiating and starting the manager.
     * @param service The remote service that created this manager
     */
    private RemoteManager(RemoteService service) {
        super("RemoteManager");
        this.remoteService = service;
        this.executor = Executors.newSingleThreadExecutor();
        this.start();
    }

    /**
     * Starts a manager instance (stopping the previous one if there was one.
     * @param service The remote service that creates a manager
     * @return The started manager instance
     */
    public static RemoteManager start(RemoteService service) {
        synchronized (RemoteManager.class) {
            if(INSTANCE != null) {
                INSTANCE.shutdown();
            }

            INSTANCE = new RemoteManager(service);
            Log.e(LOG_TAG, "RM.Instance: CREATE");
            return INSTANCE;
        }
    }

    /**
     * This code instantiates a low-level network handler, logs in then sends commands
     * to the remote server or a keep-alive message if no commands were enqueued.
     *
     * @see Runnable#run()
     */
    @Override
    public void run() {
        Set<Integer> asynchHeaders = new HashSet<Integer>(Arrays.asList(
                Header.MSG_A_KEEPALIVE,
                Header.MSG_A_START_VIDEO,
                Header.MSG_A_STOP_VIDEO,
                Header.MSG_A_PLAYER_STATE,
                Header.MSG_A_PLAYER_PARAMS,
                Header.MSG_A_PLAYER_INFO,
                Header.MSG_A_PLAYER_EXTRA
                ));

        handler = new NetworkHandler(this, asynchHeaders);
        try {
            if( handler.initialize() ) {
                Log.e(LOG_TAG, "Network handler initialized");
                handler.start();

                login();

                while(enabled && (INSTANCE == this)) {
                    try {
                        // TODO magic number
                        Command command = queue.poll(connected ? 7500 : 2500, TimeUnit.MILLISECONDS);
                        if(command != null) {
                            process(command);
                        } else {
                            handler.send(Header.MSG_A_KEEPALIVE);
                        }
                    } catch(Exception ex) {
                        Log.e(LOG_TAG, "Failed to process a command", ex);
                    }
                }
            }
        } finally {
            handler.shutdown();
            setConnected(false);
        }

        if(INSTANCE != this) {
            Log.e(LOG_TAG, "Remote manager differs from CURRENT_INSTANCE");
        }

        Log.e(LOG_TAG, "Remote manager stopped");
    }

    /** Stops this remote manager instance. */
    public void shutdown() {
        synchronized (RemoteManager.class) {
            enabled = false;
            executor.shutdown();
            queue.offer(new Command(Header.MSG_A_EXIT));

            if(INSTANCE == this) {
                INSTANCE = null;
                Log.e(LOG_TAG, "RM.Instance: null");
            }
        }
    }

    /** Returns true if this manager is connected and has an active session to the server. */
    public boolean isConnected() { return connected; }
    /** Set this true if this manager is connected and has an active session to the server. */
    void setConnected(boolean connected) {
        if(this.connected != connected) {
            Intent intent = new Intent(Intents.ACTION_CALLBACK);
            intent.putExtra(Intents.EXTRA_CONNECTION_STATE, connected);
            LocalBroadcastManager.getInstance(remoteService).sendBroadcast(intent);
        }

        this.connected = connected;
    }

    /** Returns the current state of the remote player. */
    public PlayerState getPlayerState() { return playerState; }

    /** Requests remote file list for the given path. */
    public void listFiles(String path) {
        queue.offer(new Command(Header.MSG_A_LIST_FILES, path));
    }

    /** Requests starting remote playback of a video with the given video and subtitle path. */
    public void startPlayer(String videoPath, String subtitlePath) {
        StringBuilder builder = new StringBuilder(videoPath);
        if(subtitlePath != null) {
            builder.append("|").append(subtitlePath);
        }

        queue.offer(new Command(Header.MSG_A_START_VIDEO, builder.toString()));
    }

    /** Requests stopping the remote player. */
    public void stopPlayer() {
        queue.offer(new Command(Header.MSG_A_STOP_VIDEO));
    }

    /** Requests sending a command to the remote player. */
    public void sendPlayerCommand(int command, String parameter) {
        queue.offer(new Command(command, parameter));
    }

    /** Requests remote settings from the server. */
    public void requestSettings() {
        queue.offer(new Command(Header.MSG_A_LIST_SETTINGS));
    }

    /** Requests modifying the value of a remote setting. */
    public void setSetting(String key, String value) {
        queue.offer(new Command(Header.MSG_A_SET_SETTING, key + "=" + value));
    }

    /**
     * Processes an enqueued command.
     * This usually means sending the command to the remote server,
     * processing the response and notifying the UI and/or the user.
     */
    private void process(Command command) {
        switch (command.getHeader()) {
            case Header.MSG_A_EXIT:
            {
                logoff();
                break;
            }
            case Header.MSG_A_LIST_FILES:
            {
                FileList list = requestFileList(command.getStringData());

                Intent response = new Intent(Intents.ACTION_CALLBACK);
                response.putExtra(Intents.EXTRA_FILE_LIST, list);
                LocalBroadcastManager.getInstance(remoteService).sendBroadcast(response);

                break;
            }
            case Header.MSG_A_LIST_SETTINGS:
            {
                Setting[] settings = listSettings();

                if(settings != null) {
                    Intent response = new Intent(Intents.ACTION_CALLBACK);
                    response.putExtra(Intents.EXTRA_SETTINGS_LIST, settings);
                    LocalBroadcastManager.getInstance(remoteService).sendBroadcast(response);
                }

                break;
            }
            case Header.MSG_A_SET_SETTING:
            {
                handler.send(Header.MSG_A_SET_SETTING, command.getStringData());
                break;
            }
            case Header.MSG_A_START_VIDEO:
            {
                startVideo(command.getStringData());
                break;
            }
            case Header.MSG_A_STOP_VIDEO:
            case Header.MSG_A_SEEK_TO:
            case Header.MSG_A_SET_VOLUME:
            case Header.MSG_A_PAUSE:
            case Header.MSG_A_SPEED_INC:
            case Header.MSG_A_SPEED_DEC:
            case Header.MSG_A_SUB_DELAY_INC:
            case Header.MSG_A_SUB_DELAY_DEC:
            case Header.MSG_A_SUB_TOGGLE:
            {
                handler.send(command.getHeader(), command.getData());
                break;
            }
            default:
                Log.e(LOG_TAG, "Unprocessed command received with header: 0x" + Integer.toHexString(command.getHeader()));
                break;
        }
    }

    /** Sends a remote login request to the server. */
    private void login() {
        if( handler.sendWithoutSession(Header.MSG_A_LOGIN, "RPi::omxremote") ) {
            Log.e(LOG_TAG, "Login sent");
        } else {
            Log.e(LOG_TAG, "Failed to send main request");
        }
    }

    /** Sends an exit message to the remote server. */
    private void logoff() {
        handler.send(Header.MSG_A_EXIT);
    }

    /** Sends a file list request to the server and parses the response into a FileList object. */
    private FileList requestFileList(String path) {
        if(path != null) {
            handler.send(Header.MSG_A_LIST_FILES, path);
        } else {
            handler.send(Header.MSG_A_LIST_FILES);
        }

        Packet response = handler.poll(15000L);
        if(response != null) {
            String[] parts = response.getData().split("\\|{2}");

            String root = parts[0];
            String[] files = parts.length > 1 ? parts[1].split("\\|") : new String[0];

            FileList list = new FileList(root, Arrays.asList(files));
            return list;
        }

        return null;
    }

    /**
     * Sends a request to the server to list settings and
     * parses the response into an array of Setting objects.
     */
    private Setting[] listSettings() {
        handler.send(Header.MSG_A_LIST_SETTINGS);

        Packet response = handler.poll(15000L);
        if(response != null) {
            List<Setting> settings = new LinkedList<Setting>();

            String[] parts = response.getData().split(";");
            for(int idx = 0; idx < parts.length / 5; idx++) {
                int offset = idx * 5;

                // response format: key;value;description;def.values;type

                String strType = parts[offset+4];
                Setting.Type type = Setting.Type.valueOf(strType);

                Setting setting = new Setting(parts[offset+0], parts[offset+1], parts[offset+2], parts[offset+3], type);
                settings.add(setting);
            }

            return settings.toArray(new Setting[0]);
        }

        return null;
    }

    /** Sends a start video request to the remote server. */
    private void startVideo(String parameter) {
        handler.send(Header.MSG_A_START_VIDEO, parameter);
    }

    /**
     * Processes an incoming packet that is to be
     * processed asynchronously (not a response of a command).
     */
    protected void processAsynchPacket(Packet packet) {
        switch (packet.getHeader()) {
            case Header.MSG_A_START_VIDEO:
            case Header.MSG_A_PLAYER_PARAMS:
            {
                String parameter = packet.getData();

                Intent response = new Intent(Intents.ACTION_CALLBACK);
                response.putExtra(Intents.EXTRA_PLAYER_REPORT, Intents.EXTRA_PLAYER_REPORT_INIT);

                // on initial starting of the playback or when the application restarts
                // the server sends a packet with information about the length of the video,
                // the initial volume of the player and the filename of the started video
                if(parameter != null) {
                    try {
                        String pattern = "d([0-9]+)v([0-9\\-]+)\\|(.*)";
                        long duration = Long.parseLong( parameter.replaceFirst(pattern, "$1") );
                        long volume   = Long.parseLong( parameter.replaceFirst(pattern, "$2") );
                        String fname  = parameter.replaceFirst(pattern, "$3");

                        playerState = new PlayerState(remoteService.getResources(), fname, duration, volume);

                        NotificationHelper.postNotification(remoteService, playerState);
                    } catch(Exception ex) {
                        ex.printStackTrace();

                        // TODO get error string from resources
                        response.putExtra(Intents.EXTRA_ERROR, "Failed to parse video state: " + parameter);
                    }
                } else {
                    // TODO get error string from resources
                    response.putExtra(Intents.EXTRA_ERROR, "Failed to start video");
                }

                LocalBroadcastManager.getInstance(remoteService).sendBroadcast(response);

                break;
            }
            case Header.MSG_A_STOP_VIDEO:
            {
                playerState = null;

                NotificationHelper.cancel(remoteService);

                Intent response = new Intent(Intents.ACTION_CALLBACK);
                response.putExtra(Intents.EXTRA_PLAYER_REPORT, Intents.EXTRA_PLAYER_REPORT_EXIT);
                LocalBroadcastManager.getInstance(remoteService).sendBroadcast(response);

                break;
            }
            case Header.MSG_A_PLAYER_STATE:
            {
                String parameter = packet.getData();

                Intent response = new Intent(Intents.ACTION_CALLBACK);
                response.putExtra(Intents.EXTRA_PLAYER_REPORT, Intents.EXTRA_PLAYER_REPORT_STATE);

                // when the remote player's state changes the server sends
                // the current playback position in milliseconds, the current volume
                // and whether the player is currently paused
                try {
                    String pattern = "p([0-9]+)v([0-9\\-]+)([PR])";
                    long position  = Long.parseLong( parameter.replaceFirst(pattern, "$1") );
                    long volume    = Long.parseLong( parameter.replaceFirst(pattern, "$2") );
                    boolean paused = "P".equals( parameter.replaceFirst(pattern, "$3") );

                    if(playerState != null) {
                        boolean notify = playerState.isPaused() != paused;
                        boolean refresh = notify && (playerState.isPaused() != paused);

                        playerState.setPosition(position);
                        playerState.setVolume(volume);
                        playerState.setPaused(paused);

                        if(notify) {
                            NotificationHelper.postNotification(remoteService, playerState);
                        }
                    }

                } catch(Exception ex) {
                    // TODO get error string from resources
                    response.putExtra(Intents.EXTRA_ERROR, "Failed to parse video state: " + parameter);
                }

                LocalBroadcastManager.getInstance(remoteService).sendBroadcast(response);

                break;
            }
            case Header.MSG_A_PLAYER_INFO:
            {
                String parameter = packet.getData();
                if(parameter == null || playerState == null) break;

                Intent response = new Intent(Intents.ACTION_CALLBACK);
                response.putExtra(Intents.EXTRA_PLAYER_REPORT, Intents.EXTRA_PLAYER_REPORT_INFO);

                try {

                    // if the remote server guessed the video as an episode of a show
                    // then it sends information about the guessed title, season and episode number
                    if(parameter.matches("SHOW\\$.*\\$.*")) {
                        String title = parameter.replaceFirst("SHOW\\$(.*)\\$.*", "$1");
                        String info  = parameter.replaceFirst("SHOW\\$.*\\$(.*)", "$1");

                        Integer season  = null;
                        Integer episode = null;

                        if(info.matches("S[0-9]+.*")) {
                            season  = Integer.parseInt( info.replaceFirst("S([0-9]+).*", "$1") );
                            info    = info.replaceFirst("S[0-9]+(.*)", "$1");
                        }
                        if(info.matches("E[0-9]+.*")) {
                            episode = Integer.parseInt( info.replaceFirst("E([0-9]+).*", "$1") );
                            info    = info.replaceFirst("E[0-9]+(.*)", "$1");
                        }

                        StringBuilder infoBuilder = new StringBuilder();
                        if(season != null) {
                            String season2char = (season < 10 ? "0" : "") + season;
                            infoBuilder.append("season ").append(season2char).append(" ");

                            playerState.getProperties().put(
                                    PlayerProperty.get(PlayerProperty.P_EPISODE_NUM_SEASON),
                                    season2char);
                        }
                        if(episode != null) {
                            String episode2char = (episode < 10 ? "0" : "") + episode;
                            infoBuilder.append("episode ").append(episode2char).append(" ");

                            playerState.getProperties().put(
                                    PlayerProperty.get(PlayerProperty.P_EPISODE_NUM_EPISODE),
                                    episode2char);
                        }

                        String textInfo = infoBuilder.toString().trim();

                        playerState.setTitle(title);
                        playerState.setInfo(textInfo);

                    }
                    // if the remote server guessed the video as a movie then
                    // it sends the guessed title and the year of it
                    else if(parameter.matches("MOVIE\\$.*\\$.*")) {

                        String title = parameter.replaceFirst("MOVIE\\$(.*)\\$.*", "$1");
                        String info  = parameter.replaceFirst("MOVIE\\$.*\\$(.*)", "$1");

                        Integer year = null;

                        if(info.matches("Y[0-9]+.*")) {
                            year = Integer.parseInt( info.replaceFirst("Y([0-9]+).*", "$1") );
                            info = info.replaceFirst("Y[0-9]+(.*)", "$1");
                        }

                        StringBuilder infoBuilder = new StringBuilder();
                        if(year != null) {
                            infoBuilder.append("year ");
                            infoBuilder.append(year).append(" ");
                        }

                        String textInfo = infoBuilder.toString().trim();

                        playerState.setTitle(title);
                        playerState.setInfo(textInfo);

                    }

                    NotificationHelper.postNotification(remoteService, playerState);
                } catch(Exception ex) {
                    // TODO get error string from resources
                    response.putExtra(Intents.EXTRA_ERROR, "Failed to parse video info: " + parameter);
                }

                LocalBroadcastManager.getInstance(remoteService).sendBroadcast(response);

                break;
            }
            case Header.MSG_A_PLAYER_EXTRA:
            {
                // this could be TVDB informations if the remote server found them

                String parameter = packet.getData();
                if(parameter != null && playerState != null) {
                    playerState.parseExtras(parameter, executor, remoteService);

                    NotificationHelper.postNotification(remoteService, playerState);
                }

                break;
            }
        }
    }

}
