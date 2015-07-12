package hu.rycus.rpiomxremote.manager.v1;

import java.net.SocketAddress;

/**
 * An object containing the data received from the server.
 *
 * <br/>
 * Created by Viktor Adam on 10/30/13.
 *
 * @author rycus
 */
public class Packet {

    /** The header of the packet. */
    private final int header;
    /** The contents of the packet as String. */
    private final String data;

    /**
     * Constructor to create a packet with header and data contents.
     * @param header The header of the packet
     * @param data   The contents of the packet as String
     */
    public Packet(int header, String data) {
        this.header = header;
        this.data = data;
    }

    /** Returns the header of the packet. */
    public int getHeader() { return header; }

    /** Returns the contents of the packet as String. */
    public String getData() { return data; }

}
