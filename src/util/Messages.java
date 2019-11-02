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

    public static String PING_SEND = "Sending ping to ";
    public static String PING_RECV = "Received ping from ";
    public static String PONG_SEND = "Sending pong to ";
    public static String PONG_RECV = "Received pong from ";

    public static String HBEAT_SEND = "Sending heartbeat to ";
    public static String HBEAT_TOUT(String ip) {
        return "No heartbeat received from " + ip + " in a while. Closing connection.";
    }
    public static String HBEAT_RECV(String ip) {
        return "Heartbeat recevied from " + ip + ".";
    }

    // Error messages
    public static String SHELL_CNF = " is not a valid command.";
    public static String CONNECT_USAGE = "Usage: connect <IP> <port>";

    public static String ERR_PEERCONFIG = "There was a problem reading the peer configuration.";
    public static String ERR_CHECKIP = "Using external checkip service failed, trying InetAddress.";
    public static String ERR_INETHOSTIP = "Something went wrong when trying to get a host's IP address.";

    public static String ERR_UDP_PKTRECV = "There was a problem receiving a packet from the UDP socket.";
    public static String ERR_UDP_PKTSEND = "There was a problem sending a packet through the UDP socket.";
    public static String ERR_UDP_PORTOPEN = "There was a problem opening a UDP socket.";

    public static String ERR_NOPONGS = "No pongs received after " + Values.PONGWAIT_INTERVAL + "ms.";

    public static String ERR_HBEATSEND(String ip) {
        return "There was a problem sending a heartbeat to " + ip + ".";
    }

    public static String ERR_CONNREAD(String ip) {
        return "There was a problem reading data from the connection with " + ip + ".";
    }

    public static String ERR_SOCKCLOSE = "There was a problem closing a socket.";

    public static String CONN_PKTWEIRD = "Received an unrecognized packet.";
}
