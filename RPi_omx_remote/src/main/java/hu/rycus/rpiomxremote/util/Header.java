package hu.rycus.rpiomxremote.util;

/**
 * Created by rycus on 10/30/13.
 */
public interface Header {

    int MSG_A_LOGIN         = 0xA1;
    int MSG_A_LIST_FILES    = 0xA2;
    int MSG_A_START_VIDEO   = 0xA3;
    int MSG_A_STOP_VIDEO    = 0xA4;
    int MSG_A_SET_VOLUME    = 0xA5;
    int MSG_A_SEEK_TO       = 0xA6;
    int MSG_A_PAUSE         = 0xA7;
    int MSG_A_SPEED_INC     = 0xA8;
    int MSG_A_SPEED_DEC     = 0xA9;
    int MSG_A_SUB_DELAY_INC = 0xAA;
    int MSG_A_SUB_DELAY_DEC = 0xAB;
    int MSG_A_SUB_TOGGLE    = 0xAC;
    int MSG_A_PLAYER_STATE  = 0xD1;
    int MSG_A_PLAYER_PARAMS = 0xD2;
    int MSG_A_PLAYER_INFO   = 0xD3;
    int MSG_A_PLAYER_EXTRA  = 0xD4;
    int MSG_A_KEEPALIVE     = 0xE0;
    int MSG_A_LIST_SETTINGS = 0xE1;
    int MSG_A_SET_SETTING   = 0xE2;
    int MSG_A_ERROR         = 0xF0;
    int MSG_A_EXIT          = 0xFE;

    int MSG_A_ERROR_INVALID_SESSION = 0xF1;

}
