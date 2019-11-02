package net;

import util.Log;
import util.Messages;
import util.PeerConfig;
import util.Values;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;

/**
 * Class that sends discovery packets to other peers, forming the P2P network.
 */
public class DiscoveryClient {
    private DatagramSocket udpSocket;

    // Different threads will access this, so make it volatile
    private volatile boolean listenerRunning;

    private Map<String, Integer> discoveredPeers; // Maps IP to port.
    private final List<String> recvdPongs;

    private int udpPort;
    private final int welcomePort; // Needed for sending pongs.

    /**
     * Thread that will listen on the socket for new packets.
     */
    public Thread listener;

    DiscoveryClient(PeerConfig config) throws SocketException {
        this.udpPort = config.udpClientPort;
        this.welcomePort = config.welcomePort;
        this.udpSocket = new DatagramSocket(this.udpPort);

        discoveredPeers = new HashMap<>();
        recvdPongs = new ArrayList<>();

        listener = new Thread(new Runnable() {
            @Override
            public void run() {
                listenerRunning = true;
                while (listenerRunning) {
                    // Receive a packet, then send it to the processor method.
                    byte[] recvBuf = new byte[2048];
                    DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
                    try {
                        udpSocket.receive(recvPacket);
                    } catch (IOException e) {
                        // Log an error only if the listener should be running.
                        // If it isn't, we can assume the socket was closed and ignore the thrown exception,
                        // which will probably say as much.
                        if (listenerRunning)
                            Log.e(Messages.ERR_UDP_PKTRECV, e);
                    }
                    processPacket(recvPacket);
                }
            }
        });
        // The listener should not keep the peer running.
        listener.setDaemon(true);
    }

    /**
     * Send a ping to a specified peer and wait for a pong.
     *
     * @param ip IP address of the peer
     * @param port Port of the peer
     */
    void sendConnectPing(final Peer peer, String ip, final int port) throws IOException {
        // Create and send ping
        String pingMsg = "PI:" + Values.ownIPAddr() + ":" + this.udpPort + "\004";
        sendPing(pingMsg.getBytes(), ip, port);

        discoveredPeers.put(ip, port);

        // Wait for pong, then connect to two peers
        final Thread pongListener = new Thread(new Runnable() {
            @Override
            public void run() {
                // Wait for recvdPongs to fill (or until timeout).
                synchronized (recvdPongs) {
                    try {
                        recvdPongs.wait(Values.PONGWAIT_INTERVAL);
                    } catch (InterruptedException ignored) {}
                    // Ignore if this thread was interrupted,
                    // because that probably means someone called teardown() or is otherwise exiting the peer.
                }

                // At this point, we've received at least one pong, so we should connect to two random peers.
                if (recvdPongs.isEmpty()) {
                    Log.e(Messages.ERR_NOPONGS);
                    return;
                }
                Random r = new Random();
                for (int i = 0; i < 2; i++) {
                    if (recvdPongs.isEmpty())
                        break;
                    String recvdPong = recvdPongs.get(r.nextInt(recvdPongs.size()));
                    // Extract IP address and port from pong
                    String[] pongMsgParts = recvdPong.trim().split(":");
                    String pongIP = pongMsgParts[1];
                    int pongPort = Integer.parseInt(pongMsgParts[2]);

                    // Tell the calling peer to establish a connection
                    try {
                        peer.addNeighbor(pongIP, pongPort);
                        recvdPongs.remove(recvdPong);
                    } catch (IOException e) {
                        Log.e(Messages.CONN_FAILURE(pongIP), e);
                    }
                }
            }
        });
        // This thread shouldn't keep the peer alive.
        pongListener.setDaemon(true);
        pongListener.start();
    }

    /**
     * Parse a packet received on the discovery socket.
     *
     * Either this packet is:
     *
     * - A ping, in which case we should add the sender to our peers, send a pong, and propagate the ping, or
     * - A pong, which signifies an acknowledgement of a sent discovery packet. In this case, we open a new socket.
     * Since this should only happen when we issue the "connect" command to the sender of this packet, this is handled
     * by a Timer as setup by sendConnectPing().
     *
     * @param packet The packet that was received
     */
    private void processPacket(DatagramPacket packet) {
        byte[] pktData = packet.getData();
        String pktMessage = new String(pktData);

        String[] msgParts = pktMessage.trim().split(":");
        switch(msgParts[0]) {
            case "PI": // This packet is a ping
                String pingIP = msgParts[1];
                int pingPort = Integer.parseInt(msgParts[2]);
                Log.i(Messages.PING_RECV + Values.ipPortStr(pingIP, pingPort));

                // Send a response pong
                try {
                    sendPong(pingIP, pingPort);
                } catch (IOException e) {
                    Log.e(Messages.ERR_UDP_PKTSEND, e);
                }

                // If we haven't seen this peer before, propagate it
                if (!discoveredPeers.containsKey(pingIP)) {
                    discoveredPeers.put(pingIP, pingPort);

                    propagatePing(pktData, pingIP);
                }
                break;
            case "PO": // This packet is a pong
                Log.i(Messages.PONG_RECV + Values.ipPortStr(msgParts[1], Integer.parseInt(msgParts[2])));
                synchronized (recvdPongs) {
                    recvdPongs.add(pktMessage);
                    recvdPongs.notify();
                }
        }
    }

    /**
     * Propagate a given ping message to each of this host's neighbors.
     *
     * @param pingMsgData Packet data of ping
     * @param pingIP Ping's sender's IP address
     */
    private void propagatePing(byte[] pingMsgData, String pingIP) {
        for (Map.Entry<String, Integer> peer : discoveredPeers.entrySet()) {
            String peerIP = peer.getKey();
            int peerPort = peer.getValue();

            // Don't send this ping to its sender
            if (!peerIP.equals(pingIP)) {
                try {
                    sendPing(pingMsgData, peerIP, peerPort);
                } catch (IOException e) {
                    Log.e(Messages.ERR_UDP_PKTSEND, e);
                }
            }
        }
    }

    /**
     * Send a constructed ping to a specified destination.
     *
     * @param pingMsgData Packet data of ping
     * @param destIP IP address of destination
     * @param destPort Port number of destination
     */
    private void sendPing(byte[] pingMsgData, String destIP, int destPort) throws IOException {
        Log.i(Messages.PING_SEND + Values.ipPortStr(destIP, destPort));

        InetAddress destAddr = InetAddress.getByName(destIP);
        DatagramPacket pingPkt = new DatagramPacket(pingMsgData, pingMsgData.length, destAddr, destPort);

        // Send ping!
        udpSocket.send(pingPkt);
    }

    /**
     * Send a pong as a response to a given ping.
     *
     * @param pingIP The ping's sender's IP address
     * @param pingPort The ping's sender's port number
     */
    private void sendPong(String pingIP, int pingPort) throws IOException {
        Log.i(Messages.PONG_SEND + Values.ipPortStr(pingIP, pingPort));

        // Construct pong, attaching this host's IP address and port
        String pongMsgBuilder = "PO:" + Values.ownIPAddr() + ":" + this.welcomePort + "\004";
        byte[] pongMsgData = pongMsgBuilder.getBytes();
        InetAddress destAddr = InetAddress.getByName(pingIP);
        DatagramPacket pongPkt = new DatagramPacket(pongMsgData, pongMsgData.length, destAddr, pingPort);

        // Send pong!
        udpSocket.send(pongPkt);
    }

    /**
     * Teardown this client.
     * Interrupts all threads and closes all sockets.
     */
    void teardown() {
        listenerRunning = false;
        listener.interrupt();
        udpSocket.close();
    }
}
