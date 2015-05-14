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
        long lastIterStartMs = System.currentTimeMillis();

        while (true) {
            final long iterStartMs = System.currentTimeMillis();

            final long timeDeltaMs = iterStartMs - lastIterStartMs;

            for (GameStep gameStep : gameSteps) {
                // TODO: consider computing a time delta at each step, since the other steps may cause a delay
                gameStep.step(timeDeltaMs);
            }

            lastIterStartMs = iterStartMs;

            // We want to start the next frame at time iterStartMs + targetSleepMs, but some time has
            // passed in processing. So we remove the time spent since the start of the iteration.
            final long iterEndMs = System.currentTimeMillis();
            final long sleepTimeMs = (iterStartMs - iterEndMs) + targetSleepMs;

            // TODO: consider keeping metrics of average processing time

            if (sleepTimeMs < 0) {
                logger().warning(String.format("processing time (%d ms) exceeds desired frame rate (%d ms)",
                                               iterEndMs - iterStartMs, targetSleepMs));
            } else {
                Thread.sleep(sleepTimeMs);
            }
        }
    }

}
