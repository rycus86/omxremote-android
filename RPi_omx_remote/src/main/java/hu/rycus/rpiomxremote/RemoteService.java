package hu.rycus.rpiomxremote;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hu.rycus.rpiomxremote.manager.PlayerState;
import hu.rycus.rpiomxremote.manager.RemoteManager;
import hu.rycus.rpiomxremote.manager.v2.RestRemoteManager;
import hu.rycus.rpiomxremote.manager.v2.SubtitleDownloadCallback;
import hu.rycus.rpiomxremote.manager.v2.SubtitleMetadataCallback;
import hu.rycus.rpiomxremote.manager.v2.SubtitleQueryCallback;

/**
 * Remote service responsible for background tasks
 * like handling a remote manager (and a network handler through it)
 * to communicate with and control the remote omxremote-py server and its player.
 *
 * <br/>
 * Created by Viktor Adam on 10/30/13.
 *
 * @author rycus
 */
public class RemoteService extends Service {

    /** The instance of the binder implementation. */
    private final IBinder binder = new RemoteBinder();
    /** Reference counter for bound clients. */
    private final AtomicInteger refCount = new AtomicInteger(0);

    /** Lock object around start/stop operations. */
    private Lock startStopLock = new ReentrantLock();
    /** The current remote manager object. */
    private RemoteManager remoteManager = null;

    /** Starts the remote manager. */
    private void start() {
        startStopLock.lock();
        try {
            if(remoteManager == null) {
                remoteManager = RestRemoteManager.start(this);
            }
        } finally {
            startStopLock.unlock();
        }
    }

    /** Stops the remote manager and unregisters the network state listener. */
    private void stop() {
        stop(true);
    }

    /** Stops the remote manager and (optionally) unregisters the network state listener. */
    private void stop(boolean unregisterNetworkStateListener) {
        startStopLock.lock();
        try {
            if(remoteManager != null) {
                remoteManager.shutdown();
                remoteManager = null;

                if(unregisterNetworkStateListener) {
                    unregisterReceiver(networkStateReceiver);
                }
            }
        } finally {
            startStopLock.unlock();
        }
    }

    /** Returns true if the remote manager has a valid connection to the server. */
    public boolean isConnected() {
        return remoteManager != null ? remoteManager.isConnected() : false;
    }

    /** Returns true if the remote manager is connected and there is an active remote player. */
    public boolean isPlaybackActive() {
        return isConnected() && getPlayerState() != null;
    }

    /**
     * Returns the current player state if the manager is connected
     * and there is an active remote player.
     */
    public PlayerState getPlayerState() {
        if(isConnected()) {
            return remoteManager.getPlayerState();
        } else {
            return null;
        }
    }

    /** Requests remote file list for the given path. */
    public void requestFileList(String path) {
        if(remoteManager != null) {
            remoteManager.listFiles(path);
        }
    }

    /** Requests starting remote playback of a video with the given video and subtitle path. */
    public void startPlayer(String video, String subtitle) {
        if(remoteManager != null) {
            remoteManager.startPlayer(video, subtitle);
        }
    }

    /** Requests stopping the remote player. */
    public void stopPlayer() {
        if(remoteManager != null) {
            remoteManager.stopPlayer();
        }
    }

    /** Sends a toggle play/pause command to the remote player. */
    public void playPause() {
        if(remoteManager != null) {
            remoteManager.ctrlPlayPause();
        }
    }

    /** Sends a set volume command to the remote player. */
    public void setVolume(long volume) {
        if(remoteManager != null) {
            remoteManager.ctrlVolume(volume);
        }
    }

    /** Sends a seek playback position command to the remote player. */
    public void seekPlayer(long positionInMillis) {
        if(remoteManager != null) {
            remoteManager.ctrlSeek(positionInMillis);
        }
    }

    /** Sends an increase speed command to the remote player. */
    public void increaseSpeed() {
        if(remoteManager != null) {
            remoteManager.ctrlIncreaseSpeed();
        }
    }

    /** Sends a decrease speed command to the remote player. */
    public void decreaseSpeed() {
        if(remoteManager != null) {
            remoteManager.ctrlDecreaseSpeed();
        }
    }

    /** Sends an increase subtitle delay command to the remote player. */
    public void increaseSubtitleDelay() {
        if(remoteManager != null) {
            remoteManager.ctrlIncreaseSubtitleDelay();
        }
    }

    /** Sends a decrease subtitle delay command to the remote player. */
    public void decreaseSubtitleDelay() {
        if(remoteManager != null) {
            remoteManager.ctrlDecreaseSubtitleDelay();
        }
    }

    /** Sends a toggle subtitle visibility command to the remote player. */
    public void toggleSubtitleVisibility() {
        if(remoteManager != null) {
            remoteManager.ctrlToggleSubtitleVisibility();
        }
    }

    /** Requests remote settings from the server. */
    public void requestSettings() {
        if(remoteManager != null) {
            remoteManager.requestSettings();
        }
    }

    /** Requests modifying the value of a remote setting. */
    public void setSetting(String key, String value) {
        if(remoteManager != null) {
            remoteManager.setSetting(key, value);
        }
    }

    public void loadSubtitleMetadata(final String filename,
                                     final SubtitleMetadataCallback callback) {
        if (remoteManager != null) {
            remoteManager.loadSubtitleMetadata(filename, callback);
        }
    }

    public void querySubtitles(final String provider, final String query,
                               final SubtitleQueryCallback callback) {
        if (remoteManager != null) {
            remoteManager.querySubtitles(provider, query, callback);
        }
    }

    public void downloadSubtitle(final String provider, final String id, final String directory,
                                 final SubtitleDownloadCallback callback) {
        if (remoteManager != null) {
            remoteManager.downloadSubtitle(provider, id, directory, callback);
        }
    }

    /** This runs when the first client binds to the service. */
    private void onFirstBind() {
        startService(new Intent(this, RemoteService.class));

        IntentFilter networkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, networkStateFilter);

        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
        boolean connected = activeNetwork != null && activeNetwork.isConnected();

        // start if we are connected to a Wi-Fi network
        if(connected && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            start();
        }
    }

    /** This runs when the last client unbinds. */
    private void onLastUnbind() {
        stop();
        stopSelf();
    }

    /** @see android.app.Service#onBind(android.content.Intent) */
    @Override
    public IBinder onBind(Intent intent) {
        int clients = refCount.incrementAndGet();
        if(clients == 1) {
            onFirstBind();
        }

        return binder;
    }

    /** @see android.app.Service#onUnbind(android.content.Intent) */
    @Override
    public boolean onUnbind(Intent intent) {
        int clients = refCount.decrementAndGet();
        if(clients == 0) {
            onLastUnbind();
        }

        return super.onUnbind(intent);
    }

    /** Service binder implementation returning the local service instance. */
    public class RemoteBinder extends Binder {
        RemoteService getService() {
            return RemoteService.this;
        }
    }

    /** Network state receiver to listen on connection/disconnection of a Wi-Fi network. */
    private final BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {

        /**
         * @see android.content.BroadcastReceiver
         *      #onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if(ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
                boolean connected = activeNetwork != null && activeNetwork.isConnected();
                if(connected && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    RemoteService.this.start();
                } else {
                    RemoteService.this.stop(false);
                }
            }
        }
    };

}
