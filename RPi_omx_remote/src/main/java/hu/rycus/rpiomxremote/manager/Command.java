package hu.rycus.rpiomxremote.manager;

/**
 * An object containing the data to send to the server.
 *
 * <br/>
 * Created by Viktor Adam on 10/30/13.
 *
 * @author rycus
 */
public class Command {

    /** The header of the command. */
    private final int header;
    /** The contents of the command as byte array. */
    private final byte[] data;

    /**
     * Constructor to create a command with no data.
     * @param header The header of the command
     */
    public Command(int header) {
        this.header = header;
        this.data = new byte[0];
    }

    /**
     * Constructor to create a command from data as byte array.
     * @param header The header of the command
     * @param data The contents of the command as byte array
     */
    public Command(int header, byte[] data) {
        this.header = header;
        this.data = data != null ? data : new byte[0];
    }

    /**
     * Constructor to create a command from data as String.
     * @param header The header of the command
     * @param data The contents of the command as String
     */
    public Command(int header, String data) {
        this.header = header;
        this.data = data != null ? data.getBytes() : new byte[0];
    }

    /** Returns the header of the command. */
    public int getHeader() { return header; }

    /** Returns the contents of the command as byte array. */
    public byte[] getData() { return data; }

    /**
     * Returns the contents of the command as String
     * (converted from byte array data using the system's default character set).
     */
    public String getStringData() { return new String(data); }

}
