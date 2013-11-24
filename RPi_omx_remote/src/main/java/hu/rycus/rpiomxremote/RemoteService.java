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
import hu.rycus.rpiomxremote.util.Header;

/**
 * Created by rycus on 10/30/13.
 */
public class RemoteService extends Service {

    private final IBinder binder = new RemoteBinder();
    private final AtomicInteger refCount = new AtomicInteger(0);

    private Lock startStopLock = new ReentrantLock();
    private RemoteManager remoteManager = null;

    private void start() {
        startStopLock.lock();
        try {
            if(remoteManager == null) {
                remoteManager = RemoteManager.start(this);
            }
        } finally {
            startStopLock.unlock();
        }
    }

    private void stop() {
        stop(true);
    }

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

    public boolean isConnected() {
        return remoteManager != null ? remoteManager.isConnected() : false;
    }

    public boolean isPlaybackActive() {
        return isConnected() && getPlayerState() != null;
    }

    public PlayerState getPlayerState() {
        if(isConnected()) {
            return remoteManager.getPlayerState();
        } else {
            return null;
        }
    }

    public void requestFileList(String path) {
        if(remoteManager != null) {
            remoteManager.listFiles(path);
        }
    }

    public void startPlayer(String video, String subtitle) {
        if(remoteManager != null) {
            remoteManager.startPlayer(video, subtitle);
        }
    }

    public void stopPlayer() {
        if(remoteManager != null) {
            remoteManager.stopPlayer();
        }
    }

    public void playPause() {
        if(remoteManager != null) {
            remoteManager.sendPlayerCommand(Header.MSG_A_PAUSE, null);
        }
    }

    public void setVolume(long volume) {
        if(remoteManager != null) {
            remoteManager.sendPlayerCommand(Header.MSG_A_SET_VOLUME, Long.toString(volume));
        }
    }

    public void seekPlayer(long positionInMillis) {
        if(remoteManager != null) {
            remoteManager.sendPlayerCommand(Header.MSG_A_SEEK_TO, Long.toString(positionInMillis));
        }
    }

    public void increaseSpeed() {
        if(remoteManager != null) {
            remoteManager.sendPlayerCommand(Header.MSG_A_SPEED_INC, null);
        }
    }

    public void decreaseSpeed() {
        if(remoteManager != null) {
            remoteManager.sendPlayerCommand(Header.MSG_A_SPEED_DEC, null);
        }
    }

    public void increaseSubtitleDelay() {
        if(remoteManager != null) {
            remoteManager.sendPlayerCommand(Header.MSG_A_SUB_DELAY_INC, null);
        }
    }

    public void decreaseSubtitleDelay() {
        if(remoteManager != null) {
            remoteManager.sendPlayerCommand(Header.MSG_A_SUB_DELAY_DEC, null);
        }
    }

    public void toggleSubtitleVisibility() {
        if(remoteManager != null) {
            remoteManager.sendPlayerCommand(Header.MSG_A_SUB_TOGGLE, null);
        }
    }

    public void requestSettings() {
        if(remoteManager != null) {
            remoteManager.requestSettings();
        }
    }

    public void setSetting(String key, String value) {
        if(remoteManager != null) {
            remoteManager.setSetting(key, value);
        }
    }

    private void onFirstBind() {
        startService(new Intent(this, RemoteService.class));

        IntentFilter networkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, networkStateFilter);

        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
        boolean connected = activeNetwork != null && activeNetwork.isConnected();
        if(connected && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            start();
        }
    }

    private void onLastUnbind() {
        stop();
        stopSelf();
    }

    public IBinder onBind(Intent intent) {
        int clients = refCount.incrementAndGet();
        if(clients == 1) {
            onFirstBind();
        }

        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        int clients = refCount.decrementAndGet();
        if(clients == 0) {
            onLastUnbind();
        }

        return super.onUnbind(intent);
    }

    public class RemoteBinder extends Binder {
        RemoteService getService() {
            return RemoteService.this;
        }
    }

    private final BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
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
