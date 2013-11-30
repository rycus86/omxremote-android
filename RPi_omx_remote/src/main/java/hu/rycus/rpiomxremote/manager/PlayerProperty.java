package hu.rycus.rpiomxremote.manager;

import android.content.res.Resources;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Player property descriptor class.
 *
 * Created by Viktor Adam on 11/20/13.
 *
 * @author rycus
 */
public class PlayerProperty {

    /** Player property type. */
    public enum Type {
        /* Text type */   TEXT,
        /* Link type */   LINK,
        /* Poster type */ POSTER
    }

    /** Property key (TV Series): Show title */
    public static final String P_SHOW_TITLE     = "ST";
    /** Property key (TV Series): Episode title */
    public static final String P_EPISODE_TITLE  = "ET";

    /** Property key (TV Series): Episode air date */
    public static final String P_EPISODE_DATE   = "ED";
    /** Property key (TV Series): Episode rating */
    public static final String P_EPISODE_RATING = "ER";

    /** Property key (TV Series): Episode season number */
    public static final String P_EPISODE_NUM_SEASON  = "ENS";
    /** Property key (TV Series): Episode number */
    public static final String P_EPISODE_NUM_EPISODE = "ENE";

    /** Map containing the properties by ID. */
    private static Map<String, PlayerProperty> byId = null;

    /** The identifier of the property. */
    private final String id;
    /** The name of the property. */
    private final String name;
    /** The type of the property. */
    private final Type type;

    /**
     * Private constructor assigning all local variables.
     * @param id   The identifier of the property
     * @param name The name of the property
     * @param type The type of the property
     */
    private PlayerProperty(String id, String name, Type type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    /** @see java.lang.Object#equals(Object) */
    @Override
    public boolean equals(Object o) {
        if(o instanceof PlayerProperty) {
            return ((PlayerProperty) o).id.equals(this.id);
        }

        return super.equals(o);
    }

    /** @see java.lang.Object#hashCode() */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    /** @see java.lang.Object#toString() */
    @Override
    public String toString() {
        return "PROPERTY[" + id + "]: " + name + " (" + type + ")";
    }

    /** Returns the property identified by the given String. */
    public static PlayerProperty get(String id) {
        return byId.get(id);
    }

    /** Returns the list of properties with 'link' type. */
    public static List<PlayerProperty> listLinks() {
        return Arrays.asList(get("SI"), get("EI"));
    }

    /** Returns the list of properties with 'poster' type. */
    public static List<PlayerProperty> listPosters() {
        return Arrays.asList(get("SP"), get("EP"));
    }

    /** Initializes all properties. */
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

        // TODO use resources.getString
    }

}
