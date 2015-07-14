package hu.rycus.rpiomxremote.ui;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.util.Log;
import android.widget.RemoteViews;

import hu.rycus.rpiomxremote.PlayerActivity;
import hu.rycus.rpiomxremote.R;
import hu.rycus.rpiomxremote.RemoteService;
import hu.rycus.rpiomxremote.RemoteServiceCreator;
import hu.rycus.rpiomxremote.manager.PlayerState;
import hu.rycus.rpiomxremote.util.Intents;

/**
 * Helper class to manage the status bar notification.
 *
 * <br/>
 * Created by Viktor Adam on 11/17/13.
 *
 * @author rycus
 */
public class NotificationHelper extends BroadcastReceiver {

    /** The persistent notification's identifier. */
    private static final int NOTIFICATION_ID = 1;

    /** Identifier for the pause button in the notification. */
    private static final String BTN_PAUSE       = "pause";
    /** Identifier for the volume down button in the notification. */
    private static final String BTN_VOLUME_DOWN = "vol-";
    /** Identifier for the volume up button in the notification. */
    private static final String BTN_VOLUME_UP   = "vol+";

    /**
     * Did the user clear the notification?
     * (Since the notification is started as a service's foreground notification
     * the user shouldn't be able to clear it).
     */
    private static boolean userCancelled = false;

    /** Helper object to bind/unbind the remote service. */
    private static final RemoteServiceCreator rsc = new RemoteServiceCreator();

    /** Posts a notification for the remote service with the given player state. */
    public static void postNotification(RemoteService service, PlayerState state) {
        if(userCancelled) return;

        if(!rsc.isBindRequested()) {
            rsc.bind(service);
        }

        final Notification.Builder builder = new Notification.Builder(service);
        builder.setOnlyAlertOnce(true);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setColor(0xFFFF0000);

        Intent deleteIntent = new Intent(Intents.ACTION_NOTIFICATION_DELETED);
        builder.setDeleteIntent(PendingIntent.getBroadcast(service, 1, deleteIntent, 0));

        Intent contentIntent = new Intent(service, PlayerActivity.class);
        contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_VIDEO_FILE, state.getVideofile());
        contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_DURATION, state.getDuration());
        contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_VOLUME, state.getVolume());
        PendingIntent pendingContentIntent = PendingIntent.getActivity(service, 2, contentIntent, 0);
        builder.setContentIntent(pendingContentIntent);

        builder.setContentTitle(state.getTitle());
        builder.setContentText(state.getInfo());
        builder.setSubText(state.getExtra());

        Notification notification = builder.build();

        final RemoteViews remoteViews =
                new RemoteViews(service.getPackageName(), R.layout.notification_player);

        remoteViews.setOnClickPendingIntent(R.id.notif_image, pendingContentIntent);

        Intent pauseIntent = new Intent(Intents.ACTION_NOTIFICATION_BUTTON_CLICKED);
        pauseIntent.putExtra(Intents.EXTRA_NOTIFICATION_BUTTON_ID, BTN_PAUSE);
        remoteViews.setOnClickPendingIntent(R.id.notif_btn_pause,
                PendingIntent.getBroadcast(service, 3, pauseIntent, 0));

        Intent volumeDownIntent = new Intent(Intents.ACTION_NOTIFICATION_BUTTON_CLICKED);
        volumeDownIntent.putExtra(Intents.EXTRA_NOTIFICATION_BUTTON_ID, BTN_VOLUME_DOWN);
        remoteViews.setOnClickPendingIntent(R.id.notif_btn_volume_down,
                PendingIntent.getBroadcast(service, 4, volumeDownIntent, 0));

        Intent volumeUpIntent = new Intent(Intents.ACTION_NOTIFICATION_BUTTON_CLICKED);
        volumeUpIntent.putExtra(Intents.EXTRA_NOTIFICATION_BUTTON_ID, BTN_VOLUME_UP);
        remoteViews.setOnClickPendingIntent(R.id.notif_btn_volume_up,
                PendingIntent.getBroadcast(service, 5, volumeUpIntent, 0));

        remoteViews.setTextViewText(R.id.notif_title, state.getTitle());
        remoteViews.setTextViewText(R.id.notif_info, state.getInfo());
        remoteViews.setTextViewText(R.id.notif_extra, state.getExtra());

        remoteViews.setImageViewResource(R.id.notif_btn_pause,
                state.isPaused() ?
                        R.drawable.ic_notif_play :
                        R.drawable.ic_notif_pause);

        if(state.getPoster() != null) {
            remoteViews.setImageViewBitmap(R.id.notif_image, state.getPoster().getBitmap());
        }

        notification.bigContentView = remoteViews;

        try {
            service.startForeground(NOTIFICATION_ID, notification);
            PlayerMediaReceiver.activate(service, state);
        } catch (IllegalStateException ex) {
            Log.w("Notification", "Failed to post new notification", ex);
        }
    }

    // TODO
    private MediaSession createSession(final RemoteService service) {
        final MediaSession session = new MediaSession(service, "RPiMediaSession");
        session.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
            }

            @Override
            public void onPause() {
                super.onPause();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
            }

            @Override
            public void onFastForward() {
                super.onFastForward();
            }

            @Override
            public void onRewind() {
                super.onRewind();
            }
        });

        session.setActive(true);

        final PlaybackState playbackState = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                        PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackState.ACTION_FAST_FORWARD | PlaybackState.ACTION_REWIND)
                .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build();

        session.setPlaybackState(playbackState);

        return session;
    }

    /** Cancels the notification. */
    public static void cancel(RemoteService service) {
        rsc.unbind(service);

        // PlayerMediaReceiver.deactivate(service);
        service.stopForeground(true);

        userCancelled = false;
    }

    /**
     * Receives intents from notification buttons.
     *
     * @see android.content.BroadcastReceiver
     *      #onReceive(android.content.Context, android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intents.ACTION_NOTIFICATION_DELETED.equals(intent.getAction())) {
            userCancelled = true;
        } else if(Intents.ACTION_NOTIFICATION_BUTTON_CLICKED.equals(intent.getAction())) {
            RemoteService service = rsc.isServiceBound() ? rsc.getService() : null;
            if(service == null) return;

            String button = intent.getStringExtra(Intents.EXTRA_NOTIFICATION_BUTTON_ID);
            if(BTN_PAUSE.equals(button)) {
                service.playPause();
            } else if(BTN_VOLUME_DOWN.equals(button)) {
                PlayerState state = service.getPlayerState();
                if(state != null) {
                    service.setVolume(state.getVolume() - 1L);
                }
            } else if(BTN_VOLUME_UP.equals(button)) {
                PlayerState state = service.getPlayerState();
                if(state != null) {
                    service.setVolume(state.getVolume() + 1L);
                }
            }
        }
    }

}
