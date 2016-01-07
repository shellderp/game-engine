package shellderp.game;

public interface GameStep {
  /**
   * Take a single step of the game. This is called every iteration of the game eventloop.
   *
   * @param timeDeltaMs The time in milliseconds since the last step.
   */
  void step(long timeDeltaMs);
}
