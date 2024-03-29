package util;

import msg.Query;
import msg.Response;

/**
 * Class used to provide messages.
 */
public class Messages {
    // Debug / logging messages
    public static String WELCOME_MSG = "Starting P2P node...";
    public static String WELCOME_IP() { return "Your current IP address is: " + Values.ownIPAddr().getHostAddress(); }
    public static String SHELL_READY = "Ready. Type a command.";

    public static String TRDN_CONNCLOSING = "Closing all neighboring connections...";
    public static String TRDN_NOCONNS = "No connections to close.";
    public static String TRDN_CONNCLOSED = "Successfully closed all neighboring connections and left the P2P network.";

    public static String PING_SEND = "Sending ping to ";
    public static String PING_RECV = "Received ping from ";
    public static String PONG_SEND = "Sending pong to ";
    public static String PONG_RECV = "Received pong from ";

    public static String CONN_ACPT(String ip) {
        return "Accepting new connection from " + ip + ".";
    }
    public static String CONN_FAILURE(String ip) {
        return "Failed to establish a new connection with " + ip + ".";
    }
    public static String CONN_SUCCESS(String ip) {
        return "Successfully established new connection with " + ip + ".";
    }

    public static String HBEAT_SEND = "Sending heartbeat to ";
    public static String HBEAT_TOUT(String ip) {
        return "No heartbeat received from " + ip + " in " + Values.HEARTBEAT_INTERVAL / 1000 + "s. Closing connection.";
    }
    public static String HBEAT_RECV(String ip) {
        return "Heartbeat received from " + ip + ".";
    }

    public static String QUERY_SEND(Query q, String ip) {
        return "Sending query for \"" + q.getFilename() + "\" to " + ip + ".";
    }
    public static String QUERY_RECV(Query q) {
        return "Received query from " + q.originAddr.getHostAddress() + " for file \"" + q.getFilename() + "\".";
    }
    public static String QUERY_HASFILE(Query q) {
        return "Found matching file \"" + q.getFilename() + "\" for query from " + q.originAddr + ".";
    }
    public static String QUERY_NOHASFILE(Query q) {
        return "Did not find matching file \"" + q.getFilename() + "\" for query from " + q.originAddr + ".";
    }
    public static String QUERY_FWD(Query q, String ip) {
        return "Forwarding query for \"" + q.getFilename() + "\" to " + ip + ".";
    }

    public static String RESP_SEND(Response r, String ip) {
        return "Sending response for \"" + r.getFilename() + "\" to " + ip + ".";
    }

    public static String TFER_ACPT(String ip) {
        return "Accepting new transfer request from " + ip + ".";
    }
    public static String REQ_TFER(Response r) {
        return "Requesting a transfer of file \"" + r.getFilename() + "\" from " + r.getOrigin().getHostAddress() + ".";
    }
    public static String TFER_REQRECV(String filename, String ip) {
        return "Received a request from " + ip + " to transfer file \"" + filename + "\".";
    }
    public static String TFER_FINISHED(String filename, String ip) {
        return "Sent file \"" + filename + "\" to " + ip + ".";
    }

    // Error messages
    public static String SHELL_CNF = " is not a valid command.";
    public static String CONNECT_USAGE = "Usage: connect <IP> <port>";
    public static String GET_USAGE = "Usage: get <file>";

    public static String ERR_PEERCONFIG = "There was a problem reading the peer configuration.";
    public static String ERR_CHECKIP = "Using external checkip service failed, trying InetAddress.";
    public static String ERR_INETHOSTIP = "Something went wrong when trying to get a host's IP address.";

    public static String ERR_UDP_PKTRECV = "There was a problem receiving a packet from the UDP socket.";
    public static String ERR_UDP_PKTSEND = "There was a problem sending a packet through the UDP socket.";
    public static String ERR_UDP_PORTOPEN = "There was a problem opening a UDP socket.";

    public static String ERR_NOPONGS = "No pongs received after " + Values.PONGWAIT_INTERVAL + "ms.";

    public static String ERR_WLCMACCEPT = "There was a problem accepting a new connection from the welcome socket.";
    public static String ERR_TFERACCEPT = "There was a problem accepting a new connection from the transfer socket.";
    public static String ERR_HBEATSEND(String ip) {
        return "There was a problem sending a heartbeat to " + ip + ".";
    }

    public static String ERR_CONNREAD(String ip) {
        return "There was a problem reading data from the connection with " + ip + ".";
    }

    public static String ERR_QUERYSEND(String ip) {
        return "There was a problem sending a new query to " + ip + ".";
    }
    public static String ERR_QUERYFWD(String ip) {
        return "There was a problem forwarding a query to " + ip + ".";
    }

    public static String ERR_RESPSEND(String ip) {
        return "There was a problem sending a new response to " + ip + ".";
    }
    public static String ERR_RESPUNK = "Received an unknown response.";
    public static String ERR_RESPFWD(String ip) {
        return "There was a problem forwarding a reponse to " + ip + ".";
    }

    public static String ERR_SOCKOPEN = "There was a problem opening a socket.";
    public static String ERR_SOCKCLOSE = "There was a problem closing a socket.";

    public static String CONN_PKTWEIRD = "Received an unrecognized packet.";

    public static String ERR_TFER_REQSEND = "There was a problem requesting a transfer.";
    public static String ERR_TFER_SEND = "There was a problem sending a file.";

    public static String ERR_FILEREAD = "There was a problem reading a file.";
    public static String ERR_FILEWRITE = "There was a problem writing to a file.";
}
