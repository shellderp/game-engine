package shellderp.game.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Reads whenever possible from a Socket and dispatches to the given callback.
 * <p>
 * Why don't we read in Connection::step() or Server::step()? The issue is that if we step every 100ms,
 * we would only reply with ACKs in 100ms intervals, leading to high latency and restricting the usable
 * range of tick rates.
 * <p>
 * Created by: Mike
 */
class ReceiveThread implements Runnable {

  private final Socket socket;
  private final Receiver receiver;

  private final Selector selector;

  private volatile boolean running = true;

  public ReceiveThread(Socket socket, Receiver receiver) throws IOException {
    this.socket = socket;
    this.receiver = receiver;

    selector = Selector.open();

    socket.register(selector, SelectionKey.OP_READ);
  }

  @Override
  public void run() {
    ByteBuffer buffer = ByteBuffer.allocate(Packet.MAX_PACKET_SIZE);

    while (true) {
      try {
        selector.select();

        if (!running) {
          break;
        }

        // We only have one socket to worry about, so no need to iterate over selectedKeys.
        selector.selectedKeys().clear();

        readSocketUntilDone(buffer);
      } catch (IOException e) {
        running = false;
        e.printStackTrace();
      }
    }

    try {
      selector.close();
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void readSocketUntilDone(ByteBuffer buffer) throws IOException {
    while (true) {
      SocketAddress socketAddress = socket.tryReceive(buffer);
      if (socketAddress == null) {
        break;
      }

      buffer.flip();

      Packet packet = Packet.fromBuffer(buffer);
      receiver.packetReceived(socketAddress, packet);
      buffer.clear(); // Reset for the next read.
    }
  }

  public void stop() {
    if (running) {
      running = false;
      selector.wakeup();
    }
  }
}
