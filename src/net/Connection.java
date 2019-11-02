package net;

import msg.Heartbeat;
import msg.PeerMessage;
import msg.Query;
import msg.Response;
import util.Log;
import util.Messages;
import util.PeerConfig;
import util.Values;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Represents a connection between two peers, used for exchanging queries/responses.
 *
 * Each peer in a connection has this object so its Peer instance can manage their respective sides.
 */
public class Connection {
    InetAddress neighborAddr;
    private Socket connectionSocket;

    private Map<Integer, Query> queries;
    private final Map<InetAddress, Connection> connections;

    private final List<File> sharedFileList;

    // Use different timers to make sure one task doesn't block the other
    private Timer heartbeat, reader;

    // Different threads access this, so make it volatile
    private volatile long lastHeartbeatTime;

    /**
     * Thread that listens on socket and sends heartbeat via two Timers.
     */
    public Thread listener;

    public Connection(Socket socket,
                      Map<Integer, Query> queries,
                      final Map<InetAddress, Connection> connections) {
        this.neighborAddr = socket.getInetAddress();
        this.connectionSocket = socket;
        this.queries = queries;
        this.connections = connections;
        this.sharedFileList = PeerConfig.get().sharedFileList;

        heartbeat = new Timer();
        reader = new Timer();

        this.listener = new Thread(new Runnable() {
            @Override
            public void run() {
                // Set up heartbeat send/receiver
                lastHeartbeatTime = System.currentTimeMillis();
                heartbeat.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        // Send a heartbeat if we've seen one and the socket is still alive, close otherwise
                        if (Connection.this.isAlive() &&
                                System.currentTimeMillis() - lastHeartbeatTime < Values.HEARTBEAT_INTERVAL) {
                            Log.i(Messages.HBEAT_SEND + neighborAddr.getHostAddress());

                            // Send heartbeat!
                            try {
                                Connection.this.sendPeerMessage(new Heartbeat());
                            } catch (IOException e) {
                                if (Connection.this.isAlive())
                                    Log.e(Messages.ERR_HBEATSEND(neighborAddr.getHostAddress()), e);
                            }
                        } else {
                            Log.i(Messages.HBEAT_TOUT(neighborAddr.getHostAddress()));
                            connections.remove(Connection.this.neighborAddr);
                            Connection.this.teardown();
                        }
                    }
                }, 0, Values.HEARTBEAT_INTERVAL);

                // Set up reading timer
                reader.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        byte[] recvBuf = new byte[2048];
                        int recvLen = -1;
                        do {
                            try {
                                recvLen = connectionSocket.getInputStream().read(recvBuf);
                            } catch (IOException e) {
                                // Only log an error if the connection is still alive, otherwise stop
                                if (Connection.this.isAlive())
                                    Log.e(Messages.ERR_CONNREAD(neighborAddr.getHostAddress()), e);
                                else return;
                            }
                        } while (Connection.this.isAlive() && recvLen < 0);

                        Connection.this.processPacket(recvBuf, recvLen);
                    }
                }, 0, Values.READER_INTERVAL);
            }
        });
        // This listener should not keep the peer alive.
        listener.setDaemon(true);
    }

    /**
     * Returns whether this connection is still alive, i.e. whether the socket's connection is still active.
     *
     * @return true if the socket is connected, false otherwise
     */
    public boolean isAlive() {
        return !connectionSocket.isClosed() && connectionSocket.isConnected();
    }

    /**
     * Write a peer message to the socket, sending it to the other peer.
     *
     * @param msg The PeerMessage to send to the other peer.
     * @throws IOException if the socket had a problem sending the message.
     */
    void sendPeerMessage(PeerMessage msg) throws IOException {
        connectionSocket.getOutputStream().write(msg.toString().getBytes());
    }

    /**
     * Teardown this connection.
     * Closes all sockets, cancels all timers, and stops all threads.
     *
     * This should be called if the connection becomes stale (i.e. no heartbeat was received within the timeout interval),
     * or if the client is exiting.
     */
    void teardown() {
        heartbeat.cancel();
        reader.cancel();

        try {
            connectionSocket.close();
        } catch (IOException e) {
            Log.e(Messages.ERR_SOCKCLOSE, e);
        }
    }

    /**
     * Process an incoming packet on the socket.
     * If this packet isn't a heartbeat (which just requires logging and updating one field),
     * control is passed to another function that handles that respective type of message.
     *
     * Either this packet is:
     *
     * - A heartbeat, in which case we should update the last received time.
     * - A query, in which case we should check if this host has the file, otherwise forward the query.
     * - A response, in which case we should see if we sent the query, otherwise forward the response back up the query path.
     *
     * @param pktData Byte array of the incoming packet that was sent over the socket.
     */
    private void processPacket(byte[] pktData, int pktLen) {
        String message = new String(pktData, 0, pktLen).trim(); // Exclude end-of-transmission character

        switch (message.charAt(0)) {
            case 'H': // Packet is a heartbeat
                Log.i(Messages.HBEAT_RECV(neighborAddr.getHostAddress()));
                lastHeartbeatTime = System.currentTimeMillis();
                break;
            case 'Q': // Packet is a query
                // Reconstruct and process query
                String[] queryParts = message.substring(2) // Exclude "Q:"
                        .split(";");
                Query query = new Query(Integer.parseInt(queryParts[0]), queryParts[1]);
                query.originAddr = connectionSocket.getInetAddress();
                processQuery(query);
                break;
            case 'R': // Packet is a response
                // Reconstruct and process response
                String[] responseParts = message.substring(2)
                        .split(";");
                String[] addressParts = responseParts[1].split(":");
                try {
                    Response response = new Response(
                        Integer.parseInt(responseParts[0]),
                        InetAddress.getByName(addressParts[0]),
                        Integer.parseInt(addressParts[1]),
                        responseParts[2]
                    );
                    processResponse(response);
                } catch (UnknownHostException e) {
                    Log.e(Messages.ERR_RESPUNK, e);
                }

                break;
            default:
                Log.e(Messages.CONN_PKTWEIRD);
        }
    }

    /**
     * Process an incoming query. Called by processPacket().
     *
     * If this peer has the file requested in the query, then we immediately send a response and (implicitly) discard the query.
     * Otherwise, we save the query in the queries map and forward the query to every other connection.
     *
     * To avoid infinitely forwarding queries for files that no peers have, we do nothing if we have seen this query before.
     *
     * @param query The incoming query.
     */
    private void processQuery(Query query) {
        Log.i(Messages.QUERY_RECV(query));

        // Do nothing if we've seen this query before to avoid infinite forwards.
        if (queries.containsKey(query.getId()))
            return;

        boolean hasFile = false;
        // Check if this peer has the requested file
        for (File f : sharedFileList) {
            if (query.getFilename().equals(f.getName())) {
                hasFile = true;
            }
        }

        if (hasFile) {
            Log.i(Messages.QUERY_HASFILE(query));
            Response newResponse = new Response(query);

            Log.i(Messages.RESP_SEND(newResponse, connectionSocket.getInetAddress().getHostAddress()));
            try {
                sendPeerMessage(newResponse);
            } catch (IOException e) {
                if (this.isAlive()) // This connection may be dead, so only log an error if it is alive
                    Log.e(Messages.ERR_RESPSEND(this.neighborAddr.getHostAddress()), e);
            }
        } else {
            // This peer doesn't have the file, so we can't immediately send a response.
            Log.i(Messages.QUERY_NOHASFILE(query));

            // Instead, save this query for later and propagate it to other connections.
            queries.put(query.getId(), query);

            for (Connection c : connections.values()) {
                // We don't want to forward the query to this connection
                if (c == this || c.equals(this))
                    continue;

                try {
                    Log.i(Messages.QUERY_FWD(query, c.neighborAddr.getHostAddress()));
                    c.sendPeerMessage(query);
                } catch (IOException e) {
                    if (c.isAlive()) // This connection may be dead, so only log an error if it is alive
                        Log.e(Messages.ERR_QUERYFWD(c.neighborAddr.getHostAddress()), e);
                }
            }
        }
    }

    /**
     * Process an incoming response packet. Called by processPacket().
     *
     * If this host sent this response, then we should immediately begin a transfer.
     * Otherwise, if we have the corresponding query in the queries map,
     * forward the response up to the original sender of that query.
     *
     * @param response The incoming response.
     */
    private void processResponse(Response response) {
        // If we don't have the corresponding query, log an error and discard
        if (!queries.containsKey(response.getId())) {
            Log.e(Messages.ERR_RESPUNK);
            return;
        }

        // At this point, we have the query, so pull it from the map
        Query coQuery = queries.remove(response.getId());

        // Since Peer.requestFile() doesn't give the query an origin, we can assume that
        // if coQuery.originAddr == null, this query came from this host.
        if (coQuery.originAddr == null) {
            Log.i(Messages.REQ_TFER(response));
            // TODO Initiate transfer
        } else {
            // Forward the response through the appropriate connection
            Connection originConn = connections.get(coQuery.originAddr);
            try {
                originConn.sendPeerMessage(response);
            } catch (IOException e) {
                if (originConn.isAlive()) // Connection may be dead
                    Log.e(Messages.ERR_RESPFWD(originConn.neighborAddr.getHostAddress()));
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionSocket.getInetAddress().getHostAddress());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || this.getClass() != obj.getClass())
            return false;
        Connection that = (Connection) obj;
        // Treat connections with the same peer as the same
        return this.connectionSocket.getInetAddress().getHostAddress()
                .equals(that.connectionSocket.getInetAddress().getHostAddress());
    }
}
