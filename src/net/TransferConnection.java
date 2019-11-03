package net;

import util.Log;
import util.Messages;
import util.PeerConfig;
import util.Values;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Represents a connection between two peers, used to transfer a file that was previously requested.
 *
 * This class is used by the peer sending the file, whereas the peer receiving the file will use a ReceiveConnection.
 */
public class TransferConnection extends AbstractConnection {
    Thread listener;
    private Timer reader;

    TransferConnection(final Socket socket) {
        this.socket = socket;

        reader = new Timer();

        this.listener = new Thread(new Runnable() {
            @Override
            public void run() {
                reader.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        byte[] recvBuf = new byte[2048];
                        int recvLen = -1;
                        do {
                            try {
                                recvLen = socket.getInputStream().read(recvBuf);
                            } catch (IOException e) {
                                // Only log an error if the connection is still alive, otherwise stop
                                if (TransferConnection.this.isAlive())
                                    Log.e(Messages.ERR_CONNREAD(socket.getInetAddress().getHostAddress()), e);
                                else return;
                            }
                        } while (TransferConnection.this.isAlive() && recvLen < 0);

                        TransferConnection.this.processPacket(recvBuf, recvLen);
                    }
                }, 0, Values.READER_INTERVAL);
            }
        });
        listener.setDaemon(true);
    }

    /**
     * Process an incoming packet.
     * This packet must be a request to transfer a file.
     * If it is, transfer the file then teardown this connection, since we don't need it anymore.
     *
     * @param pktData Byte array of the incoming packet that was sent over the socket.
     * @param pktLen Length of the incoming packet
     */
    protected void processPacket(byte[] pktData, int pktLen) {
        // Don't do anything if packet is empty.
        if (pktLen <= 0) return;

        String message = new String(pktData, 0, pktLen).trim(); // Exclude end-of-transmission character

        // We're only looking for one type of message here
        if (message.charAt(0) == 'T') {
            String filename = message.substring(2); // Exclude "T:"
            Log.i(Messages.TFER_REQRECV(filename, socket.getInetAddress().getHostAddress()));

            sendFile(filename);

            // This connection isn't needed anymore, we can close it.
            teardown();
        }
    }

    /**
     * Send a file, specified by name, over the socket.
     *
     * @param filename The name of the file to send.
     */
    private void sendFile(String filename) {
        File requestedFile = null;
        for (File f : PeerConfig.get().sharedFileList) {
            if (f.getName().equals(filename)) {
                requestedFile = f;
            }
        }
        if (requestedFile == null)
            return;

        // At this point, we have found the file.
        // We need to break it up in chunks to send via packets, so we use a FileReader to read the file chunks at a time.
        Reader fileReader = null;
        try {
            fileReader = new BufferedReader(new FileReader(requestedFile));
        } catch (FileNotFoundException ignored) {} // We can safely assume that the peer has the file, since PeerConfig checks this.
        assert fileReader != null;

        // Send the file!
        char[] fileChunk = new char[2048];
        try {
            while (fileReader.read(fileChunk) != -1) {
                byte[] pktData = new String(fileChunk).getBytes(StandardCharsets.UTF_8);
                socket.getOutputStream().write(pktData);
            }
        } catch (IOException e) {
            if (this.isAlive())
                Log.e(Messages.ERR_TFER_SEND, e);
        }

        try {
            fileReader.close();
        } catch (IOException e) {
            Log.e(Messages.ERR_FILEREAD, e);
        }
        Log.i(Messages.TFER_FINISHED(filename, socket.getInetAddress().getHostAddress()));
    }

    @Override
    void teardown() {
        listener.interrupt();
        reader.cancel();
        super.teardown();
    }
}
