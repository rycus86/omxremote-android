package hu.rycus.rpiomxremote.blocks;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class containing a path name and the immediate
 * file (or directory) names in it.
 *
 * <br/>
 * Created by Viktor Adam on 10/30/13.
 *
 * @author rycus
 */
public class FileList implements Parcelable {

    /** The root path of this file list block. */
    private String path;

    /** The file or directory names of the directory at path. */
    private List<String> files;

    /** Contructor to create from Parcelable. */
    private FileList() { }

    /**
     * Public constructor.
     * @param path  T)he root path of this file list block
     * @param files The file or directory names of the directory at path
     */
    public FileList(String path, List<String> files) {
        this.path = path;
        this.files = files;
    }

    /** Returns the root path of this file list block. */
    public String getPath() {
        return path;
    }

    /** Returns the file or directory names of the directory at path. */
    public List<String> getFiles() {
        return files;
    }

    /** @see android.os.Parcelable#describeContents() */
    @Override
    public int describeContents() {
        return 0;
    }

    /** @see android.os.Parcelable#writeToParcel(android.os.Parcel, int) */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeInt(files != null ? files.size() : 0);
        if(files != null) {
            for(String path : files) {
                dest.writeString(path);
            }
        }
    }

    /** @see android.os.Parcelable.Creator */
    public static Creator<FileList> CREATOR = new Creator<FileList>() {

        /** @see android.os.Parcelable.Creator#createFromParcel(android.os.Parcel) */
        @Override
        public FileList createFromParcel(Parcel source) {
            FileList instance = new FileList();

            instance.path = source.readString();

            int size = source.readInt();
            if(size > 0) {
                instance.files = new ArrayList<String>(size);
                for(int index = 0; index < size; index++) {
                    String path = source.readString();
                    instance.files.add(path);
                }
            }

            return instance;
        }

        /** @see android.os.Parcelable.Creator#newArray(int) */
        @Override
        public FileList[] newArray(int size) {
            return new FileList[size];
        }
    };

}
