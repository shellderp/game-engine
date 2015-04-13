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
public class ReliableStreamTest {

    private InetSocketAddress serverAddress;
    private final int timeout = 2000;
    private static int newPort = 1200;

    private final static boolean useUdp = false;

    @Before
    public void setUp() throws Exception {
        if (!useUdp) {
            SocketProvider.setDefault(new TestSocketProvider());
        }
        serverAddress = new InetSocketAddress("localhost", newPort++);
    }

    @Test
    public void testInOrderReceival() throws Exception {
        final AtomicInteger numReads = new AtomicInteger();

        Server server = new Server(serverAddress, () -> new ConnectionHandlerAdapter() {
            @Override
            public void onReliableRead(Connection connection, ByteBuffer payload) {
                int index = numReads.getAndIncrement();
                assertEquals(index, payload.getInt());
            }

            @Override
            public void onUnreliableRead(Connection connection, ByteBuffer payload) {
                fail("should be no unreliable read");
            }
        });

        if (!useUdp) {
            SocketProvider.setDefault(
                    new TestSocketProvider(
                            new TestSocketProvider.ByIndex(new int[]{10, 15, 30, 40, 50, 1000})));
        }

        Connection conn = Connection.open(serverAddress, timeout, new ConnectionHandlerAdapter());

        final int numWrites = 1000;
        for (int i = 0; i < numWrites; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(i);
            buffer.flip();

            conn.getReliableStream().sendAsync(buffer);
        }

        int oldValue = 0;
        int count = 0;
        while (numReads.get() != numWrites && count < 10) {
            // The protocol inherently needs time to finish acking / resending, timeouts.
            // As long as it makes progress, let it do its thing.
            server.step(0);
            conn.step(0);
            Thread.sleep(50);

            if (numReads.get() == oldValue) {
                count++;
            } else {
                oldValue = numReads.get();
                count = 0;
            }
        }
        assertEquals(numWrites, numReads.get());

        server.stop();
    }

    @Test
    public void testDroppedAcks() throws Exception {
        final AtomicInteger numReads = new AtomicInteger();

        if (!useUdp) {
            SocketProvider.setDefault(
                    new TestSocketProvider(
                            new TestSocketProvider.ByIndex(new int[]{10, 15, 30, 40, 50, 1000})));
        }

        Server server = new Server(serverAddress, () -> new ConnectionHandlerAdapter() {
            @Override
            public void onReliableRead(Connection connection, ByteBuffer payload) {
                int index = numReads.getAndIncrement();
                assertEquals(index, payload.getInt());
            }

            @Override
            public void onUnreliableRead(Connection connection, ByteBuffer payload) {
                fail("should be no unreliable read");
            }
        });

        Connection conn = Connection.open(serverAddress, timeout, new ConnectionHandlerAdapter());

        final int numWrites = 1000;
        for (int i = 0; i < numWrites; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(i);
            buffer.flip();

            conn.getReliableStream().sendAsync(buffer);
        }

        int oldValue = 0;
        int count = 0;
        while (numReads.get() != numWrites && count < 10) {
            // The protocol inherently needs time to finish acking / resending, timeouts.
            // As long as it makes progress, let it do its thing.
            server.step(0);
            conn.step(0);
            Thread.sleep(50);

            if (numReads.get() == oldValue) {
                count++;
            } else {
                oldValue = numReads.get();
                count = 0;
            }
        }
        assertEquals(numWrites, numReads.get());

        server.stop();
    }

    @Test
    public void testTwoWay() throws Exception {
        final AtomicInteger numServerReads = new AtomicInteger();

        Server server = new Server(serverAddress, () -> new ConnectionHandlerAdapter() {
            @Override
            public void onReliableRead(Connection connection, ByteBuffer payload) {
                numServerReads.getAndIncrement();
                ByteBuffer reply = ByteBuffer.wrap(String.valueOf(payload.getInt()).getBytes());
                try {
                    connection.getReliableStream().sendAsync(reply);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        if (!useUdp) {
            SocketProvider.setDefault(
                    new TestSocketProvider(
                            new TestSocketProvider.ByIndex(new int[]{10, 15, 30, 40, 50, 1000})));
        }

        final AtomicInteger numClientReads = new AtomicInteger();

        Connection conn = Connection.open(serverAddress, timeout, new ConnectionHandlerAdapter() {
            @Override
            public void onReliableRead(Connection connection, ByteBuffer payload) {
                int index = numClientReads.getAndIncrement();
                byte[] p = new byte[payload.remaining()];
                payload.get(p);
                String s = new String(p);
                assertEquals(index, Integer.parseInt(s));
            }
        });

        final int numWrites = 1000;
        for (int i = 0; i < numWrites; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(i);
            buffer.flip();

            conn.getReliableStream().sendAsync(buffer);
        }

        int oldServerReads = 0, oldClientReads = 0;
        int count = 0;
        while ((numServerReads.get() != numWrites || numClientReads.get() != numWrites) && count < 10) {
            // The protocol inherently needs time to finish acking / resending, timeouts.
            // As long as it makes progress, let it do its thing.
            server.step(0);
            conn.step(0);
            Thread.sleep(50);

            if (numServerReads.get() == oldServerReads && numClientReads.get() == oldClientReads) {
                count++;
            } else {
                oldServerReads = numServerReads.get();
                oldClientReads = numClientReads.get();
                count = 0;
            }
        }
        assertEquals(numWrites, numServerReads.get());
        assertEquals(numWrites, numClientReads.get());

        server.stop();
    }

    @Test
    public void testTimeoutAfterNoAcks() throws Exception {
        if (useUdp) {
            // Test must use packet loss simulation to work.
            return;
        }

        // Make the server fail to send any acks (other than the connect ack).
        SocketProvider.setDefault(
                new TestSocketProvider(Packet::isConnectRequest));
        Server server = new Server(serverAddress, ConnectionHandlerAdapter::new);

        // conn succeeds in sending all messages.
        SocketProvider.setDefault(new TestSocketProvider(packet -> true));

        final AtomicInteger numClosed = new AtomicInteger();
        final AtomicInteger numReads = new AtomicInteger();
        Connection conn = Connection.open(serverAddress, timeout, new ConnectionHandlerAdapter() {
            @Override
            public void onReliableRead(Connection connection, ByteBuffer payload) {
                numReads.getAndIncrement();
            }

            @Override
            public void onClose() {
                numClosed.getAndIncrement();
            }
        });
        conn.getReliableStream().setSendTimeout(1000);

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(5);
        buffer.flip();
        conn.getReliableStream().sendAsync(buffer);

        for (int i = 0; i < 2200 / 50; i++) {
            server.step(0);
            conn.step(0);
            if (numClosed.get() == 1) {
                break;
            }
            Thread.sleep(50);
        }
        assertEquals(0, numReads.get());
        assertEquals(1, numClosed.get());

        server.stop();
    }
}
