package shellderp.game;

/**
 * Computes time differences not sensitive to the system (user set) clock.
 * Useful for timers or game loop.
 * <p>
 * Created by: Mike
 */
public final class Time {
  private final long time;

  private Time(long time) {
    this.time = time;
  }

  public long millisSince(Time since) {
    return (time - since.time) / 1_000_000;
  }

  public long nanosSince(Time since) {
    return time - since.time;
  }

  public static Time now() {
    // We prefer to use nanoTime since it is the CPU clock time.
    // In comparison, currentTimeMillis can change if the user changes the system time.
    return new Time(System.nanoTime());
  }
}
