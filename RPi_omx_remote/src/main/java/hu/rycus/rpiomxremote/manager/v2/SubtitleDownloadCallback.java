package hu.rycus.rpiomxremote.manager.v2;

public interface SubtitleDownloadCallback {

    void onDownloaded(String filename);

    void onFailed();

}
