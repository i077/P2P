/**
 * Class used as entry point into P2P node program.
 */
public class p2p {
    /**
     * Start the Peer-to-Peer node.
     * This method mostly does setup, then passes control to a pseudo-shell interface.
     */
    public static void main(String[] args) {
        System.out.println(Values.WELCOME_MSG);
        System.out.println(Values.WELCOME_IP);
        System.out.println("Starting P2P node...");
    }
}
