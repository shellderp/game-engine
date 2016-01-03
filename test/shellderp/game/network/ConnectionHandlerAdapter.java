package shellderp.game.network;

import java.nio.ByteBuffer;

/**
 * Provide defaults to make shorter tests.
 * <p>
 * Created by: Mike
 */
public class ConnectionHandlerAdapter implements ConnectionHandler {
  @Override
  public void onOpen(Connection connection) {

  }

  @Override
  public void onClose(Connection connection) {

  }

  @Override
  public void onReliableRead(Connection connection, ByteBuffer payload) {

  }

  @Override
  public void onUnreliableRead(Connection connection, ByteBuffer payload) {

  }
}
