package shellderp.game.network;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by: Mike
 */
public interface SendableStream {
  /**
   * Send a payload on this stream without blocking. The connection must be open.
   *
   * @param payload The data to send on the stream. Once passed in, the buffer is owned by the stream and
   *                should not be modified. This is necessary for example if the reliable stream needs to
   *                resend the packet.
   * @throws IOException
   */
  void sendAsync(ByteBuffer payload) throws IOException;

  int maxSupportedPacketSize();
}
