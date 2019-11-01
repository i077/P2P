package net;

import msg.Query;
import util.PeerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

/**
 * Class that manages everything about the peer in the context of the network.
 * Holds state dealing with and manages the peer's connections, and handles/makes requests.
 */
public class Peer {
    private ServerSocket welcomeSocket;

    private DiscoveryClient discoveryClient;

    private Map<Integer, Query> queries;

    Peer(PeerConfig config) throws IOException {
        welcomeSocket = new ServerSocket(config.udpServerPort);
    }
}
