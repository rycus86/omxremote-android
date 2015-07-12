package hu.rycus.rpiomxremote.blocks;

public class SubtitleMetadata {

    private String show;
    private int season;
    private int episode;

    private String[] providers;

    public String getShow() {
        return show;
    }

    public int getSeason() {
        return season;
    }

    public int getEpisode() {
        return episode;
    }

    public String[] getProviders() {
        return providers != null ? providers : new String[0];
    }
}
