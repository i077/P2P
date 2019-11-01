package net;

import util.Log;
import util.Messages;
import util.PeerConfig;
import util.Values;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class containing the peer-to-peer topology for the network.
 * Contains 2 UDP socket used to discover and negotiate ports with new peers.
 */
public class P2PTopology {
    private ArrayList<DatagramPacket> recvdPackets;
    private DatagramSocket udpServerSocket, udpClientSocket;
    private PeerConfig peerConfig;

    public Map<String, Integer> peers;

    /**
     * These threads run all blocking calls involving sockets.
     * One thread listens on the server socket, waiting for other peers to send packets.
     * The other thread sends out packets on the client, waiting for
     */
    public Thread listener, broadcaster;

    public P2PTopology(PeerConfig config) {
        this.peerConfig = config;
        int     udpServerPort = config.udpServerPort,
                udpClientPort = config.udpClientPort;

        this.peers = new ConcurrentHashMap<>();
        this.recvdPackets = new ArrayList<>();

        try {
            this.udpServerSocket = new DatagramSocket(udpServerPort);
            this.udpServerSocket.setBroadcast(true);
            this.udpClientSocket = new DatagramSocket(udpClientPort);
            this.udpClientSocket.setBroadcast(true);
        } catch (SocketException e) {
            Log.fatal(Messages.ERR_UDP_PORTOPEN, e, 2);
        }
    }

    /**
     * Listen for discovery packets from other peers.
     * This method never returns and should not be started from the main thread.
     *
     * @throws IOException if the socket had a problem receiving data
     */
    private void listen(String IP, int port) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] rcvBuf = new byte[2048];
                DatagramPacket rcvPacket = new DatagramPacket(rcvBuf, rcvBuf.length);
                try {
                    udpServerSocket.receive(rcvPacket); // Blocks until packet is received
                } catch (IOException e) {
                    Log.i(Messages.ERR_UDP_PKTRECV);
                }
                System.err.println("Received a discovery packet!");

                String pktData = new String(rcvPacket.getData());
                System.out.println("Packet data: " + pktData);
            }
        });
        System.out.println(Messages.PEER_LISTENER_START);
    }

    /**
     * Send pings to the broadcast address, containing the peer's IP and next available TCP port.
     * This method never returns and should not be started from the main thread.
     */
    private void broadcast() throws IOException {
        // Wait for everyone's discovery listeners to start up.
        try {
            Thread.sleep(500);
            Log.i(Messages.PEER_BROADCASTER_START);

            InetAddress broadcastAddr = InetAddress.getByName(Values.BROADCAST_IP);
            while (!this.peerConfig.availTCPPorts.isEmpty()) {
                // Construct packet
                int nextAvailPort = peerConfig.availTCPPorts.get(0);
                byte[] pingData = ("PI:" + Values.ownIPAddr() + ":" + nextAvailPort).getBytes();
                DatagramPacket pingPacket = new DatagramPacket(pingData, pingData.length, broadcastAddr, peerConfig.udpServerPort);

                // Send packet to broadcast address
                udpClientSocket.send(pingPacket);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            Log.e(Messages.ERR_INETHOSTIP, e);
        }
    }

    /**
     * Stops all threads and closes all sockets.
     */
    public void close() {
        // Interrupt listener and broadcaster threads
        listener.interrupt();

        // Close all sockets
        udpServerSocket.close();
        udpClientSocket.close();
    }
}
