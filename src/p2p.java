import net.P2PTopology;
import util.Log;
import util.Messages;
import util.PeerConfig;
import util.Values;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;

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
        System.out.println(Messages.WELCOME_MSG);
        System.out.println(Values.WELCOME_IP());

        // Read config
        PeerConfig config = null;
        try {
            config = new PeerConfig();
        } catch (IOException e) {
            Log.fatal(Messages.ERR_PEERCONFIG, e, -1);
        }

        // Start discovery server+client
        P2PTopology topology = new P2PTopology(config);

        // Start the shell
        shell();
    }

    public static void shell() {
        System.out.println("Ready. Type a command or 'help' for help.");
        Scanner inputScanner = new Scanner(System.in);
        while (true) {
            System.out.print(Log.PROMPT);
            String input;
            try {
                input = inputScanner.nextLine();
            } catch (NoSuchElementException e) {
                // Here there is no more input to read, probably because of an EOF, so just quit.
                break;
            }
        }
    }
}
