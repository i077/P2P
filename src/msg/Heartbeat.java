package msg;

/**
 * Instances of this data class represent heartbeats sent between peers to keep their connections alive.
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
        return "H";
    }
}
