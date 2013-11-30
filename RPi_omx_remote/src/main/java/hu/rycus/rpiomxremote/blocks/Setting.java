package hu.rycus.rpiomxremote.blocks;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class representing a remote setting
 * as defined in <i>omxremote-py</i>.
 *
 * <br/>
 * Created by Viktor Adam on 11/21/13.
 *
 * @author rycus
 */
public class Setting implements Parcelable {

    /** Value type enumeration for settings. */
    public enum Type {
        /** Text type */             TEXT,
        /** Decimal number type */   NUMBER,
        /** Enumerated value type */ ENUM,
        /** Boolean value type */    SWITCH
    }

    /** The setting key (as defined in omxplayer). */
    private final String key;
    /** The description of the setting. */
    private final String description;
    /**
     * The possible values for the setting separated by comma.
     * Only valid for enumerated value types.
     */
    private final String possibleValues;
    /** The type of the setting. */
    private final Type type;
    /** The current value of the setting. */
    private String value;

    /**
     * Public constructor to create an instance from values
     * received from the <i>omxremote-py</i> server.
     * @param key             The setting key (as defined in omxplayer)
     * @param value           The initial value of the setting
     * @param description     The description of the setting
     * @param possibleValues  The possible values for the setting separated by comma
     *                        (only valid for enumerated value types)
     * @param type            The type of the setting
     */
    public Setting(String key, String value, String description, String possibleValues, Type type) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.possibleValues = possibleValues;
        this.type = type;
    }

    /** Returns the setting key (as defined in omxplayer). */
    public String getKey() { return key; }

    /** Returns the current value of the setting. */
    public String getValue() { return value; }
    /** Sets the current value of the setting. */
    public void setValue(String value) { this.value = value; }

    /** Returns the description of the setting. */
    public String getDescription() { return description; }

    /**
     * Returns the possible values for the setting separated by comma.
     * Only valid for enumerated value types.
     */
    public String getPossibleValues() { return possibleValues; }

    /** Returns the type of the setting. */
    public Type getType() { return type; }

    /** @see java.lang.Object#equals(Object) */
    @Override
    public boolean equals(Object o) {
        if(o instanceof Setting) {
            return ((Setting) o).key.equals(this.key);
        }
        return super.equals(o);
    }

    /** @see java.lang.Object#hashCode() */
    @Override
    public int hashCode() {
        return key.hashCode();
    }

    /** @see java.lang.Object#toString() */
    @Override
    public String toString() {
        return "Setting[" + type + "]: " + key + "=" + value;
    }

    /** @see android.os.Parcelable#describeContents() */
    @Override
    public int describeContents() { return 0; }

    /** @see android.os.Parcelable#writeToParcel(android.os.Parcel, int) */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeString(value);
        dest.writeString(description);
        dest.writeString(possibleValues);
        dest.writeInt(type.ordinal());
    }

    /** @see android.os.Parcelable.Creator */
    public static Parcelable.Creator<Setting> CREATOR = new Creator<Setting>() {

        /** @see android.os.Parcelable.Creator#createFromParcel(android.os.Parcel) */
        @Override
        public Setting createFromParcel(Parcel source) {
            String key = source.readString();
            String value = source.readString();
            String description = source.readString();
            String possibleValues = source.readString();
            int typeOrdinal = source.readInt();
            Type type = Type.values()[typeOrdinal];
            return new Setting(key, value, description, possibleValues, type);
        }

        /** @see android.os.Parcelable.Creator#newArray(int) */
        @Override
        public Setting[] newArray(int size) {
            return new Setting[size];
        }
    };

}
