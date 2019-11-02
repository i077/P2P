package msg;

import util.PeerConfig;
import util.Values;

import java.net.InetAddress;
import java.util.Objects;

/**
 * Instances of this data class represent responses to queries.
 */
public class Response extends PeerMessage {
    private InetAddress origin;
    private int port;
    private String filename;

    /**
     * Create a response corresponding to a specified query.
     *
     * @param query The query to which the response should be formed.
     */
    public Response(Query query) {
        this.id = query.id;
        this.origin = Values.ownIPAddr();
        this.port = PeerConfig.get().transferPort;
        this.filename = query.getFilename();
    }

    /**
     * Reconstruct a response from given fields.
     *
     * @param id ID of the response
     * @param origin Origin IP address from the response
     * @param port Port from the response
     * @param filename Filename from the response
     */
    public Response(int id, InetAddress origin, int port, String filename) {
        this.id = id;
        this.origin = origin;
        this.port = port;
        this.filename = filename;
    }

    public InetAddress getOrigin() {
        return origin;
    }

    public int getPort() {
        return port;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || this.getClass() != obj.getClass())
            return false;
        Response that = (Response) obj;
        return this.id == that.id && this.filename.equals(that.filename)
                && this.port == that.port && this.origin.getHostAddress().equals(that.origin.getHostAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, origin.getHostAddress(), port, filename);
    }

    @Override
    public String toString() {
        return "R:" + id + ";" + origin.getHostAddress() + ":" + port + ";" + filename + "\004";
    }
}
