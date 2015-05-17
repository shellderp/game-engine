package shellderp.game.network;

import shellderp.game.GameStep;
import shellderp.game.Timer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An established connection between two endpoints.
 * A unique connection is identified by a tuple of (source host, source port, endpoint host, endpoint port).
 * <p>
 * A connection can be created by opening it to a specified endpoint on the clientside ({@link #open})
 * or by a Server when a client connects.
 * <p>
 * It is safe to call send on the streams concurrently with a call to step().
 * step() needs to be called regularly (at least a few times per second) for the connection state to be
 * maintained. Not calling this often enough will result in poorer latency.
 * <p>
 * Timeout: a connection does not attempt to keep-alive internally. The only timeout scenario is if a reliable
 * packet is sent but not acked within the send timeout {@link ReliableStream#setSendTimeout(long)}. Thus a
 * keep-alive can be implemented by the user by sending keep-alive packets on the reliable stream.
 * <p>
 * Created by: Mike
 */
public class Connection implements GameStep {

    /**
     * The socket underlying this connection. In Connection we only use the send
     */
    private final Socket socket;
    private final SocketAddress endpoint;
    private ConnectionHandler handler;

    private final ReliableStream reliableStream;
    private final UnreliableStream unreliableStream;

    /**
     * The runnable that is handling our receiving in a separate thread. If this instance was created from
     * Server, this will be null.
     */
    private volatile ReceiveThread receiveThread;

    private enum State {
        OPEN,
        CLOSED_WAITING_FOR_STEP, // Closed, but waiting for step() so we can callback to handler.
        CLOSED, // Closed and callback was dispatched to handler.
    }

    // Can rely on this never changing once it is CLOSED, but must consider concurrency otherwise.
    private final AtomicReference<State> state = new AtomicReference<>(State.OPEN);

    /**
     * We callback onOpen on the first call to step(), so we need to know when the first call is made.
     * We could callback in the constructor, but this would require the handler to think about thread safety.
     */
    private boolean firstStep = true;

    /**
     * Keeps track of ack piggybacking. Unfortunately, we have to store this here and not in the
     * ReliableStream since we also want unreliable packets to carry acks.
     */
    private final PiggybackAck piggybackAck = new PiggybackAck();

    /**
     * Construct a connection representing socket connected to endpoint.
     * The socket must already have established a connection with the endpoint.
     *
     * @param socket             A socket that this connection will send and receive on.
     * @param endpoint           An endpoint which socket is already connected to.
     * @param initialSequenceIn  The initial incoming sequence number from the connection handshake.
     * @param initialSequenceOut The initial outgoing sequence number from the connection handshake.
     * @param handler            The handler that receives callbacks for this connection.
     */
    Connection(Socket socket, SocketAddress endpoint, int initialSequenceIn, int initialSequenceOut,
               ConnectionHandler handler) {
        this.socket = socket;
        this.endpoint = endpoint;
        this.handler = handler;

        this.reliableStream = new ReliableStream(this, handler, initialSequenceIn, initialSequenceOut,
                                                 piggybackAck);
        this.unreliableStream = new UnreliableStream(this, handler, initialSequenceIn, initialSequenceOut);
    }

    void setReceiveThread(ReceiveThread receiveThread) {
        this.receiveThread = receiveThread;
    }

    public SocketAddress getEndPoint() {
        return endpoint;
    }

    public void setHandler(ConnectionHandler handler) {
        this.handler = handler;
    }

    /**
     * @return true if the connection is open. This is only updated when we receive a close message from the
     * endpoint or have a reliable stream timeout, or close() is called.
     */
    public boolean isOpen() {
        return state.get() == State.OPEN;
    }

    @Override
    public String toString() {
        return "Connection{" +
               "endpoint=" + endpoint +
               ", state=" + state +
               '}';
    }

    public ReliableStream getReliableStream() {
        return reliableStream;
    }

    public UnreliableStream getUnreliableStream() {
        return unreliableStream;
    }

    public void close() {
        // We send a close message and hope the endpoint receives it. We consider this connection closed
        // immediately. If the endpoint doesn't receive it, it will time out eventually.
        try {
            send(new Packet.Builder().close().build());
        } catch (IOException e) {
            // We swallow the exception since we don't really care if the send failed, we still consider
            // this closed successfully.
            logger.log(Level.INFO, "exception when trying to send close packet", e);
        }

        markClosed();
    }

    /**
     * Internally mark the connection as closed. This will happen in one of two ways:
     * 1. close is called on the connection.
     * 2. we receive a close packet from the endpoint
     * <p>
     * The close mechanism does not bother acking packets, and instead relies on the reliable stream
     * timeout to determine (lazily) when a connection has closed.
     */
    private void markClosed() {
        if (!state.compareAndSet(State.OPEN, State.CLOSED_WAITING_FOR_STEP)) {
            // Connection is already closed, do nothing.
            logger.info("connection already closed: " + toString());
            return;
        }

        if (receiveThread != null) {
            // We can stop receiving packets now if this Connection was created with open() and not by a
            // Server. ReceiveThread will also close the socket for us when it terminates.
            receiveThread.stop();
        }
    }

    // Only used by the streams to send data, so access is package-level.
    int send(Packet packet) throws IOException {
        if (!packet.hasAck()) {
            // Try to piggyback an ack if we have one waiting. At the same time we clear it since we are
            // sending it now.
            Optional<Integer> ack = piggybackAck.getAndClearAck();

            if (ack.isPresent()) {
                packet = packet.withAck(ack.get());
            }
        }
        return socket.sendDirect(packet, getEndPoint());
    }

    /**
     * Called by ReceiveThread when a packet is received.
     * We dispatch it to be handled by the correct stream.
     *
     * @param from   The address from which we received the packet. If this is not the address we are
     *               connected to, the packet will be ignored.
     * @param packet The packet received from our Socket.
     */
    void packetReceived(SocketAddress from, Packet packet) {
        if (state.get() != State.OPEN) {
            return;
        }
        // Technically at any point after this, the state could become CLOSED, but there's no harm in
        // processing one extra packet.

        if (!from.equals(getEndPoint())) {
            return; // Ignore packets from other sources, since anyone can send to our socket.
        }

        if (packet.isConnectRequest()) {
            return; // Ignore connection requests, this could be a delayed packet or a rogue sender.
        }

        if (packet.isClose()) {
            markClosed();
            return;
        }

        // Note the packet may be dispatched to both reliable and unreliable streams if it contains an
        // unreliable payload and an ACK is piggybacked on it.

        if (packet.isReliable() || packet.hasAck()) {
            try {
                getReliableStream().packetReceived(packet);
            } catch (IOException e) {
                e.printStackTrace();
                close();
            }
        }
        if (!packet.isReliable() && packet.hasPayload()) {
            getUnreliableStream().packetReceived(packet);
        }
    }

    /**
     * A Connection step does the following:
     * 1. Check for timeout (connection dropped).
     * 2. Call step on the reliable stream.
     * <p>
     * It is assumed this method is called by at most one thread.
     *
     * @param timeDeltaMs The time in milliseconds since the last step.
     */
    @Override
    public void step(long timeDeltaMs) {
        if (firstStep) {
            firstStep = false;
            handler.onOpen(this);
        }

        if (state.get() == State.CLOSED) {
            throw new IllegalStateException("connection is closed");
        } else if (state.get() == State.CLOSED_WAITING_FOR_STEP) {
            state.set(State.CLOSED);
            handler.onClose();
            return;
        }

        getUnreliableStream().step(timeDeltaMs);
        getReliableStream().step(timeDeltaMs);
    }

    /**
     * Attempt to connect to the endpoint. Returns a connection if successful,
     * or throws an exception if failed. Generally called by a client to connect to a server.
     * <p>
     * This blocks until connected or the timeout is exceeded.
     *
     * @param target    The endpoint to connect to.
     * @param timeoutMs The limit on amount of time to try to connect before giving up.
     *                  This is exposed with the intention of giving better control of the UX.
     * @param handler   The callback for events on this connection.
     * @return A new connection to endpoint, if one can be established.
     * @throws IOException      If the connection fails for some underlying IO error.
     * @throws TimeoutException If the connection attempt times out.
     */
    public static Connection open(SocketAddress target, long timeoutMs, ConnectionHandler handler)
            throws IOException, TimeoutException, InterruptedException {
        // Create a new socket, 0 selects any open port.
        final Socket socket = SocketProvider.getDefault().createSocket(new InetSocketAddress(0));

        // Track when we started, so we can obey the timeout.
        final Timer startTimer = new Timer();
        startTimer.restart();
        // Track when we sent the last request, so we can retry after timeout, separate from the overall
        // timeout.
        final Timer lastRequestTimer = new Timer();
        lastRequestTimer.restart();

        // These will be the initial sequence numbers for creating the (un)reliable streams.
        // They come randomly from Packet.build.
        int sequenceIn, sequenceOut;

        {
            // Send a connect request. We use the sequence number generated as the initial outgoing
            // sequence number for both streams.
            Packet request = new Packet.Builder().randomSequence().connectRequest().build();
            sequenceOut = Packet.nextSequence(request.getSequence());
            socket.sendDirect(request, target);
        }

        // Wait for reply, or timeout.
        ByteBuffer buffer = ByteBuffer.allocate(Packet.MAX_PACKET_SIZE);
        while (true) {
            buffer.clear();
            SocketAddress socketAddress = socket.tryReceive(buffer);
            buffer.flip();

            // If we get a message from a different address than endpoint, ignore it.
            if (socketAddress != null && socketAddress.equals(target)) {
                Packet packet = Packet.fromBuffer(buffer);
                // We expect a packet that is a connect request and acknowledges our request. In any other
                // case we just keep trying.
                if (packet.isConnectRequest() && packet.hasAck() && packet.getAckSequence() == sequenceOut) {
                    sequenceIn = Packet.nextSequence(packet.getSequence());
                    break;
                }
            }

            // We didn't get a packet, or didn't get one that establishes our connection.
            if (startTimer.hasPassed(timeoutMs)) {
                throw new TimeoutException("connect timed out");
            }

            if (lastRequestTimer.hasPassed(ReliableStream.DEFAULT_PACKET_LOST_TIMEOUT_MS)) {
                // Assume the first packet was lost, or the server's reply was lost.
                // We send a new request and invalidate the old one by storing the new sequence.
                Packet request = new Packet.Builder().sequence(Packet.nextSequence(sequenceOut))
                                                     .connectRequest()
                                                     .build();
                sequenceOut = Packet.nextSequence(request.getSequence());
                socket.sendDirect(request, target);

                lastRequestTimer.restart();
            }

            // Sleep a little before trying to read again, otherwise we burn the CPU.
            // But not too long, or we'll quickly reach the timeout.
            Thread.sleep(10);
        }

        // Send an ACK to finish the connection.
        socket.sendDirect(new Packet.Builder().ack(sequenceIn).build(), target);

        Connection connection = new Connection(socket, target, sequenceIn, sequenceOut, handler);
        ReceiveThread receiveThread = new ReceiveThread(socket, connection::packetReceived);
        connection.setReceiveThread(receiveThread);

        // Kick off a thread to receive on this socket until it is closed.
        new Thread(receiveThread).start();

        return connection;
    }

    private static final Logger logger = Logger.getLogger(Connection.class.getName());
}
