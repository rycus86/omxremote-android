package hu.rycus.rpiomxremote.util;

/**
 * Utility class containing action and extra names for intent parameters.
 *
 * <br/>
 * Created by Viktor Adam on 11/10/13.
 *
 * @author rycus
 */
public interface Intents {

    /** Intent action for callbacks. */
    String ACTION_CALLBACK              = "hu.rycus.rpiomxremote.CALLBACK";
    /** Intent extra key for connection state callback. */
    String EXTRA_CONNECTION_STATE       = "rpiomx:connected";
    /** Intent extra key for error callback. */
    String EXTRA_ERROR                  = "rpiomx:error";
    /** Intent extra key for file list callback. */
    String EXTRA_FILE_LIST              = "rpiomx:files";
    /** Intent extra key for settings list callback. */
    String EXTRA_SETTINGS_LIST          = "rpiomx:settings";

    /** Intent extra key for player state report callback. */
    String EXTRA_PLAYER_REPORT          = "rpiomx:player.report";
    /** Intent extra value for player state type: initialization. */
    String EXTRA_PLAYER_REPORT_INIT     = "init";
    /** Intent extra value for player state type: state report. */
    String EXTRA_PLAYER_REPORT_STATE    = "state";
    /** Intent extra value for player state type: information. */
    String EXTRA_PLAYER_REPORT_INFO     = "info";
    /** Intent extra value for player state type: extra information. */
    String EXTRA_PLAYER_REPORT_EXTRA    = "extra";
    /** Intent extra value for player state type: player exit. */
    String EXTRA_PLAYER_REPORT_EXIT     = "exit";

    /** Intent action for when notification is dismissed. */
    String ACTION_NOTIFICATION_DELETED          = "hu.rycus.rpiomxremote.NOTIFICATION_DELETED";
    /** Intent action for when a notification button is clicked. */
    String ACTION_NOTIFICATION_BUTTON_CLICKED   = "hu.rycus.rpiomxremote.NOTIFICATION_BUTTON_CLICKED";

    /** Intent extra key for a notification button what was clicked. */
    String EXTRA_NOTIFICATION_BUTTON_ID = "button";

}
