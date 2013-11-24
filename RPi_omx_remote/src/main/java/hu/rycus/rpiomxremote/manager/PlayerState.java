package hu.rycus.rpiomxremote.manager;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import hu.rycus.rpiomxremote.RemoteService;
import hu.rycus.rpiomxremote.ui.NotificationHelper;
import hu.rycus.rpiomxremote.util.Intents;

/**
 * Created by rycus on 11/14/13.
 */
public class PlayerState {

    private final String videofile;
    private String title;
    private String info = "";
    private long duration;
    private long position;
    private long volume;
    private boolean paused = false;
    private final Map<PlayerProperty, String> properties = new HashMap<PlayerProperty, String>();

    private BitmapDrawable poster = null;

    public PlayerState(Context context, String videofile, long duration, long volume) {
        this.videofile  = videofile;
        this.title      = videofile;
        this.duration   = duration;
        this.volume     = volume;

        PlayerProperty.initialize(context.getResources());
    }

    public String getVideofile() {
        return videofile;
    }

    public String getTitle() {
        PlayerProperty titleProperty = PlayerProperty.get(PlayerProperty.P_SHOW_TITLE);
        if(properties.containsKey(titleProperty)) {
            return properties.get(titleProperty);
        } else {
            return title;
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInfo() {
        PlayerProperty infoProperty = PlayerProperty.get(PlayerProperty.P_EPISODE_TITLE);
        if(properties.containsKey(infoProperty)) {
            return properties.get(infoProperty);
        } else {
            return info;
        }
    }

    public String getExtra() {
        PlayerProperty infoProperty = PlayerProperty.get(PlayerProperty.P_EPISODE_TITLE);
        if(properties.containsKey(infoProperty)) {
            return info;
        } else {
            return "";
        }
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public BitmapDrawable getPoster() { return poster; }

    private void setPoster(RemoteService service, BitmapDrawable poster) {
        this.poster = poster;

        Intent intent = new Intent(Intents.ACTION_CALLBACK);
        intent.putExtra(Intents.EXTRA_PLAYER_REPORT, Intents.EXTRA_PLAYER_REPORT_EXTRA);
        LocalBroadcastManager.getInstance(service).sendBroadcast(intent);

        NotificationHelper.postNotification(service, this);
    }

    public Map<PlayerProperty, String> getProperties() {
        return properties;
    }

    public void parseExtras(String data, Executor executor, final RemoteService service) {
        if(data != null) {
            boolean hasPoster = false;

            for(String block : data.split("\\|")) {
                if(!block.matches("[A-Z]{2}:.*")) continue;

                String id    = block.substring(0, 2);
                String value = block.substring(3);

                PlayerProperty property = PlayerProperty.get(id);
                if(property != null) {
                    properties.put(property, value);
                }

                if(!hasPoster) {
                    if(PlayerProperty.listPosters().contains(property)) {
                        hasPoster = true;
                    }
                }
            }

            Intent intent = new Intent(Intents.ACTION_CALLBACK);
            intent.putExtra(Intents.EXTRA_PLAYER_REPORT, Intents.EXTRA_PLAYER_REPORT_EXTRA);
            LocalBroadcastManager.getInstance(service).sendBroadcast(intent);

            if(hasPoster) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        for(PlayerProperty posterProp : PlayerProperty.listPosters()) {
                            String posterUrl = properties.get(posterProp);
                            if(posterUrl != null) {
                                if(loadImage(posterUrl, service)) break;
                            }
                        }
                    }
                });
            }
        }
    }

    private boolean loadImage(String posterUrl, RemoteService service) {
        try {
            URL url = new URL(posterUrl);
            URLConnection connection = url.openConnection();
            connection.setReadTimeout(30000); // 30 sec

            int contentLength = connection.getContentLength();
            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(5 * 1024, contentLength)); // 5kB

            InputStream input = connection.getInputStream();

            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(input);
            } finally {
                input.close();
            }

            if(bitmap != null) {
                setPoster(service, new BitmapDrawable(service.getResources(), bitmap));
                return true;
            }
        } catch(Exception ex) {
            Log.d("PS", "Failed to load poster from " + posterUrl, ex);
        }

        return false;
    }

}
