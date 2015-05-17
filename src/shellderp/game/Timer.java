package shellderp.game;

/**
 * Simple timer. Starts inactive.
 * <p>
 * Created by Mike on 2015-04-12.
 */
public class Timer {

    private boolean active = false;

    /**
     * Tracks the last time the timer was started.
     */
    private Time startTime = null;

    public boolean isActive() {
        return active;
    }

    public void restart() {
        active = true;
        startTime = Time.now();
    }

    public void stop() {
        active = false;
    }

    public boolean hasPassed(long timeoutMs) {
        if (!isActive()) {
            return false;
        }

        return Time.now().millisSince(startTime) > timeoutMs;
    }
}
