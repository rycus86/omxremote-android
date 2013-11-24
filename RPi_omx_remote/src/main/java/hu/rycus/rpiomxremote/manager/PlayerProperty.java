package hu.rycus.rpiomxremote.manager;

import android.content.res.Resources;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by rycus on 11/20/13.
 */
public class PlayerProperty {

    public enum Type {
        TEXT, LINK, POSTER
    }

    public static final String P_SHOW_TITLE     = "ST";
    public static final String P_EPISODE_TITLE  = "ET";

    public static final String P_EPISODE_DATE   = "ED";
    public static final String P_EPISODE_RATING = "ER";

    public static final String P_EPISODE_NUM_SEASON  = "ENS";
    public static final String P_EPISODE_NUM_EPISODE = "ENE";

    private static Map<String, PlayerProperty> byId = null;

    private final String id;
    private final String name;
    private final Type type;

    private PlayerProperty(String id, String name, Type type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof PlayerProperty) {
            return ((PlayerProperty) o).id.equals(this.id);
        }

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "PROPERTY[" + id + "]: " + name + " (" + type + ")";
    }

    public static PlayerProperty get(String id) {
        return byId.get(id);
    }

    public static List<PlayerProperty> listLinks() {
        return Arrays.asList(get("SI"), get("EI"));
    }

    public static List<PlayerProperty> listPosters() {
        return Arrays.asList(get("SP"), get("EP"));
    }

    // TODO use resources.getString
    static void initialize(Resources resources) {
        if(byId != null) return;

        byId = new HashMap<String, PlayerProperty>();

        // show properties
        byId.put("ST", new PlayerProperty("ST", "Show title",   Type.TEXT));
        byId.put("SI", new PlayerProperty("SI", "IMDB link",    Type.LINK));
        byId.put("SP", new PlayerProperty("SP", "Poster",       Type.POSTER));
        // episode properties
        byId.put("ET", new PlayerProperty("ET", "Episode title",        Type.TEXT));
        byId.put("ED", new PlayerProperty("ED", "Episode airdate",      Type.TEXT));
        byId.put("ER", new PlayerProperty("ER", "Episode rating",       Type.TEXT));
        byId.put("EI", new PlayerProperty("EI", "Episode IMDB link",    Type.LINK));
        byId.put("EP", new PlayerProperty("EP", "Episode poster",       Type.POSTER));
        byId.put("ENS", new PlayerProperty("ENS", "Season number",      Type.TEXT));
        byId.put("ENE", new PlayerProperty("ENE", "Episode number",     Type.POSTER));
    }

}
