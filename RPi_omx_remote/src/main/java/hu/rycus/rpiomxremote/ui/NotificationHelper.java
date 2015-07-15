package hu.rycus.rpiomxremote.ui;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;

import java.util.concurrent.TimeUnit;

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

    private static final String BTN_REW_BIG = "rew+big";
    private static final String BTN_REW_SMALL = "rew+small";

    private static final String BTN_FF_BIG = "ff+big";
    private static final String BTN_FF_SMALL = "ff+small";

    /** Helper object to bind/unbind the remote service. */
    private static final RemoteServiceCreator rsc = new RemoteServiceCreator();

    public static void postNotification(RemoteService service, PlayerState state) {
        if(!rsc.isBindRequested()) {
            rsc.bind(service);
        }

        final Notification.Builder builder = new Notification.Builder(service)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFFAA0000)
                .setShowWhen(false)
                .setContentTitle(state.getTitle())
                .setContentText(state.getInfo())
                .setSubText(state.getExtra())
                .setContentIntent(createActivityPendingIntent(service, state));

        if (state.getPoster() != null) {
            builder.setLargeIcon(state.getPoster().getBitmap());
        } else {
            builder.setLargeIcon(
                    BitmapFactory.decodeResource(
                            service.getResources(), R.drawable.ic_launcher));
        }

        builder.addAction(new Notification.Action(
                android.R.drawable.ic_media_previous, null,
                createControlPendingIntent(service, BTN_REW_BIG, 0x21)));

        builder.addAction(new Notification.Action(
                android.R.drawable.ic_media_rew, null,
                createControlPendingIntent(service, BTN_REW_SMALL, 0x22)));

        builder.addAction(new Notification.Action(
                getPlayPauseIcon(state), null,
                createControlPendingIntent(service, BTN_PAUSE, 0x10)));

        builder.addAction(new Notification.Action(
                android.R.drawable.ic_media_ff, null,
                createControlPendingIntent(service, BTN_FF_SMALL, 0x31)));

        builder.addAction(new Notification.Action(
                android.R.drawable.ic_media_next, null,
                createControlPendingIntent(service, BTN_FF_BIG, 0x32)));

        builder.setStyle(new Notification.MediaStyle()
                .setShowActionsInCompactView(2));

        final Notification notification = builder.build();

        service.startForeground(NOTIFICATION_ID, notification);
    }

    private static int getPlayPauseIcon(final PlayerState state) {
        if (state.isPaused()) {
            return android.R.drawable.ic_media_play;
        } else {
            return android.R.drawable.ic_media_pause;
        }
    }

    private static PendingIntent createActivityPendingIntent(
            final Context context, final PlayerState state) {

        final Intent contentIntent = new Intent(context, PlayerActivity.class);
        contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_VIDEO_FILE, state.getVideofile());
        contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_DURATION, state.getDuration());
        contentIntent.putExtra(PlayerFragment.EXTRA_PLAYER_VOLUME, state.getVolume());
        return PendingIntent.getActivity(context, 0x01, contentIntent, 0);
    }

    private static PendingIntent createControlPendingIntent(
            final Context context, final String buttonId, final int requestCode) {

        final Intent controlIntent = new Intent(Intents.ACTION_NOTIFICATION_BUTTON_CLICKED);
        controlIntent.putExtra(Intents.EXTRA_NOTIFICATION_BUTTON_ID, buttonId);
        return PendingIntent.getBroadcast(context, requestCode, controlIntent, 0);
    }

    /** Cancels the notification. */
    public static void cancel(RemoteService service) {
        rsc.unbind(service);
        service.stopForeground(true);
    }

    /**
     * Receives intents from notification buttons.
     *
     * @see android.content.BroadcastReceiver
     *      #onReceive(android.content.Context, android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intents.ACTION_NOTIFICATION_BUTTON_CLICKED.equals(intent.getAction())) {
            final RemoteService service = rsc.isServiceBound() ? rsc.getService() : null;
            if(service == null) return;

            final String button = intent.getStringExtra(Intents.EXTRA_NOTIFICATION_BUTTON_ID);
            if(BTN_PAUSE.equals(button)) {
                service.playPause();
            } else if(BTN_REW_BIG.equals(button)) {
                jump(service, -TimeUnit.MINUTES.toMillis(3));
            } else if(BTN_REW_SMALL.equals(button)) {
                jump(service, -TimeUnit.SECONDS.toMillis(15));
            } else if(BTN_FF_SMALL.equals(button)) {
                jump(service, TimeUnit.SECONDS.toMillis(15));
            } else if(BTN_FF_BIG.equals(button)) {
                jump(service, TimeUnit.MINUTES.toMillis(3));
            }
        }
    }

    private void jump(final RemoteService service, final long relative) {
        final PlayerState state = service.getPlayerState();
        if(state != null) {
            final long position = state.getPosition();
            final long jumpPosition = Math.max(0, Math.min(position + relative, state.getDuration()));
            service.seekPlayer(jumpPosition);
        }
    }

}
