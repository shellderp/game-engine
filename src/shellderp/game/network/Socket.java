package shellderp.game.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Abstracts an unreliable stream, providing a direct send method and a non blocking receive method.
 * <p>
 * The underlying stream is implemented with a UDP socket, but this is subject to change.
 * <p>
 * Created by: Mike
 */
class Socket {

    private final DatagramChannel datagramChannel;

    /**
     * @param bindAddress The local address to listen on.
     * @throws IOException If binding to bindAddress fails.
     */
    public Socket(SocketAddress bindAddress) throws IOException {
        datagramChannel = DatagramChannel.open();
        datagramChannel.bind(bindAddress);
        datagramChannel.configureBlocking(false);
    }

    /**
     * We provide a default constructor for unit tests only, to avoid actual binding.
     */
    protected Socket() {
        datagramChannel = null;
    }

    public SelectionKey register(Selector selector, int ops) throws ClosedChannelException {
        return datagramChannel.register(selector, ops);
    }

    /**
     * Attempts to receive a packet on this socket without blocking.
     *
     * @param dst The buffer to read into.
     * @return The address we received from, or null if nothing was received.
     * @throws IOException
     */
    public SocketAddress tryReceive(ByteBuffer dst) throws IOException {
        return datagramChannel.receive(dst);
    }

    /**
     * Send a packet unreliably with no delay.
     * If successful, the buffer is guaranteed to be sent whole in one packet.
     *
     * This method is thread-safe since the DatagramChannel.send is thread-safe.
     *
     * @param packet   The packet to send.
     * @param endPoint The target to send to.
     * @return The number of bytes sent.
     * @throws IOException
     */
    public int sendDirect(Packet packet, SocketAddress endPoint) throws IOException {
        return datagramChannel.send(packet.toBuffer(), endPoint);
    }

    public void close() throws IOException {
        datagramChannel.close();
    }

}
