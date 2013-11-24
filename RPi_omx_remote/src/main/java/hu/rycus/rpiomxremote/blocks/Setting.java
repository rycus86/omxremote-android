package hu.rycus.rpiomxremote.blocks;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by rycus on 11/21/13.
 */
public class Setting implements Parcelable {

    public enum Type {
        TEXT, NUMBER, ENUM, SWITCH
    }

    private final String key;
    private final String description;
    private final String possibleValues;
    private final Type type;

    private String value;

    public Setting(String key, String value, String description, String possibleValues, Type type) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.possibleValues = possibleValues;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) { this.value = value; }

    public String getDescription() {
        return description;
    }

    public String getPossibleValues() {
        return possibleValues;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Setting) {
            return ((Setting) o).key.equals(this.key);
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "Setting[" + type + "]: " + key + "=" + value;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeString(value);
        dest.writeString(description);
        dest.writeString(possibleValues);
        dest.writeInt(type.ordinal());
    }

    public static Parcelable.Creator<Setting> CREATOR = new Creator<Setting>() {
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

        @Override
        public Setting[] newArray(int size) {
            return new Setting[size];
        }
    };

}
