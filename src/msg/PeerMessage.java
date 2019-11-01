package msg;

import java.util.Objects;

/**
 * Abstract class representing a message sent by a peer.
 */
public abstract class PeerMessage {
    int id;

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
