package hu.rycus.rpiomxremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import hu.rycus.rpiomxremote.blocks.FileList;
import hu.rycus.rpiomxremote.blocks.Setting;
import hu.rycus.rpiomxremote.manager.PlayerState;
import hu.rycus.rpiomxremote.ui.AboutFragment;
import hu.rycus.rpiomxremote.ui.FileListFragment;
import hu.rycus.rpiomxremote.ui.PlayerFragment;
import hu.rycus.rpiomxremote.ui.SettingsFragment;
import hu.rycus.rpiomxremote.ui.StatusFragment;
import hu.rycus.rpiomxremote.util.Intents;

/**
 * Main/launcher activity of the application.
 * It is able to display:
 * <ul>
 *     <li>Status fragment</li>
 *     <li>File list fragment</li>
 *     <li>Settings fragment</li>
 * </ul>
 *
 * Created by Viktor Adam.
 *
 * @author rycus
 */
public class MainActivity extends ActionBarActivity {

    /** Helper object to bind/unbind the remote service. */
    private final RemoteServiceCreator rsc = new RemoteServiceCreator();

    /** @see android.app.Activity#onCreate(android.os.Bundle) */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new StatusFragment())
                    .commit();
        }
    }

    /** @see android.app.Activity#onResume() */
    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter callbackFilter = new IntentFilter(Intents.ACTION_CALLBACK);
        LocalBroadcastManager.getInstance(this).registerReceiver(callbackReceiver, callbackFilter);
    }

    /** @see android.app.Activity#onPause() */
    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(callbackReceiver);
    }

    /** @see android.app.Activity#onStart() */
    @Override
    protected void onStart() {
        super.onStart();

        rsc.bind(this);
    }

    /** @see android.app.Activity#onStop() */
    @Override
    protected void onStop() {
        super.onStop();

        rsc.unbind(this);
    }

    /**
     * Changes the currently displayed fragment.
     * @param fragment    The fragment to display
     * @param transition  The transition used in the change animation
     * @param toBackStack Add the fragment to the back stack?
     */
    public void changeFragment(Fragment fragment, int transition, boolean toBackStack) {
        FragmentTransaction tx = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .setTransition(transition);

        if(toBackStack) {
            tx.addToBackStack(null);
        }

        tx.commit();
    }

    /** Starts the player activity if there is an active playback. */
    private void startPlayerActivity() {
        PlayerState state = rsc.isServiceBound() ? rsc.getService().getPlayerState() : null;
        if(state != null) {
            Intent startIntent = new Intent(MainActivity.this, PlayerActivity.class);
            startIntent.putExtra(PlayerFragment.EXTRA_PLAYER_VIDEO_FILE, state.getVideofile());
            startIntent.putExtra(PlayerFragment.EXTRA_PLAYER_DURATION,   state.getDuration());
            startIntent.putExtra(PlayerFragment.EXTRA_PLAYER_VOLUME,     state.getVolume());
            startActivity(startIntent);
        }
    }

    /** Switches to the settings fragment. */
    private void displaySettingsFragment(Setting[] settings) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if(currentFragment != null && currentFragment instanceof  SettingsFragment) return;

        SettingsFragment fragment = new SettingsFragment();

        Bundle bundle = new Bundle();
        bundle.putParcelableArray(Intents.EXTRA_SETTINGS_LIST, settings);
        fragment.setArguments(bundle);

        changeFragment(fragment, FragmentTransaction.TRANSIT_FRAGMENT_OPEN, true);
    }

    /** @see android.app.Activity#onCreateOptionsMenu(android.view.Menu) */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /** @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem) */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            {
                getSupportFragmentManager().popBackStack();
                return true;
            }
            case R.id.action_settings:
            {
                if(rsc.isServiceBound()) {
                    rsc.getService().requestSettings();
                }
                return true;
            }
            case R.id.action_list:
            {
                if(rsc.isServiceBound()) {
                    rsc.getService().requestFileList(null);
                }
                return true;
            }
            case R.id.action_player:
            {
                startPlayerActivity();
                return true;
            }
            case R.id.action_about:
            {
                changeFragment(new AboutFragment(), FragmentTransaction.TRANSIT_FRAGMENT_OPEN, true);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /** Broadcast receiver to receive callbacks from the remote service. */
    private final BroadcastReceiver callbackReceiver = new BroadcastReceiver() {

        /**
         * @see android.content.BroadcastReceiver
         *      #onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if( Intents.ACTION_CALLBACK.equals(intent.getAction()) ) {
                // errors
                if( intent.hasExtra(Intents.EXTRA_ERROR) ) {
                    String error = intent.getStringExtra(Intents.EXTRA_ERROR);
                    Toast.makeText(
                            MainActivity.this,
                            getResources().getString(R.string.error_general, error),
                            Toast.LENGTH_SHORT).show();
                }
                // file list
                else if( intent.hasExtra(Intents.EXTRA_FILE_LIST) ) {
                    FileList files = intent.getParcelableExtra(Intents.EXTRA_FILE_LIST);
                    if(files != null) {
                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
                        if(fragment != null && fragment instanceof FileListFragment) {
                            ((FileListFragment) fragment).setFiles(files);
                        } else {
                            fragment = new FileListFragment();
                            Bundle bundle = new Bundle();
                            bundle.putParcelable(Intents.EXTRA_FILE_LIST, files);
                            fragment.setArguments(bundle);
                            changeFragment(fragment, FragmentTransaction.TRANSIT_FRAGMENT_OPEN, true);
                        }
                    } else {
                        Toast.makeText(
                                MainActivity.this,
                                getResources().getText(R.string.error_list_files),
                                Toast.LENGTH_SHORT).show();
                    }
                }
                // player started
                else if( intent.hasExtra(Intents.EXTRA_PLAYER_REPORT) ) {
                    String type = intent.getStringExtra(Intents.EXTRA_PLAYER_REPORT);
                    if( Intents.EXTRA_PLAYER_REPORT_INIT.equals(type) ) {
                        startPlayerActivity();
                    }
                }
                // connection state changed
                else if( intent.hasExtra(Intents.EXTRA_CONNECTION_STATE) ) {
                    boolean connected = intent.getBooleanExtra(Intents.EXTRA_CONNECTION_STATE, false);
                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
                    if(fragment != null && fragment instanceof StatusFragment) {
                        ((StatusFragment) fragment).setConnected(connected);
                    }
                }
                // settings list ready
                else if( intent.hasExtra(Intents.EXTRA_SETTINGS_LIST) ) {
                    Setting[] settings = (Setting[]) intent.getParcelableArrayExtra(Intents.EXTRA_SETTINGS_LIST);
                    displaySettingsFragment(settings);
                }
            }
        }
    };

}
