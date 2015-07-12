package hu.rycus.rpiomxremote.manager;

public interface RemoteManager {

    /** Stops this remote manager instance. */
    void shutdown();

    /** Returns true if this manager is connected and has an active session to the server. */
    boolean isConnected();

    /** Returns the current state of the remote player. */
    PlayerState getPlayerState();

    /** Requests remote file list for the given path. */
    void listFiles(String path);

    /** Requests starting remote playback of a video with the given video and subtitle path. */
    void startPlayer(String videoPath, String subtitlePath);

    /** Requests stopping the remote player. */
    void stopPlayer();

    /** Requests remote settings from the server. */
    void requestSettings();

    /** Requests modifying the value of a remote setting. */
    void setSetting(String key, String value);

    void ctrlPlayPause();

    void ctrlVolume(long value);

    void ctrlSeek(long position);

    void ctrlIncreaseSpeed();

    void ctrlDecreaseSpeed();

    void ctrlIncreaseSubtitleDelay();

    void ctrlDecreaseSubtitleDelay();

    void ctrlToggleSubtitleVisibility();

}
