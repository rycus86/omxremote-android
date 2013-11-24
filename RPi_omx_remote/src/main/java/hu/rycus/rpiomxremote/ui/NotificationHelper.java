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
 * Created by rycus on 11/17/13.
 */
public class NotificationHelper extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 1;

    private static final String BTN_PAUSE       = "pause";
    private static final String BTN_VOLUME_DOWN = "vol-";
    private static final String BTN_VOLUME_UP   = "vol+";

    private static NotificationCompat.Builder builder = null;
    private static RemoteViews remoteViews = null;

    // private static NotificationManager manager = null;
    private static boolean userCancelled = false;

    private static final RemoteServiceCreator rsc = new RemoteServiceCreator();

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

    public static void cancel(RemoteService service) {
        rsc.unbind(service);

        PlayerMediaReceiver.deactivate(service);
        service.stopForeground(true);

        builder = null;
        remoteViews = null;
        userCancelled = false;
    }

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

    private static String formatSeconds(long value) {
        StringBuilder builder = new StringBuilder();
        long hours   = value / 3600;
        long minutes = (value % 3600) / 60;
        long seconds = value % 60;
        // hourse
        if(hours > 0)
            builder.append(hours).append(":");
        // minutes
        if(minutes < 10)
            builder.append("0");
        builder.append(minutes);
        builder.append(":");
        // seconds
        if(seconds < 10)
            builder.append("0");
        builder.append(seconds);
        // to result
        return builder.toString();
    }

}
