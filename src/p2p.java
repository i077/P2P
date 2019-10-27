import net.P2PTopology;
import util.PeerConfig;
import util.Values;

import java.io.IOException;

/**
 * Class used as entry point into P2P node program.
 */
public class p2p {

    /**
     * Start the peer.
     *
     * This method mostly does setup, then passes control to a pseudo-shell interface.
     */
    public static void main(String[] args) {
        System.out.println(Values.WELCOME_MSG);
        System.out.println(Values.WELCOME_IP());

        // Read config
        PeerConfig config = null;
        try {
            config = new PeerConfig();
        } catch (IOException e) {
            System.err.println("There was a problem reading in configuration.");
            e.printStackTrace();
            System.exit(-2);
        }
    }
}
