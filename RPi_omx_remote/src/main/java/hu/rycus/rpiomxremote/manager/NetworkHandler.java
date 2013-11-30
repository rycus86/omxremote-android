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
 * Network handler class responsible for low-level network communication
 * with the remote <i>omxremote-py</i> server.
 *
 * <br/>
 * Created by Viktor Adam on 10/30/13.
 *
 * @author rycus
 */
class NetworkHandler extends Thread {

    /** Tag for logcat. */
    private static final String LOG_TAG = "RPiOMX|NET";

    /** Is this handler still enabled? */
    private boolean enabled = true;

    /** The UDP/Multicast socket used for communication. */
    private MulticastSocket socket;

    /** The session ID used in communication (as sent by the remote server. */
    private String sessionID = "???";
    /** The buffer size (as defined by the remote server. */
    private int bufferSize = 1500;

    /** The last known address of the remote server. */
    private SocketAddress address;

    /**
     * Blocking queue containing the received but unprocessed packets
     * (should be empty because there should always be a consumer for the next packet).
     */
    private final LinkedBlockingQueue<Packet> receivedPackets = new LinkedBlockingQueue<Packet>();

    /** Map containing incomplete multipart packets by header. */
    private final Map<Integer, Packet> incompleteMessages = new HashMap<Integer, Packet>();

    /** The remote manager instance which created this handler. */
    private final RemoteManager manager;
    /**
     * Set containing header types which should be processed asynchronously.
     * At this level it means that they won't be offered to the packet queue,
     * they will be forwarded to the manager instead to process it directly.
     */
    private final Set<Integer> asynchHeaders;

    /**
     * Package-private constructor with the creator/owner of the instance.
     * @param manager       The remote manager instance which created this handler
     * @param asynchHeaders Set containing header types which should be processed asynchronously
     */
    NetworkHandler(RemoteManager manager, Set<Integer> asynchHeaders) {
        super("NetworkHandler");

        this.manager        = manager;
        this.asynchHeaders  = asynchHeaders;
    }

    /**
     * <p>
     *     This basically waits for an UDP packet,
     *     merges it with a previous one if it was multipart,
     *     then either sends it to the remote manager to process it
     *     or queues it to have it processed elsewhere.
     * </p>
     * <p>
     *     This code automatically handles session parameter changes
     *     and reconnections in case of communication loss.
     * </p>
     *
     * @see Runnable#run()
     */
    @Override
    public void run() {
        while (enabled) {
            // wait for a packet
            DatagramPacket dp = receivePacket();

            if(enabled && dp != null && dp.getLength() >= 2) {
                // signal connection OK
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
                    // set session parameters
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
                    // signal connection loss
                    manager.setConnected(false);

                    if(enabled) {
                        // retry login
                        sendWithoutSession(Header.MSG_A_LOGIN, "RPi::omxremote");
                        continue;
                    }
                }

                // create a new version of a packet, possibly by merging this to a previous one
                Packet packet = mergeIncomplete(header, data, finish);
                if(packet != null) {
                    // process this packet
                    if(asynchHeaders.contains(header)) {
                        manager.processAsynchPacket(packet);
                    } else {
                        receivedPackets.offer(packet);
                    }
                }
            } else {
                // we are not connected anymore
                manager.setConnected(false);
            }
        }
    }

    /**
     * Creates a new version of a packet, possibly by merging the given data into a previous packet.
     * @param header The header of the received packet
     * @param data   The (possibly partial) data of a packet
     * @param finish Is this the last packet? (or else more follows)
     * @return A new version of a packet with the given header
     */
    private Packet mergeIncomplete(int header, String data, boolean finish) {
        Packet incomplete = incompleteMessages.remove(header);

        // concatenate data
        String newData = incomplete != null ? (incomplete.getData() + data) : data;
        Packet packet = new Packet(header, newData);

        if(finish) {
            return packet;
        } else {
            incompleteMessages.put(header, packet);
            return null;
        }
    }

    /**
     * Returns a queued packet immediately if there is any
     * or null if there aren't any received packets queued.
     */
    Packet poll() { return poll(0L); }

    /** Polls a queued packet waiting 2.5 seconds at most to receive one. */
    Packet pollBlocking() { return poll(2500L); }

    /** Polls a queued packet waiting for the given time interval at most to receive one. */
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

    /**
     * Initializes the connection by setting the address, port number and other settings
     * of the multicast UDP socket, then joins the multicast group.
     */
    boolean initialize() {
        // TODO from database
        String group = "224.1.1.7";
        int port = 42001;

        try {
            socket = new MulticastSocket(port);
            socket.setTimeToLive(4); // TODO magic numbers
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

    /** Stops this network handler instance. */
    void shutdown() {
        enabled = false;
        interrupt();

        if(socket != null) socket.close();

        Log.e(LOG_TAG, "Network handler stopped");
    }

    /** Helper method to receive one UDP datagram packet. */
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

    /**
     * Sends a command with the given header and data contents
     * without prefixing it with the session ID.
     */
    boolean sendWithoutSession(int header, String data) {
        return send(header, data.getBytes(), Flags.WITHOUT_SESSION_ID);
    }
    /**
     * Sends a command with the given header and data contents
     * without prefixing it with the session ID.
     */
    boolean sendWithoutSession(int header, byte[] data) {
        return send(header, data, Flags.WITHOUT_SESSION_ID);
    }
    /** Sends a command with the given header and data contents prefixing it with the session ID. */
    boolean send(int header, String data) {
        return send(header, data.getBytes(), 0);
    }
    /** Sends a command with the given header and no content prefixing it with the session ID. */
    boolean send(int header) {
        return send(header, new byte[0]);
    }
    /** Sends a command with the given header and data contents prefixing it with the session ID. */
    boolean send(int header, byte[] data) {
        return send(header, data, 0);
    }
    /**
     * Sends a command with the given header and data contents
     * modifying it according to the given flags.
     */
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

        // send multipart chunks
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

        // send the rest of the message
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
