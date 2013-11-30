package hu.rycus.rpiomxremote.ui;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
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

    /** The notification builder used to create the notification. */
    private static NotificationCompat.Builder builder = null;
    /** The remote views used on the expanded notification. */
    private static RemoteViews remoteViews = null;

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

        if(builder == null) {
            builder = new NotificationCompat.Builder(service);
            builder.setOnlyAlertOnce(true);
            builder.setSmallIcon(R.drawable.ic_notification);

            Intent deleteIntent = new Intent(Intents.ACTION_NOTIFICATION_DELETED);
            builder.setDeleteIntent(PendingIntent.getBroadcast(service, 1, deleteIntent, 0));

            Intent contentIntent = new Intent(service, PlayerActivity.class);
            contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_VIDEO_FILE, state.getVideofile());
            contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_DURATION,   state.getDuration());
            contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_VOLUME,     state.getVolume());
            PendingIntent pendingContentIntent = PendingIntent.getActivity(service, 2, contentIntent, 0);
            builder.setContentIntent(pendingContentIntent);
        }

        Notification notification = builder.build();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if(remoteViews == null) {
                remoteViews = new RemoteViews(service.getPackageName(), R.layout.notification_player);

                Intent contentIntent = new Intent(service, PlayerActivity.class);
                contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_VIDEO_FILE, state.getVideofile());
                contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_DURATION,   state.getDuration());
                contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_VOLUME,     state.getVolume());
                PendingIntent pendingContentIntent = PendingIntent.getActivity(service, 2, contentIntent, 0);
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
            }

            notification.bigContentView = remoteViews;
        }

        builder.setContentTitle(state.getTitle());
        builder.setContentText(state.getInfo());

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
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
        }

        service.startForeground(NOTIFICATION_ID, notification);
        PlayerMediaReceiver.activate(service, state);
    }

    /** Cancels the notification. */
    public static void cancel(RemoteService service) {
        rsc.unbind(service);

        PlayerMediaReceiver.deactivate(service);
        service.stopForeground(true);

        builder = null;
        remoteViews = null;
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
