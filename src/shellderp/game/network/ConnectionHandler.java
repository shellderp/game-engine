package shellderp.game.network;

import java.nio.ByteBuffer;

/**
 * All callbacks will only be called inside a call to Connection::step(), so thread safety is not a concern.
 * <p>
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

  /**
   * Called when the connection is closed. No further callbacks will be made for this ConnectionHandler
   * after onClose is called.
   * It is invalid to call step again on this Connection, doing so will result in an exception.
   *
   * @param connection
   */
  void onClose(Connection connection);

  void onReliableRead(Connection connection, ByteBuffer payload);

  void onUnreliableRead(Connection connection, ByteBuffer payload);

}
