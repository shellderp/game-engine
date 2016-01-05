package shellderp.game.network;

import java.nio.ByteBuffer;

/**
 * Immutable class representing a packet sent on the network. Provides toBuffer and fromBuffer methods to
 * send and receive on the network. This is transparent to a user of this library, since they just send and
 * receive payload ByteBuffers. Packets must be created using the Builder subclass.
 */
public class Packet {
  private static final int BITFLAG_RELIABLE = 1 << 0;
  private static final int BITFLAG_ACK = 1 << 1;
  private static final int BITFLAG_CONNECT_REQUEST = 1 << 2;
  private static final int BITFLAG_CLOSE = 1 << 3;

  static final int MAX_SEQUENCE = 65536;
  public static final int MAX_PACKET_SIZE = 8192;

  private final ByteBuffer payload;
  private final int sequence;

  private final boolean reliable;
  private final boolean connectRequest;
  private final boolean close;

  private final boolean ack;
  private final int ackSequence;

  private Packet(ByteBuffer payload, int sequence, boolean reliable, boolean connectRequest, boolean ack,
      int ackSequence, boolean close) {
    this.payload = payload;
    this.sequence = sequence;
    this.reliable = reliable;
    this.connectRequest = connectRequest;
    this.ack = ack;
    this.ackSequence = ackSequence;
    this.close = close;
  }

  public boolean hasPayload() {
    return payload != null;
  }

  public ByteBuffer getPayload() {
    return payload;
  }

  public boolean isReliable() {
    return reliable;
  }

  public boolean isConnectRequest() {
    return connectRequest;
  }

  public int getSequence() {
    return sequence;
  }

  public boolean hasAck() {
    return ack;
  }

  public int getAckSequence() {
    return ackSequence;
  }

  public boolean isClose() {
    return close;
  }

  public static class Builder {
    private ByteBuffer payload = null;
    private int sequence;
    private boolean connectRequest = false;
    private boolean reliable = false;
    private boolean close = false;
    private boolean hasAck = false;
    private int ackSequence;

    // Below are used to verify that the packet is constructed correctly.
    private boolean sequenceSet = false;

    /**
     * Sets the payload of the packet. A sequence must also be set.
     *
     * @param payload The buffer to include as the payload. This is what the receiving stream will see.
     *                Once passed in, this buffer is owned by the Packet and should not be modified.
     * @return This builder.
     */
    public Builder payload(ByteBuffer payload) {
      if (payload == null || payload.limit() == 0) {
        throw new IllegalArgumentException("payload cannot be null or empty");
      }
      this.payload = payload;
      return this;
    }

    public Builder ack(int ackSequence) {
      if (ackSequence < 0 || ackSequence >= MAX_SEQUENCE) {
        throw new IllegalArgumentException("sequence numbers must be from 0 to " + MAX_SEQUENCE);
      }

      this.hasAck = true;
      this.ackSequence = ackSequence;
      return this;
    }

    public Builder sequence(int sequence) {
      if (sequence < 0 || sequence >= MAX_SEQUENCE) {
        throw new IllegalArgumentException("sequence numbers must be from 0 to " + MAX_SEQUENCE);
      }

      this.sequence = sequence;
      sequenceSet = true;
      return this;
    }

    public Builder connectRequest() {
      this.connectRequest = true;
      return this;
    }

    /**
     * The packet is unreliable unless marked reliable.
     */
    public Builder reliable() {
      this.reliable = true;
      return this;
    }

    public Builder randomSequence() {
      // Generate a random sequence number for connecting, similar to TCP.
      this.sequence = (int) (Math.random() * (MAX_SEQUENCE - 1));
      return this;
    }

    public Builder close() {
      this.close = true;
      return this;
    }

    public Packet build() {
      if (payload != null && connectRequest) {
        throw new IllegalArgumentException("CONNECT REQUEST packet cannot have a payload");
      }
      if (payload != null && close) {
        throw new IllegalArgumentException("CLOSE packet cannot have a payload");
      }
      if (connectRequest && close) {
        throw new IllegalArgumentException("packet cannot be CLOSE and CONNECT REQUEST");
      }
      if (payload != null && !sequenceSet) {
        throw new IllegalArgumentException("packet with a payload must have sequence set");
      }
      return new Packet(payload, sequence, reliable, connectRequest, hasAck, ackSequence, close);
    }
  }

  /**
   * @return A new packet that is identical to this one but also has an ACK. Used by ACK-piggybacking.
   */
  public Packet withAck(int ackSequence) {
    return new Packet(payload, sequence, reliable, connectRequest, true, ackSequence, close);
  }

  /**
   * Creates a packet from the data in buffer which is assumed to be in read mode.
   * Buffer must be originally created with toBuffer() and sent on the network.
   * <p>
   * The payload buffer of the returned packet is created as a copy and thus not tied to the buffer
   * parameter.
   */
  public static Packet fromBuffer(ByteBuffer buffer) throws MalformedPacketException {
    if (buffer.limit() < 3) {
      throw new MalformedPacketException();
    }

    final byte flags = buffer.get();
    final int sequence = buffer.getShort() & 0xFFFF; // Make sure it isn't negative.

    final boolean reliable = (flags & BITFLAG_RELIABLE) != 0;
    final boolean connectRequest = (flags & BITFLAG_CONNECT_REQUEST) != 0;
    final boolean ack = (flags & BITFLAG_ACK) != 0;
    final boolean close = (flags & BITFLAG_CLOSE) != 0;

    final int ackSequence;
    if (ack) {
      if (buffer.remaining() < 2) {
        throw new MalformedPacketException();
      }
      ackSequence = buffer.getShort() & 0xFFFF;
    } else {
      ackSequence = 0;
    }

    ByteBuffer payload = null;
    if (buffer.hasRemaining()) {
      payload = ByteBuffer.allocate(buffer.remaining());
      payload.put(buffer);
      // Put the payload in read mode.
      payload.flip();
    }

    return new Packet(payload, sequence, reliable, connectRequest, ack, ackSequence, close);
  }

  public ByteBuffer toBuffer() throws MalformedPacketException {
    final int size = ((payload == null) ? 0 : payload.limit())
                     + 1 /* 1 byte flags */
                     + 2 /* 2 byte sequence */
                     + (ack ? 2 : 0); /* optional 2 byte ack sequence */

    if (size > MAX_PACKET_SIZE) {
      throw new MalformedPacketException(
          "payload size exceeds maximum packet size (" + size + " bytes)");
    }

    ByteBuffer buffer = ByteBuffer.allocate(size);

    final int flags = (reliable ? BITFLAG_RELIABLE : 0)
                      | (connectRequest ? BITFLAG_CONNECT_REQUEST : 0)
                      | (ack ? BITFLAG_ACK : 0)
                      | (close ? BITFLAG_CLOSE : 0);
    buffer.put((byte) flags);

    buffer.putShort((short) sequence);

    if (ack) {
      buffer.putShort((short) ackSequence);
    }

    if (payload != null) {
      buffer.put(payload);

      // Rewind the payload, in case we wish to call toBuffer again.
      payload.rewind();
    }

    // Done writing, return the buffer in read mode.
    buffer.flip();
    return buffer;
  }

  @Override
  public String toString() {
    return "Packet{" +
           "payload=[" + (payload == null ? "" : byteBufferToHex(payload.duplicate())) + "]" +
           ", sequence=" + sequence +
           ", reliable=" + reliable +
           ", connectRequest=" + connectRequest +
           ", close=" + close +
           ", ack=" + ack +
           ", ackSequence=" + ackSequence + '}';
  }

  private static String byteBufferToHex(ByteBuffer byteBuffer) {
    StringBuilder stringBuilder = new StringBuilder();
    while (byteBuffer.hasRemaining()) {
      final String hex = Integer.toHexString(byteBuffer.get() & 0xFF).toUpperCase();
      if (hex.length() == 1) {
        stringBuilder.append("0");
      }
      stringBuilder.append(hex);
      if (byteBuffer.hasRemaining()) {
        stringBuilder.append(" ");
      }
    }
    return stringBuilder.toString();
  }

  public static int nextSequence(int sequence) {
    return (sequence + 1) % MAX_SEQUENCE;
  }

  public static boolean newerThanExpected(int expected, int sequence) {
    // A packet is considered newer than the current expected value if it is in the range
    // [expected, expected + MAX/2] mod MAX.

    // Move the newSeq into the same domain as expected (modulo MAX).
    if (sequence < expected) {
      sequence += Packet.MAX_SEQUENCE;
    }

    return sequence >= expected && sequence < (expected + (Packet.MAX_SEQUENCE / 2));
  }
}
