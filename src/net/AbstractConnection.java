package net;

import util.Log;
import util.Messages;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

/**
 * Abstract representation for a connection between two peers.
 */
public abstract class AbstractConnection {
    protected Socket socket;

    /**
     * Returns whether this connection is still alive, i.e. whether the socket's connection is still active.
     *
     * @return true if the socket is connected, false otherwise
     */
    boolean isAlive() {
        return !socket.isClosed() && socket.isConnected();
    }

    /**
     * Teardown this connection.
     *
     * This should be called if the connection becomes stale (i.e. no heartbeat was received within the timeout interval),
     * or if the client is exiting.
     * Subclasses should cancel timers and interrupt threads when overriding this method.
     */
    void teardown() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(Messages.ERR_SOCKCLOSE, e);
        }
    }

    /**
     * Process an incoming packet on the socket.
     *
     * @param pktData Byte array of the incoming packet that was sent over the socket.
     * @param pktLen Length of the incoming packet
     */
    abstract protected void processPacket(byte[] pktData, int pktLen);

    @Override
    public int hashCode() {
        return Objects.hash(socket.getInetAddress().getHostAddress());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || this.getClass() != obj.getClass())
            return false;
        Connection that = (Connection) obj;
        // Treat connections with the same peer as the same
        return this.socket.getInetAddress().getHostAddress()
                .equals(that.socket.getInetAddress().getHostAddress());
    }
}
