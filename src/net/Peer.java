package net;

import msg.Query;
import util.Log;
import util.Messages;
import util.PeerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that manages everything about the peer in the context of the network.
 * Holds state dealing with and manages the peer's connections, and handles/makes requests.
 */
public class Peer {
    private ServerSocket welcomeSocket;

    private DiscoveryClient discoveryClient;

    private Map<Integer, Query> queries;
    // TODO Should this be a Set?
    private Map<String, Connection> connections; // Maps IP address to connection

    public Peer(PeerConfig config) throws IOException {
        welcomeSocket = new ServerSocket(config.udpServerPort);

        discoveryClient = new DiscoveryClient(config);
        discoveryClient.listener.start();

        // Queries and connections are accessed by different threads, so make them thread-safe
        queries = Collections.synchronizedMap(new HashMap<Integer, Query>());
        connections = Collections.synchronizedMap(new HashMap<String, Connection>());
    }

    public void connect(String ip, int port) {
        try {
            discoveryClient.sendConnectPing(this, ip, port);
        } catch (IOException e) {
            Log.e(Messages.ERR_UDP_PKTSEND, e);
        }
    }

    /**
     * Teardown this peer. Closes all connections and interrupts all threads.
     * Used for a clean exit.
     */
    public void teardown() {
        discoveryClient.teardown();
        for (Connection c : connections.values()) {
            c.teardown();
        }
    }
}
