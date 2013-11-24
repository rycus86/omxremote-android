package hu.rycus.rpiomxremote.util;

/**
* Created by rycus on 11/10/13.
*/
public interface Intents {

    String ACTION_CALLBACK              = "hu.rycus.rpiomxremote.CALLBACK";
    String EXTRA_CONNECTION_STATE       = "rpiomx:connected";
    String EXTRA_ERROR                  = "rpiomx:error";
    String EXTRA_FILE_LIST              = "rpiomx:files";
    String EXTRA_SETTINGS_LIST          = "rpiomx:settings";

    String EXTRA_PLAYER_REPORT          = "rpiomx:player.report";
    String EXTRA_PLAYER_REPORT_INIT     = "init";
    String EXTRA_PLAYER_REPORT_STATE    = "state";
    String EXTRA_PLAYER_REPORT_INFO     = "info";
    String EXTRA_PLAYER_REPORT_EXTRA    = "extra";
    String EXTRA_PLAYER_REPORT_EXIT     = "exit";

    String ACTION_NOTIFICATION_DELETED          = "hu.rycus.rpiomxremote.NOTIFICATION_DELETED";
    String ACTION_NOTIFICATION_BUTTON_CLICKED   = "hu.rycus.rpiomxremote.NOTIFICATION_BUTTON_CLICKED";

    String EXTRA_NOTIFICATION_BUTTON_ID = "button";

}
