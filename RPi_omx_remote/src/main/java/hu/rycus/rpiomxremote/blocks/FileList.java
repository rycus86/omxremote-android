package hu.rycus.rpiomxremote.blocks;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rycus on 10/30/13.
 */
public class FileList implements Parcelable {

    private String path;
    private List<String> files;

    private FileList() { }

    public FileList(String path, List<String> files) {
        this.path = path;
        this.files = files;
    }

    public String getPath() {
        return path;
    }

    public List<String> getFiles() {
        return files;
    }

    @Override
    public int describeContents() {
        return 0;
    }

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

    public static Creator<FileList> CREATOR = new Creator<FileList>() {
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

        @Override
        public FileList[] newArray(int size) {
            return new FileList[size];
        }
    };

}
