package shellderp.game.network;

import shellderp.game.GameStep;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Implements a reliable stream using the Go-Back-N protocol. This stream guarantees messages are received
 * in the order they are sent, messages are not corrupted, and messages will arrive as long as the
 * connection is not closed.
 * <p>
 * Created by: Mike
 */
public class ReliableStream implements GameStep, SendableStream {

  /**
   * We need a default value for how long to wait before we assume a packet is lost and needs to be resent.
   * As the reliable stream runs, we adjust our timeout according to the observed RTT - this will be our
   * starting value. Since this is a game library, we use 500ms, since above this would be unacceptable
   * for gameplay anyway.
   * <p>
   * Note Connection.open() also uses this as a default timeout to resend the connection request.
   */
  public static final int DEFAULT_PACKET_LOST_TIMEOUT_MS = 500;

  private final Connection connection;
  private final ConnectionHandler handler;

  private int sequenceIn;
  private int sequenceOut;

  private final PiggybackAck piggybackAck;

  private final GoBackNWindow window;

  // TODO: think about adding an inQueue size limit so rogue clients can't result in out-of-memory crashes
  private final ConcurrentLinkedQueue<Packet> inQueue = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Packet> outQueue = new ConcurrentLinkedQueue<>();

  private long sendTimeoutMs = 5000;

  /**
   * Create a reliable stream from this connection. Note there can only ever be one per connection,
   * and conceptually a reliable stream cannot exist without a connection, so to enforce this we take
   * one as an argument instead of just taking the Socket and endpoint address.
   *
   * @param connection         The connection this stream is bound to, used to send packets.
   * @param handler            The handler for our connection.
   * @param initialSequenceIn  The next expected incoming sequence number, obtained from the connect
   *                           handshake.
   * @param initialSequenceOut The next expected outgoing sequence number, obtained from the connect
   *                           handshake.
   * @param piggybackAck       The ack piggyback handler owned by connection.
   */
  ReliableStream(Connection connection, ConnectionHandler handler, int initialSequenceIn,
      int initialSequenceOut, PiggybackAck piggybackAck) {
    this.connection = connection;
    this.handler = handler;
    this.sequenceIn = initialSequenceIn;
    this.sequenceOut = initialSequenceOut;
    this.piggybackAck = piggybackAck;

    window = new GoBackNWindow(initialSequenceOut, DEFAULT_PACKET_LOST_TIMEOUT_MS);
  }

  /**
   * @param sendTimeoutMs The max amount of time to wait for an ACK before deciding the connection is dead.
   */
  public void setSendTimeout(long sendTimeoutMs) {
    this.sendTimeoutMs = sendTimeoutMs;
  }

  /**
   * Attempt to send immediately, but with no guarantee that the endpoint has received the message after
   * this method returns. The packet will be delivered eventually as long as there is no connection error.
   * <p>
   * There are several scenarios in which the send will not succeed immediately:
   * 1. The kernel buffer is full, so we must attempt to send later.
   * 2. The packet is lost in the network, so we retry after not seeing an ACK for some time.
   * 3. The window is full, so we can't send another packet until we receive the ACK for the first packet
   * in the window.
   *
   * @param payload The contents of the message to send.
   */
  public synchronized void sendAsync(ByteBuffer payload) throws IOException {
    if (!connection.isOpen()) {
      throw new ClosedChannelException();
    }

    Packet packet = new Packet.Builder()
        .reliable()
        .payload(payload)
        .sequence(sequenceOut)
        .build();
    sequenceOut = Packet.nextSequence(sequenceOut);

    if (!outQueue.isEmpty()) {
      // If there is already something on the outqueue, we can't send immediately, else we violate
      // the in-order guarantee. If isEmpty becomes false right after this check, this is okay - we
      // end up sending on the next call to step().
      outQueue.add(packet);
      return;
    }

    synchronized (window) {
      if (!window.isFull()) {
        int bytesSent = connection.send(packet);

        if (bytesSent != 0) {
          // Tell the window that we've sent this packet, so that it ensures we get an ACK for it.
          window.packetSent(packet);
        } else {
          // The kernel outqueue was full, so queue this packet for write when possible.
          outQueue.add(packet);
        }
      } else {
        // The window is full, so queue this packet for write when possible.
        outQueue.add(packet);
      }
    }

    // TODO: have a log warning when send queue is large, however ConcurrentLinkedQueue.size() is O(n)
  }

  @Override
  public int maxSupportedPacketSize() {
    return Packet.MAX_PACKET_SIZE;
  }

  void packetReceived(Packet packet) throws IOException {
    if (packet.hasAck()) {
      synchronized (window) {
        window.ackReceived(packet.getAckSequence());
      }

      // This packet may be unreliable, since we allow piggy-backing ACKs on any send.
      if (!packet.isReliable()) {
        // This was only an ACK, the payload was meant for the unreliable stream, so stop here.
        return;
      }

      if (!packet.hasPayload()) {
        // Only an ACK, no payload, so we stop processing the packet.
        return;
      }
    }

    if (!packet.isReliable()) { // sanity check for packet dispatching logic
      throw new IllegalArgumentException("reliable stream got unreliable packet with no ACK");
    }

    // We received a payload. If this packet has the sequence we are expecting, we add it to the queue.
    // Otherwise, we discard it, since it is out of order and we will receive it later correctly.
    if (packet.getSequence() == sequenceIn) {
      inQueue.add(packet);
      sequenceIn = Packet.nextSequence(packet.getSequence());
    } else if (Packet.newerThanExpected(sequenceIn, packet.getSequence())) {
      // This packet is AHEAD of what we expect. It is very likely the packet we expect was lost,
      // so immediately send an ACK.

      // First we need to clear the piggyback ack value in case one is set.
      piggybackAck.getAndClearAck();

      Packet ack = new Packet.Builder().ack(sequenceIn).build();
      connection.send(ack);

      return;
    }

    // Update the piggyback handler with the latest sequence to ack.
    piggybackAck.setAckSequence(sequenceIn);
  }

  /**
   * During step, we:
   * 1. Check if any packets need to be re-sent because we didn't receive an ACK in the alloted time.
   * 2. Attempt to send any queued writes (see sendAsync for possible reasons).
   * 3. Callback for any queued reads.
   * <p>
   * This method should only be called by one thread at a time.
   *
   * @param timeDeltaMs The time in milliseconds since the last step.
   */
  @Override
  public void step(long timeDeltaMs) {
    // Dispatch read packets first, so the handler can start processing them immediately.
    while (true) {
      Packet packet = inQueue.poll();
      if (packet == null) {
        break;
      }
      handler.onReliableRead(connection, packet.getPayload());
    }

    synchronized (window) {
      try {
        if (window.hasPassedSinceLastUsefulAck(sendTimeoutMs)) {
          connection.close();
          return;
        }

        // Resend any packets that have timed out waiting for ACK.
        if (window.needToRetransmit()) {
          for (Packet packet : window.packetsToRetransmit()) {
            connection.send(packet);
          }
        }

        sendQueuedPackets();
      } catch (IOException e) {
        // An exception in sending at any point invalidates our connection, so close it.
        e.printStackTrace();
        connection.close();
      }
    }

    Optional<Integer> ack = piggybackAck.getAndClearAckIfTimeoutPassed();
    if (ack.isPresent()) {
      // We didn't send any packets to piggyback on within the timer, so we have to send a
      // payload-less ack.
      Packet ackPacket = new Packet.Builder().ack(ack.get()).build();
      try {
        connection.send(ackPacket);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Must be called in a synchronized (window) block.
   */
  private void sendQueuedPackets() throws IOException {
    // Attempt to send any queue'd writes if there is space in the send window.
    while (!window.isFull()) {
      // This is the only place we read from the outQueue, so it is fine to do peek() then
      // poll().
      Packet packet = outQueue.peek();
      if (packet == null) {
        break;
      }

      int bytesSent = connection.send(packet);
      if (bytesSent == 0) {
        // Failed to send, so don't remove the packet from the queue and try again next
        // step.
        break;
      }

      // Now actually remove the packet from the queue.
      if (packet != outQueue.poll()) {
        throw new IllegalStateException(
            "assumption violated: only step() should remove items from outQueue");
      }

      window.packetSent(packet);
    }
  }

}
