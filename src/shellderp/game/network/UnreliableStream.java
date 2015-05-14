package shellderp.game.network;

import shellderp.game.GameStep;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * UnreliableStream allows sending and receiving packets that provide no guarantee on delivery, but can
 * provide lower latency. Old packets are ignored after newer ones have been received. The packets received
 * are buffered until a call to step(), in which the ConnectionHandler.onUnreliableRead callback is made.
 * <p>
 * Created by: Mike
 */
public class UnreliableStream implements GameStep {

    private final Connection connection;
    private final ConnectionHandler handler;

    /**
     * The next incoming sequence number we expect, i.e. last packet's sequence + 1 modulo MAX.
     */
    private int sequenceIn;
    private int sequenceOut;

    // TODO: think about adding an inQueue size limit so rogue clients can't result in out-of-memory crashes
    final ConcurrentLinkedQueue<Packet> inQueue = new ConcurrentLinkedQueue<>();

    /**
     * Create an unreliable stream from this connection. Note there can only ever be one per connection,
     * and conceptually an unreliable stream cannot exist without a connection, so to enforce this we take
     * one as an argument instead of just taking the Socket and endpoint address.
     *
     * @param connection         The connection this stream is bound to, used to send packets.
     * @param initialSequenceIn  The next expected incoming sequence number, obtained from the connect
     *                           handshake.
     * @param initialSequenceOut The next expected outgoing sequence number, obtained from the connect
     *                           handshake.
     * @param handler            The handler for our connection.
     */
    UnreliableStream(Connection connection, ConnectionHandler handler, int initialSequenceIn,
                     int initialSequenceOut) {
        this.connection = connection;
        this.handler = handler;
        this.sequenceIn = initialSequenceIn;
        this.sequenceOut = initialSequenceOut;
    }

    /**
     * Send a packet asynchronously. There is no guarantee this packet will arrive at the endpoint.
     * This method is thread-safe.
     *
     * @param payload The contents of the message to send.
     * @throws IOException
     */
    public synchronized void sendAsync(ByteBuffer payload) throws IOException {
        if (!connection.isOpen()) {
            throw new IllegalStateException("cannot send on closed Connection");
        }

        Packet packet = new Packet.Builder().payload(payload).sequence(sequenceOut).build();

        // Note we ignore the return value of send, since we are ok with failing to send the packet.
        connection.send(packet);

        sequenceOut = Packet.nextSequence(sequenceOut);
    }

    /**
     * Packet was just received on the channel, if valid then dispatch it.
     * An unreliable packet is valid unless the sequence is out of order,
     * i.e. we have already seen a newer packet.
     *
     * @param packet The packet received on the stream, must not be marked reliable.
     */
    void packetReceived(Packet packet) {
        if (packet.isReliable()) {  // sanity check for packet dispatching logic
            throw new IllegalArgumentException("unreliable stream got reliable packet");
        }

        // In the unreliable stream, we only want to receive the latest data. As long as this packet is newer
        // than the last packet we've seen, we use it, even if we missed some packets on the way.
        if (Packet.newerThanExpected(sequenceIn, packet.getSequence())) {
            // TODO: count this stat - how many packets we skip on avg
//            if (newSeq > sequenceIn) {
//                logger.info(
//                        String.format("skipped, %d -> %d -- %d", sequenceIn, newSeq, newSeq - sequenceIn));
//            }

            inQueue.add(packet);
            sequenceIn = Packet.nextSequence(packet.getSequence());
        } else {
            logger.info(String.format("received old packet; expected: %d, got: %d", sequenceIn,
                                      packet.getSequence()));
        }
    }

    /**
     * At each step we simply callback for any queued reads.
     *
     * @param timeDeltaMs The time in milliseconds since the last step.
     */
    @Override
    public void step(long timeDeltaMs) {
        while (true) {
            Packet packet = inQueue.poll();
            if (packet == null) {
                break;
            }
            handler.onUnreliableRead(connection, packet.getPayload());
        }
    }

    private static final Logger logger = Logger.getLogger(UnreliableStream.class.getName());

}
