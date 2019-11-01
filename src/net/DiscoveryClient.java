package net;

import util.Log;
import util.Messages;
import util.PeerConfig;
import util.Values;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that sends discovery packets to other peers, forming the P2P network.
 */
public class DiscoveryClient {
    private DatagramSocket udpSocket;
    private volatile boolean listenerRunning;

    private Map<String, Integer> discoveryPeers; // Maps IP to port.
    private List<String> pongs;

    private int welcomePort;

    /**
     * Thread that will listen on the socket for new packets.
     */
    public Thread listener;

    public DiscoveryClient(PeerConfig config) throws SocketException {
        this.welcomePort = config.welcomePort;
        this.udpSocket = new DatagramSocket(config.udpClientPort);

        discoveryPeers = new HashMap<>();
        pongs = new ArrayList<>();

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
                        Log.e(Messages.ERR_UDP_PKTRECV, e);
                    }
                    processPacket(recvPacket);
                }
            }
        });
    }

    /**
     * Parse a packet received on the discovery socket.
     *
     * Either this packet is:
     *
     * - A ping, in which case we should add the sender to our peers, send a pong, and propagate the ping, or
     * - A pong, which signifies an acknowledgement of a sent discovery packet. In this case, we open a new socket.
     * Since this should only happen when we issue the "connect" command to the sender of this packet, this is handled
     * by a Timer.
     *
     * @param packet The packet that was received
     */
    private void processPacket(DatagramPacket packet) {
        String pktMessage = new String(packet.getData());

        String[] msgParts = pktMessage.split(":");
        switch(msgParts[0]) {
            case "PI": // This packet is a ping
                String pingIP = msgParts[1];
                int pingPort = Integer.parseInt(msgParts[2]);

                // If we haven't seen this peer before, process it
                if (!discoveryPeers.containsKey(pingIP)) {
                    discoveryPeers.put(pingIP, pingPort);

                    try {
                        sendPong(pingIP, pingPort);
                    } catch (IOException e) {
                        Log.e(Messages.ERR_UDP_PKTSEND, e);
                    }
                    // TODO propagate ping
                }
                break;
            case "PO": // This packet is a pong
                pongs.add(pktMessage);
        }
    }

    /**
     * Send a pong as a response to a given ping.
     *
     * @param pingIP The ping's sender's IP address
     * @param pingPort The ping's sender's port number
     */
    private void sendPong(String pingIP, int pingPort) throws IOException {
        // Construct pong, attaching this host's IP address and port
        String pongMsgBuilder = "PO:" + Values.ownIPAddr() + ":" + this.welcomePort;
        byte[] pongMsgData = pongMsgBuilder.getBytes();
        InetAddress destAddr = InetAddress.getByName(pingIP);
        DatagramPacket pongPkt = new DatagramPacket(pongMsgData, pongMsgData.length, destAddr, pingPort);

        // Send pong!
        udpSocket.send(pongPkt);
    }
}
