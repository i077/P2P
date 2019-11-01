package msg;

import java.net.InetAddress;
import java.util.Random;

/**
 * Instances of this data class represent queries for files exchanged between peers.
 */
public class Query extends PeerMessage {
    private String filename;
    public InetAddress originAddr;

    /**
     * Create a new query for a certain file.
     * This constructor is called when this peer is creating the query.
     *
     * @param filename The name of the file to query.
     */
    public Query(String filename) {
        this.id = new Random().nextInt(Integer.MAX_VALUE);
        this.filename = filename;
    }

    /**
     * Recreate a passed query.
     * This constructor is called when this peer receives a query from a socket.
     *
     * @param id ID of the received query.
     * @param filename Filename of the received query.
     */
    public Query(int id, String filename) {
        this.id = id;
        this.filename = filename;
    }

    public String getFilepath() {
        return filename;
    }
}
