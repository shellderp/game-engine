package shellderp.game.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Created by: Mike
 */
public class TestSocketProvider extends SocketProvider {

    private final Predicate<Packet> allowSend;
    private final Predicate<Packet> delay;

    public TestSocketProvider() {
        this(packet -> true);
    }

    public TestSocketProvider(Predicate<Packet> allowSend) {
        this(allowSend, packet -> true);
    }

    public TestSocketProvider(Predicate<Packet> allowSend, Predicate<Packet> delay) {
        this.allowSend = allowSend;
        this.delay = delay;
    }

    @Override
    public Socket createSocket(SocketAddress bindAddress) throws IOException {
        return new SocketStub(bindAddress) {
            @Override
            public synchronized int sendDirect(Packet packet, SocketAddress endPoint) throws IOException {
                if (!allowSend.test(packet)) {
                    System.out.println("dropping: " + packet);
                    // Return the real length, to pretend like we sent it but it got lost in the network.
                    // Otherwise, the sender knows there is an issue and can react differently.
                    return packet.toBuffer().limit();
                }
                if (!delay.test(packet)) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(50);
                            super.sendDirect(packet, endPoint);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                    return packet.toBuffer().limit();
                }
                return super.sendDirect(packet, endPoint);
            }
        };
    }

    public static class ByIndex implements Predicate<Packet> {

        private final int[] dropIndices;
        private volatile int count = 0;

        public ByIndex(int[] dropIndices) {
            Arrays.sort(dropIndices);
            this.dropIndices = dropIndices;
        }

        @Override
        public boolean test(Packet packet) {
            return Arrays.binarySearch(dropIndices, count++) < 0;
        }
    }

}
