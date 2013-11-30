package hu.rycus.rpiomxremote.util;

/**
 * Utility class containing flags for network communication.
 *
 * <br/>
 * Created by Viktor Adam on 10/30/13.
 *
 * @author rycus
 */
public interface Flags {

    /** This flag is set when a packet is not the last one of a multipart packet. */
    int MORE_FOLLOWS        = 0x01 << 0;
    /** This flag is set when an outgoing packet shouldn't be prefixed with session ID. */
    int WITHOUT_SESSION_ID  = 0x01 << 1;

}
