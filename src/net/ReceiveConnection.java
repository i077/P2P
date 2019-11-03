package net;

import msg.Response;
import util.Log;
import util.Messages;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Represents a connection between two peers, used to receive a file that was previously requested.
 *
 * This class is used by the peer receiving the file, whereas the peer sending the file will use a TransferConnection.
 */
public class ReceiveConnection extends AbstractConnection {
    private Writer outputFileWriter;

    /**
     * Thread to request the file.
     */
    Thread requester;

    /**
     * Thread to receive the file.
     */
    Thread receiver;

    ReceiveConnection(final Response response) throws IOException {
        this.socket = new Socket(response.getOrigin(), response.getPort());
        File outputFile = new File("./obtained/" + response.getFilename());
        this.outputFileWriter = new BufferedWriter(new FileWriter(outputFile, false));

        this.requester = new Thread(new Runnable() {
            @Override
            public void run() {
                String pktMsg = "T:" + response.getFilename() + "\004";
                try {
                    socket.getOutputStream().write(pktMsg.getBytes());
                } catch (IOException e) {
                    if (ReceiveConnection.this.isAlive())
                        Log.e(Messages.ERR_TFER_REQSEND);
                }
            }
        });

        this.receiver = new Thread(new Runnable() {
            @Override
            public void run() {
                // Wait for request to finish
                try {
                    ReceiveConnection.this.requester.join();
                } catch (InterruptedException ignored) {}
                // If the requester was interrupted, that means this connection is in teardown, so just ignore the exception.

                // Receive file (in chunks)
                byte[] recvBuf = new byte[65536];
                int recvLen = -1;
                do {
                    try {
                        recvLen = socket.getInputStream().read(recvBuf);
                    } catch (IOException e) {
                        // Only log an error if the connection is still alive, otherwise stop
                        if (ReceiveConnection.this.isAlive())
                            Log.e(Messages.ERR_CONNREAD(socket.getInetAddress().getHostAddress()));
                        else return;
                    }
                    processPacket(recvBuf, recvLen);
                } while (ReceiveConnection.this.isAlive() && recvLen > 0);

            }
        });
    }

    /**
     * Process an incoming packet.
     * This should be a chunk of the requested file, which is written to a new file on the peer's filesystem.
     *
     * @param pktData Byte array of the incoming packet that was sent over the socket.
     * @param pktLen Length of the incoming packet
     */
    @Override
    protected void processPacket(byte[] pktData, int pktLen) {
        // Don't do anything if packet is empty.
        if (pktLen <= 0) return;

        String chunk = new String(pktData, 0, pktLen, StandardCharsets.UTF_8).trim();

        // Write to the file
        try {
            outputFileWriter.write(chunk);
        } catch (IOException e) {
            Log.e(Messages.ERR_FILEWRITE, e);
        }
    }

    @Override
    void teardown() {
        try {
            outputFileWriter.close();
        } catch (IOException e) {
            Log.e(Messages.ERR_FILEWRITE, e);
        }
        requester.interrupt();
        receiver.interrupt();
        super.teardown();
    }
}
