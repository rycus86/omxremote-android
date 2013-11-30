package hu.rycus.rpiomxremote.ui;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.Build;
import android.view.KeyEvent;

import hu.rycus.rpiomxremote.R;
import hu.rycus.rpiomxremote.RemoteService;
import hu.rycus.rpiomxremote.manager.PlayerProperty;
import hu.rycus.rpiomxremote.manager.PlayerState;

/**
 * Media receiver to provide lock screen controls.
 *
 * <br/>
 * Created by Viktor Adam on 11/23/13.
 *
 * @author rycus
 */
public class PlayerMediaReceiver extends BroadcastReceiver {

    /** Media control client. */
    private static RemoteControlClient remoteControlClient;
    /** Remote service instance used to control playback. */
    private static RemoteService remoteService;

    /**
     * @see android.content.BroadcastReceiver
     *      #onReceive(android.content.Context, android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if(event.getAction() == KeyEvent.ACTION_UP) {
                if(event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    if(remoteService != null) {
                        remoteService.playPause();
                    }
                } else if((event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS) ||
                        (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT)) {
                    if(remoteService != null) {
                        PlayerState state = remoteService.getPlayerState();
                        if(state != null) {
                            long jumpTime = 10000L; // 10 sec
                            if(event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                                jumpTime *= -1L;
                            }

                            long position = state.getPosition();
                            long jumpPosition = Math.max(0, Math.min(position + jumpTime, state.getDuration()));
                            remoteService.seekPlayer(jumpPosition);
                        }
                    }
                }
            }
        }
    }

    /**
     * Activates lock screen controls.
     * @param service The remote service that requested the controls
     * @param state   The current player state
     */
    public static void activate(RemoteService service, PlayerState state) {
        remoteService = service;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if(remoteControlClient == null) {
                ComponentName receiver = new ComponentName(service.getPackageName(), PlayerMediaReceiver.class.getName());

                AudioManager audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
                audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                audioManager.registerMediaButtonEventReceiver(receiver);
                // build the PendingIntent for the remote control client
                Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                mediaButtonIntent.setComponent(receiver);
                PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(service.getApplicationContext(), 0, mediaButtonIntent, 0);
                // create and register the remote control client
                remoteControlClient = new RemoteControlClient(mediaPendingIntent);
                audioManager.registerRemoteControlClient(remoteControlClient);
            }

            remoteControlClient.setPlaybackState(state.isPaused() ?
                    RemoteControlClient.PLAYSTATE_PAUSED :
                    RemoteControlClient.PLAYSTATE_PLAYING);

            remoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                    RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                    RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS);

            RemoteControlClient.MetadataEditor editor = remoteControlClient.editMetadata(true);
            if(state.getPoster() != null) {
                editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, state.getPoster().getBitmap());
            } else {
                editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK,
                        BitmapFactory.decodeResource(service.getResources(), R.drawable.raspberry));
            }
            editor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, state.getDuration());

            String mainTitle = state.getTitle();
            String subTitle = state.getInfo();

            String showTitle = state.getProperties().get(PlayerProperty.get(PlayerProperty.P_SHOW_TITLE));
            String episodeTitle = state.getProperties().get(PlayerProperty.get(PlayerProperty.P_EPISODE_TITLE));

            if(episodeTitle != null) {
                mainTitle = episodeTitle;
                if(showTitle != null) {
                    subTitle = showTitle;

                    String season  = state.getProperties().get(PlayerProperty.get(PlayerProperty.P_EPISODE_NUM_SEASON));
                    String episode = state.getProperties().get(PlayerProperty.get(PlayerProperty.P_EPISODE_NUM_EPISODE));
                    if(season != null && episode != null) {
                        subTitle += " - S" + season + "E" + episode;
                    } else if(state.getExtra().length() > 0) {
                        subTitle += " - " + state.getExtra();
                    }
                } else if(state.getExtra().length() > 0) {
                    subTitle = state.getExtra();
                }
            } else if(showTitle != null) {
                if(state.getInfo() != null) {
                    mainTitle = state.getInfo();
                    subTitle = showTitle;
                    if(state.getExtra().length() > 0) {
                        subTitle += " - " + state.getExtra();
                    }
                } else if(state.getExtra().length() > 0) {
                    subTitle = state.getExtra();
                }
            } else {
                mainTitle = state.getTitle();
                subTitle = state.getInfo();
                if(state.getExtra().length() > 0) {
                    subTitle += " - " + state.getExtra();
                }
            }

            if(mainTitle != null) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, mainTitle);
            }
            if(subTitle != null) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, subTitle);
            }
            String date = state.getProperties().get(PlayerProperty.get(PlayerProperty.P_EPISODE_DATE));
            if(date != null) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_DATE, date);
            }
            /* TODO needs KitKat
            if(Build.VERSION.SDK_INT >= 19) {
                String rating = state.getProperties().get(PlayerProperty.get(PlayerProperty.P_EPISODE_RATING));
                if(rating != null) {
                    editor.putObject(RemoteControlClient.MetadataEditor.RATING_KEY_BY_OTHERS,
                                        Rating.newPercentageRating(Float.parseFloat(rating));
                }
            }
            */
            editor.apply();
        }
    }

    /** Deactivates lock screen controls. */
    public static void deactivate(RemoteService service) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if(remoteControlClient != null) {
                AudioManager audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
                audioManager.abandonAudioFocus(focusChangeListener);
                audioManager.unregisterRemoteControlClient(remoteControlClient);
                audioManager.unregisterMediaButtonEventReceiver(
                        new ComponentName(service.getPackageName(), PlayerMediaReceiver.class.getName()));
            }
        }

        remoteControlClient = null;
    }

    /** Focus change listener doing absolutely nothing. */
    private static AudioManager.OnAudioFocusChangeListener focusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            /* NO-OP */
        }
    };

}
