package shellderp.game.network;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Simple interface to provide a packet-received handler when Socket
 * has read a message packet. Implemented by Connection and Server.
 * <p>
 * Server receives a packet from Socket and dispatches it to Connection.
 * If using a Connection without a Server, Socket dispatches to Connection.
 * <p>
 * Created by: Mike
 */
interface Receiver {

    void packetReceived(SocketAddress from, Packet packet) throws IOException;

}
