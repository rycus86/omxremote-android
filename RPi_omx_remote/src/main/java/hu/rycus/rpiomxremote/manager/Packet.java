package hu.rycus.rpiomxremote.manager;

import java.net.SocketAddress;

/**
 * Created by rycus on 10/30/13.
 */
public class Packet {

    private final int header;
    private final String data;

    public Packet(int header, String data) {
        this.header = header;
        this.data = data;
    }

    public int getHeader() {
        return header;
    }

    public String getData() {
        return data;
    }

}
