import net.Peer;
import util.Log;
import util.Messages;
import util.PeerConfig;

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
        System.out.println(Messages.WELCOME_IP());

        // Make sure config is read into singleton class before starting peer.
        assert PeerConfig.get() != null;

        // Start the shell
        Peer peer = null;
        try {
            peer = new Peer();
        } catch (IOException e) {
            Log.fatal("", e, 1);
        }
        shell(peer);
    }

    /**
     * Spawn a shell that controls a given peer.
     * This method doesn't return until a fatal error occurs or the user chooses to exit.
     *
     * @param peer The peer that the shell controls.
     */
    private static void shell(Peer peer) {
        Scanner inputScanner = new Scanner(System.in);
        String input;

        System.out.println(Messages.SHELL_READY);
        boolean running = true;
        while (running) {
            System.out.print(Log.PROMPT);
            try {
                input = inputScanner.nextLine();
            } catch (NoSuchElementException e) {
                // Here there is no more input to read, probably because of an EOF, so just quit.
                System.out.println();
                break;
            }

            // Ignore empty input and prompt again
            if (input.isEmpty())
                continue;

            // Parse input
            String[] argv = input.split(" ");
            switch (argv[0]) {
                case "connect":
                case "Connect":
                    if (argv.length != 3) {
                        System.err.println(Messages.CONNECT_USAGE);
                        continue;
                    }
                    String ip = argv[1];
                    int port = Integer.parseInt(argv[2]);
                    peer.connect(ip, port);
                    break;
                case "get":
                case "Get":
                    if (argv.length != 2) {
                        System.err.println(Messages.GET_USAGE);
                        continue;
                    }
                    peer.requestFile(argv[1]);
                    break;
                case "leave":
                case "Leave":
                    peer.closeAllConnections();
                    break;
                case "exit":
                case "Exit":
                    running = false;
                    break;
                default:
                    System.err.println(argv[0] + Messages.SHELL_CNF);
            }

            System.out.flush();
        }

        // At this point, the peer should no longer be running, so tear it down.
        peer.teardown();
    }
}
