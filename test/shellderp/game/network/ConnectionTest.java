package shellderp.game.network;

import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.*;

public class ConnectionTest {

  private InetSocketAddress serverAddress;
  private final int timeout = 1000;
  private static int newPort = 1000;

  @Before
  public void setUp() throws Exception {
    SocketProvider.setDefault(new TestSocketProvider());
    serverAddress = new InetSocketAddress("localhost", newPort++);
  }

  @Test
  public void testConnect() throws Exception {
    AtomicInteger openedServer = new AtomicInteger();
    AtomicInteger openedClient = new AtomicInteger();

    Server server = new Server(serverAddress, () -> new CountOpenConnectionHandler(openedServer));
    Connection conn = Connection.open(serverAddress, timeout,
        new CountOpenConnectionHandler(openedClient));
    Thread.sleep(50); // Give server a chance to read the ACK from conn.
    conn.step(0);
    server.step(0);
    assertEquals(1, openedServer.get());
    assertEquals(1, openedClient.get());

    server.stop();
  }

  @Test
  public void testConnectClose() throws Exception {
    AtomicInteger openedServer = new AtomicInteger();
    AtomicInteger openedClient = new AtomicInteger();
    AtomicInteger closedServer = new AtomicInteger();
    AtomicInteger closedClient = new AtomicInteger();

    Server server = new Server(serverAddress, () -> new CountOpenConnectionHandler(openedServer) {
      @Override
      public void onClose(Connection connection) {
        closedServer.getAndIncrement();
      }
    });
    Connection conn = Connection.open(serverAddress, timeout,
        new CountOpenConnectionHandler(openedClient) {
          @Override
          public void onClose(Connection connection) {
            closedClient.getAndIncrement();
          }
        });
    conn.step(0);
    server.step(0);
    assertTrue(conn.isOpen());
    conn.close();
    assertTrue(conn.isOpen());
    conn.step(0); // Connection only updates isOpen() status after step().
    assertFalse(conn.isOpen());
    Thread.sleep(50); // Give server a chance to read the ACK from conn.

    assertEquals(1, openedServer.get());
    assertEquals(1, openedClient.get());
    assertEquals(1, closedClient.get());

    server.step(0);
    assertEquals(1, closedServer.get());

    server.stop();
  }

  @Test
  public void testConnectWithDroppedFirstRequest() throws Exception {
    AtomicInteger openedServer = new AtomicInteger();
    AtomicInteger openedClient = new AtomicInteger();

    Server server = new Server(serverAddress, () -> new CountOpenConnectionHandler(openedServer));

    SocketProvider.setDefault(
        new TestSocketProvider(new TestSocketProvider.ByIndex(new int[]{0})));
    Connection conn = Connection.open(serverAddress, timeout,
        new CountOpenConnectionHandler(openedClient));
    Thread.sleep(50); // Give server a chance to read the ACK from conn.
    conn.step(0);
    server.step(0);
    assertEquals(1, openedServer.get());
    assertEquals(1, openedClient.get());

    server.stop();
  }

  @Test
  public void testConnectWithDroppedFirstRequestAndResponse() throws Exception {
    AtomicInteger openedServer = new AtomicInteger();
    AtomicInteger openedClient = new AtomicInteger();

    SocketProvider.setDefault(
        new TestSocketProvider(new TestSocketProvider.ByIndex(new int[]{0})));

    Server server = new Server(serverAddress, () -> new CountOpenConnectionHandler(openedServer));
    Connection conn = Connection.open(serverAddress, timeout,
        new CountOpenConnectionHandler(openedClient));
    Thread.sleep(50); // Give server a chance to read the ACK from conn.
    conn.step(0);
    server.step(0);
    assertEquals(1, openedServer.get());
    assertEquals(1, openedClient.get());

    server.stop();
  }

  @Test(expected = TimeoutException.class)
  public void testConnectWithAllDropped() throws Exception {
    AtomicInteger openedServer = new AtomicInteger();
    AtomicInteger openedClient = new AtomicInteger();

    Server server = new Server(serverAddress, () -> new CountOpenConnectionHandler(openedServer));

    SocketProvider.setDefault(new TestSocketProvider(packet -> false));
    Connection conn = Connection.open(serverAddress, timeout,
        new CountOpenConnectionHandler(openedClient));
  }

  class CountOpenConnectionHandler extends ConnectionHandlerAdapter {
    private final AtomicInteger count;

    public CountOpenConnectionHandler(AtomicInteger count) {
      this.count = count;
    }

    @Override
    public void onOpen(Connection connection) {
      count.incrementAndGet();
    }
  }
}
