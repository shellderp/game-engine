package shellderp.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by: Mike
 */
public final class GameLoop implements Loggable {

  private final int targetSleepMs;

  private final List<GameStep> gameSteps = new ArrayList<>();

  public GameLoop(int targetFps) {
    this.targetSleepMs = 1000 / targetFps;
  }

  public void addStep(GameStep step) {
    gameSteps.add(step);
  }

  public void loopForever() throws InterruptedException {
    Time lastIterStart = Time.now();

    while (true) {
      final Time iterStart = Time.now();

      final long timeDeltaMs = iterStart.millisSince(lastIterStart);

      for (GameStep gameStep : gameSteps) {
        gameStep.step(timeDeltaMs);
      }

      lastIterStart = iterStart;

      // We want to start the next frame at time iterStart + targetSleepMs, but some time has
      // passed in processing. So we remove the time spent since the start of the iteration.
      final Time iterEnd = Time.now();
      final long sleepTimeMs = iterStart.millisSince(iterEnd) + targetSleepMs;

      // TODO: consider keeping metrics of average processing time

      if (sleepTimeMs < 0) {
        if (sleepTimeMs < -targetSleepMs) {
          logger().info(String.format(
              "processing time (%d ms) exceeds two frames (%d ms per frame)",
              iterEnd.millisSince(iterStart), targetSleepMs));
        }
      } else {
        Thread.sleep(sleepTimeMs);
      }
    }
  }

}
