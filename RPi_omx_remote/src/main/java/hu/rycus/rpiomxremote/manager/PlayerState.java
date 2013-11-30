package hu.rycus.rpiomxremote.manager;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
 * Class containing informations about the current state of the remote player.
 *
 * <br/>
 * Created by Viktor Adam on 11/14/13.
 *
 * @author rycus
 */
public class PlayerState {

    /** The filename of the started video. */
    private final String videofile;
    /** The title of the video. */
    private String title;
    /** Information about the video. */
    private String info = "";
    /** The duration of the video in milliseconds. */
    private long duration;
    /** The current playback position of the video in milliseconds. */
    private long position;
    /** The current volume of the playback. */
    private long volume;
    /** True if the video is currently paused, false if the video is playing. */
    private boolean paused = false;
    /** Collection of player properties with their values as Strings. */
    private final Map<PlayerProperty, String> properties = new HashMap<PlayerProperty, String>();

    /** The poster bitmap that belongs to the current video. */
    private BitmapDrawable poster = null;

    /**
     * Constructor with basic parameters.
     * @param resources Resources object to get strings from
     * @param videofile The filename of the started video
     * @param duration  The duration of the video in milliseconds
     * @param volume    The initial volume of the playback
     */
    public PlayerState(Resources resources, String videofile, long duration, long volume) {
        this.videofile  = videofile;
        this.title      = videofile;
        this.duration   = duration;
        this.volume     = volume;

        PlayerProperty.initialize(resources);
    }

    /** Returns the filename of the started video. */
    public String getVideofile() { return videofile; }

    /**
     * Returns the best guessed title for the player.
     * @return If the video is an episode of a show then its title else the filename of the video
     */
    public String getTitle() {
        PlayerProperty titleProperty = PlayerProperty.get(PlayerProperty.P_SHOW_TITLE);
        if(properties.containsKey(titleProperty)) {
            return properties.get(titleProperty);
        } else {
            return title;
        }
    }

    /** Sets the title of the current player. */
    public void setTitle(String title) { this.title = title; }

    /**
     * Returns the best guessed information for the player.
     * @return If the video is an episode of a show then the episode's title
     *          (or the season and epiosde number of it when the title is not found)
     */
    public String getInfo() {
        PlayerProperty infoProperty = PlayerProperty.get(PlayerProperty.P_EPISODE_TITLE);
        if(properties.containsKey(infoProperty)) {
            return properties.get(infoProperty);
        } else {
            return info;
        }
    }

    /**
     * Returns some extra information about the video of the player.
     * @return If the video is an episode of a show then the season and episode number of it
     *          (if the info doesn't contain this information)
     */
    public String getExtra() {
        PlayerProperty infoProperty = PlayerProperty.get(PlayerProperty.P_EPISODE_TITLE);
        if(properties.containsKey(infoProperty)) {
            return info;
        } else {
            return "";
        }
    }

    /**
     * Sets basic information about the video.
     * @param info If the video if an episode of a show then this is either the title
     *             of the episode or the season and episode number of it
     */
    public void setInfo(String info) { this.info = info; }

    /** Returns the duration of the video in milliseconds. */
    public long getDuration() { return duration; }
    /** Sets the duration of the video in milliseconds. */
    public void setDuration(long duration) { this.duration = duration; }

    /** Returns the current playback position of the video in milliseconds. */
    public long getPosition() { return position; }
    /** Sets the current playback position of the video in milliseconds. */
    public void setPosition(long position) { this.position = position; }

    /** Returns the current volume of the playback. */
    public long getVolume() { return volume; }
    /** Sets the current volume of the playback. */
    public void setVolume(long volume) { this.volume = volume; }

    /** Returns true if the video is currently paused, false if the video is playing. */
    public boolean isPaused() { return paused; }
    /** Set this true if the video is currently paused, false if the video is playing. */
    public void setPaused(boolean paused) { this.paused = paused; }

    /** Returns a collection of player properties with their values as Strings. */
    public Map<PlayerProperty, String> getProperties() { return properties; }

    /** Returns the best guessed poster for the player when found any. */
    public BitmapDrawable getPoster() { return poster; }

    /**
     * Sets the bitmap as poster for the player.
     * Also notifies the user and broadcasts modified player state.
     * @param service
     * @param poster
     */
    private void setPoster(RemoteService service, BitmapDrawable poster) {
        this.poster = poster;

        Intent intent = new Intent(Intents.ACTION_CALLBACK);
        intent.putExtra(Intents.EXTRA_PLAYER_REPORT, Intents.EXTRA_PLAYER_REPORT_EXTRA);
        LocalBroadcastManager.getInstance(service).sendBroadcast(intent);

        NotificationHelper.postNotification(service, this);
    }

    /**
     * Parses extra information for player properties.
     * @param data     The data received from the server
     * @param executor The executor to download poster images when found any
     * @param service  The remote service requested parsing the extras
     */
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

    /**
     * Load a poster image from the given URL.
     * @param posterUrl The URL location of the poster image
     * @param service   The remote service requested parsing the extras
     * @return true if the poster was downloaded
     */
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
