package msg;

import java.util.Objects;
import java.util.Random;

/**
 * Abstract class representing a message sent by a peer.
 */
public abstract class PeerMessage {
    int id;

    public int getId() {
        return id;
    }

    public PeerMessage() {
        id = new Random().nextInt(Integer.MAX_VALUE);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Connection messages must have an explicit string form that is sent over a socket
    abstract public String toString();
}
