package shellderp.game.network;

import java.io.IOException;

public class MalformedPacketException extends IOException {
  public MalformedPacketException() {
    super();
  }

  public MalformedPacketException(String message) {
    super(message);
  }
}
