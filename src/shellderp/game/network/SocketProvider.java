package shellderp.game.network;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Global provider of Socket objects, for testing purposes.
 * In unit tests - to control for packet loss and delays.
 * In-game testing - to simulate latency (e.g. for interpolation testing).
 * <p>
 * Created by: Mike
 */
public abstract class SocketProvider {

  private static SocketProvider defaultProvider = new SocketProvider() {
    @Override
    public Socket createSocket(SocketAddress bindAddress) throws IOException {
      return new Socket(bindAddress);
    }
  };

  public static SocketProvider getDefault() {
    return defaultProvider;
  }

  public static void setDefault(SocketProvider socketProvider) {
    defaultProvider = socketProvider;
  }

  public abstract Socket createSocket(SocketAddress bindAddress) throws IOException;

}
