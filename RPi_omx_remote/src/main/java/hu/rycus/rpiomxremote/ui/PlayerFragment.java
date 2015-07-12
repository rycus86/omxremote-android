package hu.rycus.rpiomxremote.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Map;

import hu.rycus.rpiomxremote.R;
import hu.rycus.rpiomxremote.RemoteService;
import hu.rycus.rpiomxremote.RemoteServiceCreator;
import hu.rycus.rpiomxremote.manager.PlayerProperty;
import hu.rycus.rpiomxremote.manager.PlayerState;

/**
 * Fragment displaying player controls during playback.
 *
 * <br/>
 * Created by Viktor Adam on 11/7/13.
 *
 * @author rycus
 */
public class PlayerFragment extends Fragment {

    /** Intent extra key for video duration in milliseconds. */
    public static final String EXTRA_PLAYER_DURATION    = "init.duration";
    /** Intent extra key for video volume. */
    public static final String EXTRA_PLAYER_VOLUME      = "init.volume";
    /** Intent extra key for video file name. */
    public static final String EXTRA_PLAYER_VIDEO_FILE  = "init.video.file";

    /** The root view group of the fragment. */
    private ViewGroup   vRoot;
    /** Image view for the background poster. */
    private ImageView   imgBackground;
    /** View containing header controls and texts. */
    private View        vHeader;
    /** View (button) for back navigation. */
    private View        btnBack;
    /** View (button) for menu popup. */
    private View        btnMenu;
    /** Text view displaying video title. */
    private TextView    txtTitle;
    /** Text view displaying additional video info. */
    private TextView    txtInfo;
    /** Text view displaying extra info for the video. */
    private TextView    txtExtra;
    /** View containing buttons for extra controls. */
    private View        vExtraControls;
    /** View containing miscellaneous informations. */
    private ViewGroup   vMiscellaneous;
    /** View (button) for adjusting subtitle delay (decrementing it). */
    private View        btnSubtitleDelayDec;
    /** View (button) for adjusting subtitle delay (incrementing it). */
    private View        btnSubtitleDelayInc;
    /** View (button) to jump back in the video. */
    private View        btnJumpBackward;
    /** View (button) to jump forward in the video. */
    private View        btnJumpForward;
    /** View containing basic video controls (seek bar and volume controls). */
    private View        vControls;
    /** Seek bar displaying/adjusting playback position. */
    private SeekBar     seekbar;
    /** Button to play/pause the video. */
    private ImageButton btnPlayPause;
    /** View (button) to adjust the volume (up). */
    private View        btnVolumeUp;
    /** View (button) to adjust the volume (down). */
    private View        btnVolumeDown;
    /** Text view displaying the elapsed time (the position of the player). */
    private TextView    txtTimeElapsed;
    /** Text view displaying the length of the video. */
    private TextView    txtTimeLength;
    /** View to provide background for the elapsed time view. */
    private View        vShadowTimeElapsed;
    /** View to provide background for the video length view. */
    private View        vShadowTimeLength;
    /** Text view displaying the current volume. */
    private TextView    txtVolume;

    // These functions are not implemented yet
    // private View        btnSlower;
    // private View        btnFaster;
    // private View        btnToggleSubtitle;

    /** True if the user is adjusting the position seek bar. */
    private boolean seekbarIsAdjusting = false;
    /** The length of the video in playback. */
    private long playbackLength = 0;
    /** The current volume. */
    private long volume = Long.MIN_VALUE;
    /** Popup menu for the menu button. */
    private PopupMenu popupMenu;
    /** True if the player provided miscellaneous data. */
    private boolean hasMiscellaneousData = false;
    /** True if miscellaneous data was shown at least once. */
    private boolean miscellaneousWasShown = false;
    /** True if a poster was set instead of the default background. */
    private boolean posterWasSet = false;

    /** Helper object to bind/unbind the remote service. */
    private final RemoteServiceCreator rsc = new RemoteServiceCreator() {

        /**
         * @see hu.rycus.rpiomxremote.RemoteServiceCreator
         *      #onServiceInstanceReceived(hu.rycus.rpiomxremote.RemoteService)
         */
        @Override
        protected void onServiceInstanceReceived(RemoteService service) {
            super.onServiceInstanceReceived(service);

            if(service != null) {
                PlayerState state = service.getPlayerState();
                if(state != null) {
                    processPlayerState(state);
                    return;
                }
            }

            // at this point there is no active player so finish
            onPlaybackFinished();

            vRoot.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getActivity().finish();
                }
            }, 2000);
        }
    };

    /** @see android.support.v4.app.Fragment#onCreate(android.os.Bundle) */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    /** @see android.support.v4.app.Fragment#onStart() */
    @Override
    public void onStart() {
        super.onStart();
        rsc.bind(getActivity());
    }

    /** @see android.support.v4.app.Fragment#onStop() */
    @Override
    public void onStop() {
        super.onStop();
        rsc.unbind(getActivity());
    }

    /** Finds a view by the given identifier. */
    private <T> T find(int id) {
        return (T) vRoot.findViewById(id);
    }

    /** Formats time given in seconds. */
    private String formatSeconds(long value) {
        StringBuilder builder = new StringBuilder();
        long hours   = value / 3600;
        long minutes = (value % 3600) / 60;
        long seconds = value % 60;
        // hours
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

    /** Sets the current playback length (on the UI). */
    private void setLength(long milliseconds) {
        this.playbackLength = milliseconds;
        txtTimeLength.setText(formatSeconds(milliseconds / 1000));
    }

    /** Sets the current playback position (on the UI). */
    private void setPosition(long milliseconds, boolean fromUser) {
        if(fromUser || !seekbarIsAdjusting) {
            txtTimeElapsed.setText(formatSeconds(milliseconds / 1000));
        }

        if(playbackLength <= 0) {
            seekbar.setProgress(0);
        } else {
            if(seekbarIsAdjusting && !fromUser) return;

            int progress = (int) (milliseconds * 1000 / playbackLength);
            seekbar.setProgress(progress);
        }
    }

    /** Sets the current volume (on the UI). */
    private void setVolume(long volume) {
        if(this.volume == volume) return;

        this.volume = volume;

        this.txtVolume.clearAnimation();
        this.txtVolume.setText(getResources().getString(R.string.pl_volume, volume));

        Animation animation = new AlphaAnimation(1f, 0f);
        animation.setStartOffset(1500);
        animation.setDuration(750);
        animation.setFillAfter(true);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationRepeat(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                txtVolume.setVisibility(View.GONE);
            }
        });

        txtVolume.setVisibility(View.VISIBLE);
        this.txtVolume.startAnimation(animation);
    }

    /** Sets the paused state of the player (on the UI). */
    private void setPaused(boolean paused) {
        btnPlayPause.setImageResource(paused ?
                R.drawable.ic_action_play :
                R.drawable.ic_action_pause);
    }

    /** Sets player state (on UI). */
    public void setState(long positionInMillis, long volume, boolean paused) {
        setPosition(positionInMillis, false);
        setVolume(volume);
        setPaused(paused);
    }

    /** Processes current player state. */
    public void processPlayerState(PlayerState state) {
        setLength(state.getDuration());
        setState(state.getPosition(), state.getVolume(), state.isPaused());

        // TODO process movie title when movies get processed
        txtTitle.setText(state.getTitle());
        txtInfo.setText(state.getInfo());
        txtInfo.setVisibility(state.getInfo().length() > 0 ? View.VISIBLE : View.GONE);
        txtExtra.setText(state.getExtra());
        txtExtra.setVisibility(state.getExtra().length() > 0 ? View.VISIBLE : View.GONE);

        if(state.getPoster() != null) {
            final Drawable poster = state.getPoster();

            if(!posterWasSet) {
                AlphaAnimation fadeOutAnimation = new AlphaAnimation(1f, 0.1f);
                fadeOutAnimation.setDuration(400);
                fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationEnd(Animation animation) {
                        imgBackground.setImageDrawable(poster);

                        AlphaAnimation fadeInAnimation = new AlphaAnimation(0.1f, 1f);
                        fadeInAnimation.setInterpolator(new DecelerateInterpolator());
                        fadeInAnimation.setDuration(750);
                        imgBackground.startAnimation(fadeInAnimation);
                    }

                    @Override public void onAnimationStart(Animation animation) { }
                    @Override public void onAnimationRepeat(Animation animation) { }
                });
                imgBackground.startAnimation(fadeOutAnimation);

                posterWasSet = true;
            } else {
                imgBackground.setImageDrawable(poster);
            }
        }

        Map<PlayerProperty, String> properties = state.getProperties();

        hasMiscellaneousData = false;
        vMiscellaneous.removeAllViews();

        String date = properties.get(PlayerProperty.get(PlayerProperty.P_EPISODE_DATE));
        if(date != null) {
            TextView vtxt = (TextView) vMiscellaneous.inflate(vMiscellaneous.getContext(), R.layout.player_misc_text, null);
            vtxt.setText(date);
            vtxt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_date, 0, 0, 0);
            vMiscellaneous.addView(vtxt);
            hasMiscellaneousData = true;
        }

        String rating = properties.get(PlayerProperty.get(PlayerProperty.P_EPISODE_RATING));
        if(rating != null) {
            TextView vtxt = (TextView) vMiscellaneous.inflate(vMiscellaneous.getContext(), R.layout.player_misc_text, null);
            vtxt.setText(rating);
            vtxt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_rating, 0, 0, 0);
            vMiscellaneous.addView(vtxt);
            hasMiscellaneousData = true;
        }

        if(hasMiscellaneousData) {
            vMiscellaneous.requestLayout();

            if(!miscellaneousWasShown) {
                showHidingControls(vMiscellaneous);
                miscellaneousWasShown = true;
            }
        }
    }

    /**
     * Fade in then out floating controls.
     * @param singleControl If not null then only show this single control
     */
    private void showHidingControls(final View singleControl) {
        boolean show = singleControl != null ?
                singleControl.getVisibility() != View.VISIBLE :
                vExtraControls.getVisibility() != View.VISIBLE;

        if(singleControl != null) {
            singleControl.clearAnimation();
        } else {
            vExtraControls.clearAnimation();
            vShadowTimeElapsed.clearAnimation();
            vShadowTimeLength.clearAnimation();

            if(hasMiscellaneousData) vMiscellaneous.clearAnimation();
        }

        AnimationSet fullAnimation = new AnimationSet(true);

        if(show) {
            Animation showAnimation = new AlphaAnimation(0f, 1f);
            showAnimation.setFillAfter(true);
            showAnimation.setDuration(350);

            fullAnimation.addAnimation(showAnimation);
        }

        Animation hideAnimation = new AlphaAnimation(1f, 0f);
        hideAnimation.setStartOffset(5000);
        hideAnimation.setDuration(750);
        hideAnimation.setFillAfter(true);
        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationRepeat(Animation animation) { }
            @Override public void onAnimationEnd(Animation animation) {
                if(singleControl != null) {
                    singleControl.setVisibility(View.GONE);
                } else {
                    vExtraControls.setVisibility(View.GONE);
                    vShadowTimeElapsed.setVisibility(View.GONE);
                    vShadowTimeLength.setVisibility(View.GONE);

                    if(hasMiscellaneousData) vMiscellaneous.setVisibility(View.GONE);
                }
            }
        });

        fullAnimation.addAnimation(hideAnimation);

        if(singleControl != null) {
            singleControl.setVisibility(View.VISIBLE);
        } else {
            vExtraControls.setVisibility(View.VISIBLE);
            vShadowTimeElapsed.setVisibility(View.VISIBLE);
            vShadowTimeLength.setVisibility(View.VISIBLE);

            if(hasMiscellaneousData) vMiscellaneous.setVisibility(View.VISIBLE);
        }

        if(singleControl != null) {
            singleControl.startAnimation(fullAnimation);
        } else {
            vExtraControls.startAnimation(fullAnimation);
            vShadowTimeElapsed.startAnimation(fullAnimation);
            vShadowTimeLength.startAnimation(fullAnimation);

            if(hasMiscellaneousData) vMiscellaneous.startAnimation(fullAnimation);
        }
    }

    /** Runs when playback finishes. */
    public void onPlaybackFinished() {
        Animation fadeoutAnimation = new AlphaAnimation(1f, 0f);
        fadeoutAnimation.setDuration(1000);
        fadeoutAnimation.setFillAfter(true);

        btnPlayPause.setEnabled(false);
        btnVolumeDown.setEnabled(false);
        btnVolumeUp.setEnabled(false);

        vHeader.startAnimation(fadeoutAnimation);
        vControls.startAnimation(fadeoutAnimation);

        TranslateAnimation exitLeftAnimation = new TranslateAnimation(0, -200, 0, 0);
        exitLeftAnimation.setDuration(1000);
        exitLeftAnimation.setFillAfter(true);
        AnimationSet exitLeftAnimSet = new AnimationSet(true);
        exitLeftAnimSet.setDuration(1000);
        exitLeftAnimSet.addAnimation(fadeoutAnimation);
        exitLeftAnimSet.addAnimation(exitLeftAnimation);
        exitLeftAnimSet.setFillAfter(true);
        txtTimeElapsed.startAnimation(exitLeftAnimSet);

        TranslateAnimation exitRightAnimation = new TranslateAnimation(0, 200, 0, 0);
        exitRightAnimation.setDuration(1000);
        exitRightAnimation.setFillAfter(true);
        AnimationSet exitRightAnimSet = new AnimationSet(true);
        exitRightAnimSet.setDuration(1000);
        exitRightAnimSet.addAnimation(fadeoutAnimation);
        exitRightAnimSet.addAnimation(exitRightAnimation);
        exitRightAnimSet.setFillAfter(true);
        txtTimeLength.startAnimation(exitRightAnimSet);
    }

    /**
     * @see android.support.v4.app.Fragment
     *      #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        vRoot = (ViewGroup) inflater.inflate(R.layout.fragment_player, container, false);

        imgBackground       = find(R.id.img_player_bg);
        vHeader             = find(R.id.player_header);
        btnBack             = find(R.id.btn_player_back);
        btnMenu             = find(R.id.btn_player_menu);
        txtTitle            = find(R.id.txt_play_title);
        txtInfo             = find(R.id.txt_play_info);
        txtExtra            = find(R.id.txt_play_extra);
        vExtraControls      = find(R.id.player_extra_controls);
        vMiscellaneous      = find(R.id.player_miscellaneous);
        // btnSlower           = find(R.id.btn_play_slower);
        // btnFaster           = find(R.id.btn_play_faster);
        btnSubtitleDelayDec = find(R.id.btn_play_subtitle_delay_dec);
        btnSubtitleDelayInc = find(R.id.btn_play_subtitle_delay_inc);
        btnJumpBackward     = find(R.id.btn_play_jump_prev);
        btnJumpForward      = find(R.id.btn_play_jump_next);
        vControls           = find(R.id.player_controls);
        seekbar             = find(R.id.seek_play);
        btnPlayPause        = find(R.id.btn_play_pause);
        btnVolumeUp         = find(R.id.btn_play_volume_up);
        btnVolumeDown       = find(R.id.btn_play_volume_down);
        txtTimeElapsed      = find(R.id.txt_play_time_elapsed);
        txtTimeLength       = find(R.id.txt_play_time_length);
        vShadowTimeElapsed  = find(R.id.shadow_play_time_elapsed);
        vShadowTimeLength   = find(R.id.shadow_play_time_length);
        txtVolume           = find(R.id.txt_play_volume);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(popupMenu != null) {
                    // TODO remove this, or add support for SDK.Version < 11
                    popupMenu.show();
                }
            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            popupMenu = new PopupMenu(getActivity(), btnMenu);
            popupMenu.getMenuInflater().inflate(R.menu.player, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch(item.getItemId()) {
                        case R.id.menu_player_stop:
                        {
                            if(rsc.isServiceBound()) {
                                rsc.getService().stopPlayer();
                            }
                            getActivity().finish();
                            return true;
                        }
                    }

                    return false;
                }
            });
        } else {
            btnMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    // TODO resource for String[]
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            v.getContext(),
                            android.R.layout.select_dialog_item,
                            new String[] { getResources().getString(R.string.pl_menu_stop) });
                    builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(which == 0) { // stop player
                                if(rsc.isServiceBound()) {
                                    rsc.getService().stopPlayer();
                                }
                                getActivity().finish();
                            }
                        }
                    });
                    builder.show();
                }
            });
        }

        seekbar.setMax(1000);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    setPosition(playbackLength * progress / seekBar.getMax(), true);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekbarIsAdjusting = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekbarIsAdjusting = false;

                if(rsc.isServiceBound()) {
                    long position = playbackLength * seekBar.getProgress() / seekBar.getMax();
                    rsc.getService().seekPlayer(position);
                }
            }
        });

        vExtraControls.setVisibility(View.GONE);
        vMiscellaneous.setVisibility(View.GONE);

        imgBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHidingControls(null);
            }
        });

        long length = 0L;
        long volume = 0L;

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rsc.isServiceBound()) {
                    rsc.getService().playPause();
                }
            }
        });
        btnVolumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO configurable step value
                if(rsc.isServiceBound()) {
                    setVolume(PlayerFragment.this.volume - 1);
                    rsc.getService().setVolume(PlayerFragment.this.volume);
                }
            }
        });
        btnVolumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rsc.isServiceBound()) {
                    setVolume(PlayerFragment.this.volume + 1);
                    rsc.getService().setVolume(PlayerFragment.this.volume);
                }
            }
        });

        /*
        btnFaster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rsc.isServiceBound()) {
                    rsc.getService().increaseSpeed();
                }

                showExtraControls();
            }
        });
        btnSlower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rsc.isServiceBound()) {
                    rsc.getService().decreaseSpeed();
                }

                showExtraControls();
            }
        });
        */

        btnSubtitleDelayInc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rsc.isServiceBound()) {
                    rsc.getService().increaseSubtitleDelay();
                }

                showHidingControls(vExtraControls);
            }
        });
        btnSubtitleDelayDec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rsc.isServiceBound()) {
                    rsc.getService().decreaseSubtitleDelay();
                }

                showHidingControls(vExtraControls);
            }
        });
        btnJumpBackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rsc.isServiceBound()) {
                    long jumpTime = 10000L; // 10 sec

                    PlayerState state = rsc.getService().getPlayerState();
                    if(state != null) {
                        long position = state.getPosition();
                        long jumpPosition = Math.max(0, position - jumpTime);
                        rsc.getService().seekPlayer(jumpPosition);

                        setPosition(jumpPosition, true);
                    }
                }

                showHidingControls(vExtraControls);
            }
        });
        btnJumpForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rsc.isServiceBound()) {
                    long jumpTime = 10000L; // 10 sec

                    PlayerState state = rsc.getService().getPlayerState();
                    if(state != null) {
                        long position = state.getPosition();
                        long jumpPosition = Math.min(position + jumpTime, state.getDuration());
                        rsc.getService().seekPlayer(jumpPosition);

                        setPosition(jumpPosition, true);
                    }
                }

                showHidingControls(vExtraControls);
            }
        });

        Bundle args = getArguments();
        if(args != null) {
            length = args.getLong(EXTRA_PLAYER_DURATION, length);
            volume = args.getLong(EXTRA_PLAYER_VOLUME,   volume);

            String videoFilename = args.getString(EXTRA_PLAYER_VIDEO_FILE);
            txtTitle.setText(videoFilename);
        }

        setLength(length);
        setState(0, volume, false);

        if(rsc.isServiceBound()) {
            PlayerState state = rsc.getService() != null ? rsc.getService().getPlayerState() : null;
            if(state != null) {
                processPlayerState(state);
            }
        }

        return vRoot;
    }
}