package hu.rycus.rpiomxremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import java.util.Map;

import hu.rycus.rpiomxremote.manager.PlayerProperty;
import hu.rycus.rpiomxremote.manager.PlayerState;
import hu.rycus.rpiomxremote.ui.PlayerFragment;
import hu.rycus.rpiomxremote.util.Intents;

public class PlayerActivity extends ActionBarActivity {

    private final RemoteServiceCreator rsc = new RemoteServiceCreator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_player);

        if (savedInstanceState == null) {
            PlayerFragment fragment = new PlayerFragment();
            fragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(Intents.ACTION_CALLBACK);
        LocalBroadcastManager.getInstance(this).registerReceiver(callbackReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(callbackReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        rsc.bind(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        rsc.unbind(this);
    }

    private final BroadcastReceiver callbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( Intents.ACTION_CALLBACK.equals(intent.getAction()) ) {
                if( intent.hasExtra(Intents.EXTRA_ERROR) ) {
                    String error = intent.getStringExtra(Intents.EXTRA_ERROR);
                    Toast.makeText(
                            PlayerActivity.this,
                            getResources().getString(R.string.error_general, error),
                            Toast.LENGTH_SHORT).show();
                } else if( intent.hasExtra(Intents.EXTRA_PLAYER_REPORT) ) {
                    String type = intent.getStringExtra(Intents.EXTRA_PLAYER_REPORT);
                    if( Intents.EXTRA_PLAYER_REPORT_STATE.equals(type) ) {
                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
                        if(fragment != null && fragment instanceof PlayerFragment) {
                            PlayerState state = rsc.isServiceBound() ? rsc.getService().getPlayerState() : null;
                            if(state != null) {
                                long position  = state.getPosition();
                                long volume    = state.getVolume();
                                boolean paused = state.isPaused();

                                ((PlayerFragment) fragment).setState(position, volume, paused);
                            }
                        }
                    } else if( Intents.EXTRA_PLAYER_REPORT_INFO.equals(type) ) {
                        PlayerState state = rsc.isServiceBound() ? rsc.getService().getPlayerState() : null;
                        if(state == null) return;

                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
                        if(fragment != null && fragment instanceof PlayerFragment) {
                            ((PlayerFragment) fragment).processPlayerState(state);
                        }
                    } else if( Intents.EXTRA_PLAYER_REPORT_EXTRA.equals(type) ) {
                        PlayerState state = rsc.isServiceBound() ? rsc.getService().getPlayerState() : null;
                        if(state == null) return;

                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
                        if(fragment != null && fragment instanceof PlayerFragment) {
                            ((PlayerFragment) fragment).processPlayerState(state);
                        }
                    } else if( Intents.EXTRA_PLAYER_REPORT_EXIT.equals(type) ) {
                        Toast.makeText(
                                PlayerActivity.this,
                                getResources().getText(R.string.pl_finished),
                                Toast.LENGTH_LONG).show();

                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
                        if(fragment != null && fragment instanceof PlayerFragment) {
                            ((PlayerFragment) fragment).onPlaybackFinished();
                        }

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                PlayerActivity.this.finish();
                            }
                        }, 2500);
                    }
                }
            }
        }
    };

}
