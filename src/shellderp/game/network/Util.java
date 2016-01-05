package shellderp.game.network;

import java.nio.ByteBuffer;

public class Util {
  public static String byteBufferToHex(ByteBuffer byteBuffer) {
    // Don't modify the original.
    byteBuffer = byteBuffer.duplicate();
    StringBuilder stringBuilder = new StringBuilder();
    while (byteBuffer.hasRemaining()) {
      final String hex = Integer.toHexString(byteBuffer.get() & 0xFF).toUpperCase();
      if (hex.length() == 1) {
        stringBuilder.append("0");
      }
      stringBuilder.append(hex);
      if (byteBuffer.hasRemaining()) {
        stringBuilder.append(" ");
      }
    }
    return stringBuilder.toString();
  }
}
