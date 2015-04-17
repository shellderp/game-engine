package shellderp.game.network;

import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by: Mike
 */
public class UnreliableStreamTest {

    private InetSocketAddress serverAddress;
    private final int timeout = 2000;
    private static int newPort = 1100;

    @Before
    public void setUp() throws Exception {
        SocketProvider.setDefault(new TestSocketProvider());
        serverAddress = new InetSocketAddress("localhost", newPort++);
    }

    @Test
    public void testPayloadMatches() throws Exception {
        final AtomicInteger numReads = new AtomicInteger();
        final ByteBuffer buffer = ByteBuffer.wrap("test message".getBytes());

        Server server = new Server(serverAddress, () -> new ConnectionHandlerAdapter() {
            @Override
            public void onReliableRead(Connection connection, ByteBuffer payload) {
                fail("should be no reliable read");
            }

            @Override
            public void onUnreliableRead(Connection connection, ByteBuffer payload) {
                numReads.getAndIncrement();
                assertEquals(payload, buffer);
            }
        });
        Connection conn = Connection.open(serverAddress, timeout, new ConnectionHandlerAdapter());

        conn.getUnreliableStream().sendAsync(buffer.duplicate());

        Thread.sleep(50); // Give server a chance to read the ACK from conn.
        server.step(0);
        assertEquals(1, numReads.get());

        server.stop();
    }

    @Test
    public void testNotDelivered() throws Exception {
        final ByteBuffer buffer = ByteBuffer.wrap("test message".getBytes());

        Server server = new Server(serverAddress, () -> new ConnectionHandlerAdapter() {
            @Override
            public void onReliableRead(Connection connection, ByteBuffer payload) {
                fail("should be no reliable read");
            }

            @Override
            public void onUnreliableRead(Connection connection, ByteBuffer payload) {
                fail("should be no unreliable read");
            }
        });

        // Let the connection packets through, but block the message packet.
        SocketProvider.setDefault(
                new TestSocketProvider(packet -> packet.hasAck() || packet.isConnectRequest()));
        Connection conn = Connection.open(serverAddress, timeout, new ConnectionHandlerAdapter());

        conn.getUnreliableStream().sendAsync(buffer.duplicate());

        Thread.sleep(50); // Give server a chance to read the ACK from conn.
        server.step(0);

        server.stop();
    }

    @Test
    public void testIgnoreDelayedPacket() throws Exception {
        final AtomicInteger numReads = new AtomicInteger();
        final ByteBuffer buffer = ByteBuffer.wrap("test message".getBytes());

        Server server = new Server(serverAddress, () -> new ConnectionHandlerAdapter() {
            @Override
            public void onReliableRead(Connection connection, ByteBuffer payload) {
                fail("should be no reliable read");
            }

            @Override
            public void onUnreliableRead(Connection connection, ByteBuffer payload) {
                numReads.getAndIncrement();
            }
        });

        SocketProvider.setDefault(
                new TestSocketProvider(p -> true, new TestSocketProvider.ByIndex(new int[]{2})));
        Connection conn = Connection.open(serverAddress, timeout, new ConnectionHandlerAdapter());

        conn.getUnreliableStream().sendAsync(buffer.duplicate());
        conn.getUnreliableStream().sendAsync(buffer.duplicate());

        Thread.sleep(150); // Give server a chance to read
        server.step(0);
        assertEquals(1, numReads.get());

        server.stop();
    }

    @Test
    public void testSeqNumOverflow() throws Exception {
        int seq = Packet.MAX_SEQUENCE - 1;
        UnreliableStream unreliableStream = new UnreliableStream(null, new ConnectionHandlerAdapter(), seq,
                                                                 0);
        Packet p1 = new Packet.Builder().sequence(seq).build();
        seq = Packet.nextSequence(seq);
        Packet p2 = new Packet.Builder().sequence(seq).build();

        unreliableStream.packetReceived(p1);
        unreliableStream.packetReceived(p2);

        assertEquals(2, unreliableStream.inQueue.size());
    }

    @Test
    public void testSeqNumOverflowAfterMissedPackets() throws Exception {
        int seq = Packet.MAX_SEQUENCE - 50;
        UnreliableStream unreliableStream = new UnreliableStream(null, new ConnectionHandlerAdapter(), seq,
                                                                 0);
        Packet p1 = new Packet.Builder().sequence(seq).build();

        // In this case we have a 100 gap, as if we lost 100 packets.
        seq = Packet.nextSequence(seq + 100);
        Packet p2 = new Packet.Builder().sequence(seq).build();

        unreliableStream.packetReceived(p1);
        unreliableStream.packetReceived(p2);

        assertEquals(2, unreliableStream.inQueue.size());
    }

    @Test
    public void testSeqNumNotOverflow() throws Exception {
        int seq = Packet.MAX_SEQUENCE - 1;
        UnreliableStream unreliableStream = new UnreliableStream(null, new ConnectionHandlerAdapter(), seq,
                                                                 0);
        Packet p1 = new Packet.Builder().sequence(seq).build();

        // In this case we have a 100 gap, as if a packet was delayed and arrived after 100 others.
        // This SHOULD be ignored
        seq = seq - 100;
        Packet p2 = new Packet.Builder().sequence(seq).build();

        unreliableStream.packetReceived(p1);
        unreliableStream.packetReceived(p2);

        assertEquals(1, unreliableStream.inQueue.size());
    }

    @Test
    public void testSeqNumOppositeOverflow() throws Exception {
        int seq = 100;
        UnreliableStream unreliableStream = new UnreliableStream(null, new ConnectionHandlerAdapter(), seq,
                                                                 0);
        Packet p1 = new Packet.Builder().sequence(seq).build();

        // This SHOULD be ignored
        seq = 40000;
        Packet p2 = new Packet.Builder().sequence(seq).build();

        unreliableStream.packetReceived(p1);
        unreliableStream.packetReceived(p2);

        assertEquals(1, unreliableStream.inQueue.size());
    }

//    @Test
//    public void testSpam() throws Exception {
//        SocketProvider.setDefault(new SocketProvider() {
//            @Override
//            public Socket createSocket(SocketAddress bindAddress) throws IOException {
//                return new Socket(bindAddress);
//            }
//        });
//
//        final AtomicInteger numReads = new AtomicInteger();
//        final ByteBuffer buffer = ByteBuffer.allocate(8000);
//
//        Server server = new Server(serverAddress, () -> new ConnectionHandlerAdapter() {
//            @Override
//            public void onUnreliableRead(Connection connection, ByteBuffer payload) {
//                numReads.getAndIncrement();
//            }
//        });
//
//        Connection conn = Connection.open(serverAddress, timeout, new ConnectionHandlerAdapter());
//
//        for (int i = 0; i < 10000; i++) {
//            conn.getUnreliableStream().sendAsync(buffer.duplicate());
//            Thread.sleep(1);
//        }
//
//        Thread.sleep(1000); // Give server a chance to read
//        server.step(0);
//
//        server.stop();
//        System.out.println(numReads);
//    }

}
