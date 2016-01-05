package shellderp.game.network;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class PacketTest {

  @Test
  public void testFromBuffer() throws Exception {
    ByteBuffer payload = ByteBuffer.wrap("test".getBytes());
    Packet packet = new Packet.Builder().reliable().payload(payload.duplicate()).sequence(65535).build();

    ByteBuffer buffer = packet.toBuffer();

    Packet fromBuffer = Packet.fromBuffer(buffer);
    assertEquals(payload, fromBuffer.getPayload());
    assertEquals(true, fromBuffer.isReliable());
    assertEquals(65535, fromBuffer.getSequence());
  }

  @Test
  public void testNextSequence() throws Exception {
    assertEquals(1, Packet.nextSequence(0));
    assertEquals(0, Packet.nextSequence(65535));
  }

  @Test
  public void testNewerThanExpected() {
    assertTrue(Packet.newerThanExpected(0, 1));
    assertTrue(Packet.newerThanExpected(0, 100));
    assertTrue(Packet.newerThanExpected(1, 2));
    assertTrue(Packet.newerThanExpected(0, 0));
    assertFalse(Packet.newerThanExpected(1, 0));
    assertFalse(Packet.newerThanExpected(100, 0));
    assertTrue(Packet.newerThanExpected(31240, 31340));
    assertTrue(Packet.newerThanExpected(65535, 0));
    assertTrue(Packet.newerThanExpected(65535, 100));
    assertFalse(Packet.newerThanExpected(65535, 40000));
    assertFalse(Packet.newerThanExpected(0, 40000));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPayloadWithNoSequence() {
    new Packet.Builder().payload(ByteBuffer.allocate(1)).build();
  }

  @Test(expected = MalformedPacketException.class)
  public void testMalformedTooShort0() throws MalformedPacketException {
    Packet.fromBuffer(ByteBuffer.allocate(0));
  }

  @Test(expected = MalformedPacketException.class)
  public void testMalformedTooShort1() throws MalformedPacketException {
    Packet.fromBuffer(ByteBuffer.allocate(1));
  }

  @Test(expected = MalformedPacketException.class)
  public void testMalformedTooShort2() throws MalformedPacketException {
    Packet.fromBuffer(ByteBuffer.allocate(2));
  }

  @Test(expected = MalformedPacketException.class)
  public void testMalformedMissingAckSequence() throws MalformedPacketException {
    ByteBuffer buffer = new Packet.Builder().ack(5).build().toBuffer();
    final ByteBuffer duplicate = buffer.duplicate();
    duplicate.limit(3);
    Packet.fromBuffer(duplicate);
  }
}
