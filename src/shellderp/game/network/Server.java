package shellderp.game.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages connections from multiple clients and implements connection establishment protocol.
 *
 * Created by: Mike
 */
public class Server implements Receiver {

    private final Socket socket;

    private final ReceiveThread receiveThread;

    private final Supplier<ConnectionHandler> connectionHandlerProvider;

    private final Predicate<SocketAddress> allowConnection;

    private final HashMap<SocketAddress, Connection> clients = new HashMap<>();

    /**
     * We keep track of pending connections - entries in the set indicate that a certain (host, port)
     * has requested a connection, with the value being the client's sequence number from that request.
     * Upon receiving an ACK from one of these addresses, with the correct sequence number, we
     * accept the new connection.
     */
    private final LinkedHashMap<PendingConnection, Integer> pendingConnections = new LinkedHashMap<>();

    /**
     * Maximum time to keep track of a connection request. Default is abitrary.
     * A lower value will help prevent DDOS, but may prematurely ignore legitimate clients.
     */
    private long timeToKeepPendingConnsMs = 5000;

    public Server(SocketAddress bindAddress,
                  Supplier<ConnectionHandler> connectionHandlerProvider) throws IOException {
        this(bindAddress, connectionHandlerProvider, socketAddress -> true);
    }

    public Server(SocketAddress bindAddress,
                  Supplier<ConnectionHandler> connectionHandlerProvider,
                  Predicate<SocketAddress> allowConnection) throws IOException {
        this.connectionHandlerProvider = connectionHandlerProvider;
        this.allowConnection = allowConnection;

        socket = SocketProvider.getDefault().createSocket(bindAddress);

        receiveThread = new ReceiveThread(socket, this);
        new Thread(receiveThread).start();
    }

    public void setTimeToKeepPendingConns(long timeToKeepPendingConnsMs) {
        this.timeToKeepPendingConnsMs = timeToKeepPendingConnsMs;
    }

    public void stop() throws IOException {
        receiveThread.stop();

        for (Iterator<Connection> iterator = clients.values().iterator(); iterator.hasNext(); ) {
            Connection client = iterator.next();
            client.close();
            iterator.remove();
        }
    }

    public void step(long timeDeltaMs) {
        removeExpiredPendingConnections();

        for (Iterator<Connection> iterator = clients.values().iterator(); iterator.hasNext(); ) {
            Connection client = iterator.next();
            client.step(timeDeltaMs);
            if (!client.isOpen()) {
                iterator.remove();
            }
        }
    }

    private void removeExpiredPendingConnections() {
        Iterator<PendingConnection> iterator = pendingConnections.keySet().iterator();
        while (iterator.hasNext()) {
            PendingConnection pendingConnection = iterator.next();

            if (System.currentTimeMillis() - pendingConnection.getTimeAddedMs() > timeToKeepPendingConnsMs) {
                iterator.remove();
            } else {
                // Since the pending connections are in order of time added, we can stop now since the rest
                // have a later timeAddedMs than this one.
                break;
            }
        }
    }

    @Override
    public void packetReceived(SocketAddress socketAddress, Packet packet) {
        // If this address is already connected, we dispatch to the connection instance.
        if (clients.containsKey(socketAddress)) {
            Connection connection = clients.get(socketAddress);
            try {
                connection.packetReceived(socketAddress, packet);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "IOException in processing client", e);
            }
            return;
        }

        // Not connected and an ACK? This is likely a pending connection, unless either
        // this is a rogue client or the pending connection has expired.
        if (packet.hasAck()) {
            // Create a dummy to probe the set of pending connections.
            PendingConnection probe = new PendingConnection(socketAddress,
                                                            packet.getAckSequence());
            if (pendingConnections.containsKey(probe)) {
                int clientSequence = pendingConnections.remove(probe);
                Connection client = new Connection(socket, socketAddress, clientSequence,
                                                   packet.getAckSequence(), connectionHandlerProvider.get());
                clients.put(socketAddress, client);
            } else {
                logger.info("got ACK with no corresponding pending connection " + probe);
            }
        } else if (packet.isConnectRequest()) {
            if (allowConnection.test(socketAddress)) {
                Packet reply = new Packet.Builder().randomSequence()
                                                   .connectRequest()
                                                   .ack(Packet.nextSequence(packet.getSequence()))
                                                   .build();
                // Track that we received a connect request from this address,
                // so we know that on a follow up ACK the connection is established.
                // We track it with our outgoing sequence since that is the ACK we expect back.
                pendingConnections.put(new PendingConnection(socketAddress,
                                                             Packet.nextSequence(reply.getSequence())),
                                       Packet.nextSequence(packet.getSequence()));
                // Then reply with a connect request + ACK
                try {
                    socket.sendDirect(reply, socketAddress);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "IOException in sending connection handshake reply", e);
                }
            } else {
                logger.info("rejected connect request from " + socketAddress);
                // Ignore the connect request since we decided to reject it.
                // It may be better to reply with a reject packet, but that may
                // increase the damage of a DDOS.
                // If we want to reply with a reason for rejection, this should be done
                // at the layer above this one.
            }
        } else {
            // We must be receiving data from this address, but we have no connection.
            // This means they think we have a connection, which we can ignore since their end will
            // time out shortly.
            logger.fine("got an unhandleable packet from unconnected source: " + packet);
        }
    }

    private static final Logger logger = Logger.getLogger(Server.class.getName());
}
