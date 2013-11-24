package hu.rycus.rpiomxremote.manager;

/**
 * Created by rycus on 10/30/13.
 */
public class Command {

    private final int header;
    private final byte[] data;

    public Command(int header) {
        this.header = header;
        this.data = new byte[0];
    }

    public Command(int header, byte[] data) {
        this.header = header;
        this.data = data != null ? data : new byte[0];
    }

    public Command(int header, String data) {
        this.header = header;
        this.data = data != null ? data.getBytes() : new byte[0];
    }

    public int getHeader() {
        return header;
    }

    public byte[] getData() {
        return data;
    }

    public String getStringData() {
        return new String(data);
    }
}
