package util;

/**
 * Class used to provide messages.
 */
public class Messages {
    // Debug / logging messages
    public static String WELCOME_MSG = "Starting P2P node...";
    public static String PEER_LISTENER_START = "Starting discovery server...";
    public static String PEER_BROADCASTER_START = "Starting discovery client...";
    public static String SHELL_READY = "Ready. Type a command or 'help' for help.";

    // Error messages
    public static String SHELL_CNF = "is not a valid command.";

    public static String ERR_PEERCONFIG = "There was a problem reading the peer configuration.";
    public static String ERR_CHECKIP = "Using external checkip service failed, trying InetAddress.";
    public static String ERR_INETHOSTIP = "Something went wrong when trying to get a host's IP address.";

    public static String ERR_SOCKOPEN1 = "There was a problem opening a socket at port ",
            ERR_SOCKOPEN2 = ".";
    public static String ERR_UDP_PKTRECV = "There was a problem receiving a packet from the UDP socket.";
    public static String ERR_UDP_PKTSEND = "There was a problem sending a packet through the UDP socket.";
    public static String ERR_UDP_PORTOPEN = "There was a problem opening a UDP socket.";
}
