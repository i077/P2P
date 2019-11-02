package msg;

/**
 * Instances of this data class represent heartbeats sent between peers to keep their connections alive.
 * Even though a Heartbeat doesn't really need an ID or most of the properties the PeerMessage abstract class offers,
 * this class still extends PeerMessage for socket functionality.
 */
public class Heartbeat extends PeerMessage {
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || this.getClass() != obj.getClass())
            return false;
        Heartbeat that = (Heartbeat) obj;
        return this.id == that.id;
    }

    @Override
    public String toString() {
        return "H\004";
    }
}
