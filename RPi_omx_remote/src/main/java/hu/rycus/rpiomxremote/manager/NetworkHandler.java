package hu.rycus.rpiomxremote.manager;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import hu.rycus.rpiomxremote.util.Flags;
import hu.rycus.rpiomxremote.util.Header;

/**
 * Created by rycus on 10/30/13.
 */
class NetworkHandler extends Thread {

    private static final String LOG_TAG = "RPiOMX|NET";

    private boolean enabled = true;

    private MulticastSocket socket;

    private String sessionID = "???";
    private int bufferSize = 1500;

    private SocketAddress address;

    private final LinkedBlockingQueue<Packet> receivedPackets = new LinkedBlockingQueue<Packet>();

    private final Map<Integer, Packet> incompleteMessages = new HashMap<Integer, Packet>();

    private final RemoteManager manager;
    private final Set<Integer> asynchHeaders;

    NetworkHandler(RemoteManager manager, Set<Integer> asynchHeaders) {
        super("NetworkHandler");

        this.manager        = manager;
        this.asynchHeaders  = asynchHeaders;
    }

    @Override
    public void run() {
        while (enabled) {
            DatagramPacket dp = receivePacket();

            if(enabled && dp != null && dp.getLength() >= 2) {
                manager.setConnected(true);

                byte[] buffer = dp.getData();

                int header  = buffer[0] & 0xFF;
                int flags   = buffer[1] & 0xFF;
                String data = new String(buffer, 2, dp.getLength() - 2);

                boolean finish = (flags & Flags.MORE_FOLLOWS) != Flags.MORE_FOLLOWS;
                Log.d(LOG_TAG,
                        "Packet received (H" + Integer.toHexString(header) + "), " +
                        "length: " + (dp.getLength() - 2) + " | " + (finish ? "Complete" : "INCOMPLETE"));

                if(finish && header == Header.MSG_A_LOGIN) {
                    address = dp.getSocketAddress();

                    Log.e(LOG_TAG, "Datagram packet received, header: 0x" + Integer.toHexString(header) + " source: " + address);

                    String pattern = "^([0-9a-f\\-]+)\\s*\\(([0-9]+)\\)$";
                    sessionID = data.replaceFirst(pattern, "$1");

                    String bufSize = data.replaceFirst(pattern, "$2");
                    bufferSize = Integer.parseInt(bufSize);

                    Log.e(LOG_TAG, "Login result: " + sessionID + " (" + bufSize + ")");

                    continue;
                } else if(finish && header == Header.MSG_A_ERROR_INVALID_SESSION) {
                    Log.e(LOG_TAG, "Invalid session error received");
                    manager.setConnected(false);

                    if(enabled) {
                        sendWithoutSession(Header.MSG_A_LOGIN, "RPi::omxremote");
                        continue;
                    }
                }

                Packet packet = mergeIncomplete(header, data, finish);
                if(packet != null) {
                    if(asynchHeaders.contains(header)) {
                        manager.processAsynchPacket(packet);
                    } else {
                        receivedPackets.offer(packet);
                    }
                }
            } else {
                manager.setConnected(false);
            }
        }
    }

    private Packet mergeIncomplete(int header, String data, boolean finish) {
        Packet incomplete = incompleteMessages.remove(header);

        String newData = incomplete != null ? (incomplete.getData() + data) : data;
        Packet packet = new Packet(header, newData);

        if(finish) {
            return packet;
        } else {
            incompleteMessages.put(header, packet);
            return null;
        }
    }

    Packet poll() {
        return poll(0L);
    }

    Packet pollBlocking() {
        return poll(2500L);
    }

    Packet poll(long timeout) {
        try {
            if(timeout > 0L) {
                return receivedPackets.poll(timeout, TimeUnit.MILLISECONDS);
            } else {
                return receivedPackets.poll();
            }
        } catch (InterruptedException e) { }

        return null;
    }

    boolean initialize() {
        // TODO from database
        String group = "224.1.1.7";
        int port = 42001;

        try {
            socket = new MulticastSocket(port);
            socket.setTimeToLive(2); // TODO magic numbers
            socket.setSoTimeout(10000);
            socket.setLoopbackMode(true);
        } catch(Exception ex) {
            Log.e(LOG_TAG, "Failed to start multicast socket on port " + port, ex);
        }

        if(socket != null) {
            try {
                socket.joinGroup(InetAddress.getByName(group));
                address = new InetSocketAddress(group, port);

                return true;
            } catch(Exception ex) {
                Log.e(LOG_TAG, "Failed to join multicast group at " + group, ex);
            }
        }

        return false;
    }

    void shutdown() {
        enabled = false;
        interrupt();

        if(socket != null) socket.close();

        Log.e(LOG_TAG, "Network handler stopped");
    }

    void setBufferSize(int size) {
        this.bufferSize = size;
    }

    private DatagramPacket receivePacket() {
        try {
            DatagramPacket packet = new DatagramPacket(new byte[bufferSize], bufferSize);
            socket.receive(packet);
            return packet;
        } catch(SocketTimeoutException stex) {
            Log.d(LOG_TAG, "Socket timeout");
        } catch(Exception ex) {
            if(enabled) {
                Log.e(LOG_TAG, "Failed to receive datagram packet", ex);
            }
        }

        return null;
    }

    boolean sendWithoutSession(int header, String data) {
        return send(header, data.getBytes(), Flags.WITHOUT_SESSION_ID);
    }

    boolean send(int header, String data) {
        return send(header, data.getBytes(), 0);
    }

    boolean sendWithoutSession(int header, byte[] data) {
        return send(header, data, Flags.WITHOUT_SESSION_ID);
    }

    boolean send(int header) {
        return send(header, new byte[0]);
    }

    boolean send(int header, byte[] data) {
        return send(header, data, 0);
    }

    private boolean send(int header, byte[] data, int flags) {
        Log.v(LOG_TAG, "Sending H" + Integer.toHexString(header) + ": " + new String(data));

        if( (flags & Flags.WITHOUT_SESSION_ID) != Flags.WITHOUT_SESSION_ID ) {
            byte[] fulldata = new byte[sessionID.length() + data.length];

            System.arraycopy(sessionID.getBytes(), 0, fulldata, 0, sessionID.length());
            System.arraycopy(data, 0, fulldata, sessionID.length(), data.length);

            data = fulldata;
        }

        int maxSize = bufferSize - 2; // BufferSize - (HeaderLength + FlagsLength)
        int toSend = data.length;

        int offset = 0;

        while (toSend > maxSize) {
            flags |= Flags.MORE_FOLLOWS;

            byte[] buffer = new byte[bufferSize];
            buffer[0] = (byte) header;
            buffer[1] = (byte) flags;
            System.arraycopy(data, offset, buffer, 2, maxSize);

            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address);
                socket.send(packet);

                Log.i(LOG_TAG, "Sent " + buffer.length + " bytes");
            } catch(Exception ex) {
                Log.e(LOG_TAG, "Failed to send message to " + address, ex);
                return false;
            }

            offset += maxSize;
            toSend -= maxSize;
        }

        flags &= ~Flags.MORE_FOLLOWS;

        if (toSend > 0 || data.length == 0) {
            byte[] buffer = new byte[2 + data.length];
            buffer[0] = (byte) header;
            buffer[1] = (byte) flags;
            if(data.length > 0) {
                System.arraycopy(data, offset, buffer, 2, data.length);
            }

            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address);
                socket.send(packet);

                Log.i(LOG_TAG, "Sent " + buffer.length + " bytes");
            } catch(Exception ex) {
                Log.e(LOG_TAG, "Failed to send message to " + address, ex);
                return false;
            }
        }

        return true;
    }

}
