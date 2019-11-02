package net;

import msg.Heartbeat;
import msg.PeerMessage;
import msg.Query;
import util.Log;
import util.Messages;
import util.Values;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Represents a connection between two peers, used for exchanging queries/responses.
 *
 * Each peer in a connection has this object so its Peer instance can manage their respective sides.
 */
public class Connection {
    private InetAddress neighborAddr;
    private Socket connectionSocket;

    private Map<Integer, Query> queries;
    private final Map<String, Connection> connections;

    // Use different timers to make sure one task doesn't block the other
    private Timer heartbeat, reader;

    private long lastHeartbeatTime;

    /**
     * Thread that listens on socket and sends heartbeat via two Timers.
     */
    public Thread listener;

    public Connection(InetAddress ip, int port,
                      Map<Integer, Query> queries,
                      Map<String, Connection> connections) throws IOException {
        this.neighborAddr = ip;
        this.connectionSocket = new Socket(ip, port);
        this.queries = queries;
        this.connections = connections;

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
                        if (isAlive() &&
                                System.currentTimeMillis() - lastHeartbeatTime < Values.HEARTBEAT_INTERVAL) {
                            Log.i(Messages.HBEAT_SEND + neighborAddr.getHostAddress());

                            // Send heartbeat!
                            try {
                                sendPeerMessage(new Heartbeat());
                            } catch (IOException e) {
                                Log.e(Messages.ERR_HBEATSEND(neighborAddr.getHostAddress()), e);
                            }
                        } else {
                            Log.i(Messages.HBEAT_TOUT(neighborAddr.getHostAddress()));
                            teardown();
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
                                Log.e(Messages.ERR_CONNREAD(neighborAddr.getHostAddress()), e);
                            }
                        } while (isAlive() && recvLen < 0);

                        processPacket(recvBuf, recvLen);
                    }
                }, 0, Values.READER_INTERVAL);
            }
        });
    }

    /**
     * Returns whether this connection is still alive, i.e. whether the socket's connection is still active.
     *
     * @return true if the socket is connected, false otherwise
     */
    public boolean isAlive() {
        return connectionSocket.isConnected();
    }

    /**
     * Write a peer message to the socket, sending it to the other peer.
     *
     * @param msg The PeerMessage to send to the other peer.
     * @throws IOException if the socket had a problem sending the message.
     */
    private void sendPeerMessage(PeerMessage msg) throws IOException {
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
    }
}
