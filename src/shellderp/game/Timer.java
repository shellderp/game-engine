package shellderp.game;

/**
 * Simple timer. Starts inactive.
 *
 * Created by Mike on 2015-04-12.
 */
public class Timer {

    /**
     * Tracks the last time the timer was started. If 0, then the timer is not active.
     */
    private long startTimeMs = 0;

    public boolean isActive() {
        return startTimeMs != 0;
    }

    public void restart() {
        startTimeMs = System.currentTimeMillis();
    }

    public void stop() {
        startTimeMs = 0;
    }

    public boolean hasPassed(long timeoutMs) {
        long now = System.currentTimeMillis();

        return isActive() && ((now - startTimeMs) > timeoutMs);
    }
}
