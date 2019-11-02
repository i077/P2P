package net;

import msg.Query;
import util.Log;
import util.Messages;
import util.PeerConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that manages everything about the peer in the context of the network.
 * Holds state dealing with and manages the peer's connections, and handles/makes requests.
 */
public class Peer {
    private ServerSocket welcomeSocket;

    /**
     * Thread to listen for any incoming connections
     */
    public Thread welcomeSocketListener;
    private boolean welcomeListenerRunning;

    private DiscoveryClient discoveryClient;

    private Map<Integer, Query> queries;
    private Map<InetAddress, Connection> connections; // Maps IP address to connection

    public Peer() throws IOException {
        welcomeSocket = new ServerSocket(PeerConfig.get().welcomePort);

        welcomeSocketListener = new Thread(new Runnable() {
            @Override
            public void run() {
                welcomeListenerRunning = true;
                while (welcomeListenerRunning) {
                    try {
                        // Block for incoming connections
                        Socket newSocket = welcomeSocket.accept();
                        Connection newConnection = new Connection(newSocket, queries, connections);
                        Log.i(Messages.CONN_ACPT(newSocket.getInetAddress().getHostAddress()));

                        putConnection(newConnection);
                    } catch (IOException e) {
                        Log.e(Messages.ERR_WLCMACCEPT, e);
                    }
                }
            }
        });
        welcomeSocketListener.setDaemon(true);
        welcomeSocketListener.start();

        discoveryClient = new DiscoveryClient();
        discoveryClient.listener.start();

        // Queries and connections are accessed by different threads, so make them thread-safe
        queries = Collections.synchronizedMap(new HashMap<Integer, Query>());
        connections = Collections.synchronizedMap(new HashMap<InetAddress, Connection>());
    }

    public void connect(String ip, int port) {
        try {
            discoveryClient.sendConnectPing(this, ip, port);
        } catch (IOException e) {
            Log.e(Messages.ERR_UDP_PKTSEND, e);
        }
    }

    /**
     * Send a query for a file, specified by name, over all connections.
     *
     * @param filename The name of the file to request.
     */
    public void requestFile(String filename) {
        Query newQuery = new Query(filename);
        for (Connection c : connections.values()) {
            Log.i(Messages.QUERY_SEND(newQuery, c.neighborAddr.getHostAddress()));
            try {
                c.sendPeerMessage(newQuery);
            } catch (IOException e) {
                // Don't want to stop sending to all connections just because one failed
                if (c.isAlive())
                    Log.e(Messages.ERR_QUERYSEND(c.neighborAddr.getHostAddress()));
            }
        }
    }

    /**
     * Add a new neighboring connection with a host of the specified IP address and port.
     * This method is used by the DiscoveryClient when it receives a pong from a connect command.
     *
     * @param ip The IP address of the new peer
     * @param port The port of the new peer
     */
    void addNeighbor(String ip, int port) throws IOException {
        InetAddress peerAddr = InetAddress.getByName(ip);
        Socket newSocket = new Socket(peerAddr, port);

        Connection newConn = new Connection(newSocket, queries, connections);
        putConnection(newConn);
    }

    /**
     * Put the specified connection into the map of connections, closing the previous one with the same host if it exists.
     * The connection is put in the map only if it is alive, i.e. the socket is connected.
     *
     * @param conn The connection to put
     */
    private void putConnection(Connection conn) {
        Connection lastConn = connections.get(conn.neighborAddr);

        if (lastConn != null && lastConn.isAlive()) {
            lastConn.teardown();
        }

        // Fail if this connection didn't connect or otherwise died
        if (!conn.isAlive()) {
            Log.i(Messages.CONN_FAILURE(conn.neighborAddr.getHostAddress()));
            return;
        }

        Log.i(Messages.CONN_SUCCESS(conn.neighborAddr.getHostAddress()));
        conn.listener.start();
        connections.put(conn.neighborAddr, conn);
    }

    public void closeAllConnections() {
        // Don't need to do anything if no connections exist.
        if (connections.isEmpty()) {
            Log.i(Messages.TRDN_NOCONNS);
            return;
        }

        Log.i(Messages.TRDN_CONNCLOSING);
        for (Connection c : connections.values()) {
            c.teardown();
        }
        connections.clear();
        Log.i(Messages.TRDN_CONNCLOSED);
    }

    /**
     * Teardown this peer. Closes all connections and stops accepting new ones.
     * Used for a clean exit.
     */
    public void teardown() {
        discoveryClient.teardown();
        closeAllConnections();
        // Stop accepting new connections
        welcomeListenerRunning = false;
        welcomeSocketListener.interrupt();
    }
}
