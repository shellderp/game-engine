package shellderp.game.ui;

/**
 * Created by: Mike
 */
public class Coords {

  public static Coord fixed(int length) {
    return new FixedCoord(length);
  }

  public static Coord percent(double percent) {
    return percent(percent, 0);
  }

  public static Coord percent(double percent, int minLength) {
    return new RelativeCoord(percent, minLength);
  }

  public static final Coord ZERO = fixed(0);

  public static Coord centerInParent() {
    return CENTER_IN_PARENT;
  }

  private static final Coord CENTER_IN_PARENT =
      (containerLength, myLength) -> containerLength / 2 - myLength / 2;

  private static class FixedCoord implements Coord {
    private final int length;

    public FixedCoord(int length) {
      this.length = length;
    }

    @Override
    public int computeAbsoluteLength(int containerLength, int myLength) {
      return length;
    }
  }

  private static class RelativeCoord implements Coord {
    private final double percent;
    private final int minLength;

    public RelativeCoord(double percent, int minLength) {
      if (percent < 0 || percent > 1) {
        throw new IllegalArgumentException("percent must be between 0 and 1");
      }
      this.percent = percent;
      this.minLength = minLength;
    }

    @Override
    public int computeAbsoluteLength(int containerLength, int myLength) {
      final int length = (int) (percent * containerLength);
      return Math.max(length, minLength);
    }
  }
}
