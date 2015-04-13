package shellderp.game.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.UnresolvedAddressException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Provide a stub implementation of Socket, so that we can control message delivery and simulate
 * unreliable transfer with delays, dropped packets and reordering.
 * <p>
 * The underlying pipe ensures we don't lose any packets unless we choose to.
 * Created by: Mike
 */
public class SocketStub extends Socket {

    /**
     * We keep a map of all socket stubs so we can simulate sends by adding to eachothers inQueues.
     */
    private static final HashMap<SocketAddress, SocketStub> allSockets = new HashMap<>();
    private final SocketAddress bindAddress;

    private int totalSent;

    static class Message {
        final SocketAddress source;
        final ByteBuffer buffer;

        Message(SocketAddress source, ByteBuffer buffer) {
            this.source = source;
            this.buffer = buffer;
        }
    }

    private final ConcurrentLinkedQueue<Message> inQueue = new ConcurrentLinkedQueue<>();

    private final Pipe pipe;

    /**
     * @param bindAddress The local address to listen on.
     * @throws IOException If binding to bindAddress fails.
     */
    public SocketStub(SocketAddress bindAddress) throws IOException {
        super();

        while (allSockets.containsKey(bindAddress)) {
            InetSocketAddress addr = (InetSocketAddress) bindAddress;
            bindAddress = new InetSocketAddress(addr.getAddress(), addr.getPort() + 1);
        }
        this.bindAddress = bindAddress;
        allSockets.put(bindAddress, this);

        pipe = Pipe.open();
        pipe.source().configureBlocking(false);
        pipe.sink().configureBlocking(false);
    }

    @Override
    public SelectionKey register(Selector selector, int ops) throws ClosedChannelException {
        return pipe.source().register(selector, ops);
    }

    @Override
    public synchronized SocketAddress tryReceive(ByteBuffer dst) throws IOException {
        // Ignore what we read from pipe; we use it just for signalling.
        {
            ByteBuffer dummy = ByteBuffer.allocate(dst.capacity());
            pipe.source().read(dummy);
        }

        Message m = inQueue.poll();
        if (m == null) {
            return null;
        }

        m.buffer.rewind();
        dst.put(m.buffer);

        return m.source;
    }

    @Override
    public synchronized int sendDirect(Packet packet, SocketAddress endPoint) throws IOException {
        if (!allSockets.containsKey(endPoint)) {
            throw new UnresolvedAddressException();
        }

        totalSent++;

        int fromPort = ((InetSocketAddress) bindAddress).getPort();
        int toPort = ((InetSocketAddress) endPoint).getPort();

        System.out.printf("%d -> %d: %s%n", fromPort, toPort, packet);

        Message m = new Message(bindAddress, packet.toBuffer());
        SocketStub other = allSockets.get(endPoint);
        other.inQueue.add(m);
        other.pipe.sink().write(m.buffer.duplicate());

        return m.buffer.limit();
    }

    @Override
    public void close() throws IOException {
        pipe.source().close();
        pipe.sink().close();

        int fromPort = ((InetSocketAddress) bindAddress).getPort();
        System.out.printf("Total packets sent from %d: %d%n", fromPort, totalSent);
    }
}
