package shellderp.game.network;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Packet-received handler when ReceiveThread has read a message packet.
 */
interface Receiver {
  void packetReceived(SocketAddress from, Packet packet) throws IOException;
}
