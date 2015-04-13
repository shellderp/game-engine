package shellderp.game.network;

import java.nio.ByteBuffer;

/**
 * Created by: Mike
 */
public interface ConnectionHandler {

    /**
     * Called when the connection is opened.
     *
     * @param connection The connection object bound to this ConnectionHandler. Further callbacks on this
     *                   ConnectionHandler instance will have the same Connection object.
     */
    void onOpen(Connection connection);

    // The following callbacks will only be called inside a call to step(), and thus need not worry about
    // thread safety.

    /**
     * Called when the connection is closed. No further callbacks will be made for this ConnectionHandler
     * after onClose is called.
     */
    void onClose();

    void onReliableRead(Connection connection, ByteBuffer payload);

    void onUnreliableRead(Connection connection, ByteBuffer payload);

}
