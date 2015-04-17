package shellderp.game.network;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Simple interface to provide a packet-received handler when ReceiveThread has read a message packet.
 * <p>
 * Created by: Mike
 */
interface Receiver {

    void packetReceived(SocketAddress from, Packet packet) throws IOException;

}
