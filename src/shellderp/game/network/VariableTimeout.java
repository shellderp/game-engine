package shellderp.game.network;

/**
 * Essentially copying the TCP method of estimating Round-Trip-Time to pick a good timeout.
 * <p>
 * Created by: Mike
 */
public class VariableTimeout {

    /**
     * How much we value previous estimatedRtt vs the current sample.
     */
    private final static float alpha = 0.125f;

    /**
     * How much we value previous deviationRtt vs the current sample.
     */
    private final static float beta = 0.25f;

    private long estimatedRtt;

    private long deviationRtt;

    /**
     * A default value to use until we get some sample RTT's.
     */
    private final long defaultTimeoutMs;
    private boolean gotSample = false;

    public VariableTimeout(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
        estimatedRtt = 0;
        deviationRtt = 0;
    }

    public long getTimeoutMs() {
        if (!gotSample) {
            return defaultTimeoutMs;
        }

        long timeoutMs = estimatedRtt + (4 * deviationRtt);
        return (timeoutMs < 10) ? 10 : timeoutMs;
    }

    public void updateFromSample(long sampleRtt) {
        estimatedRtt = (long) ((1 - alpha) * estimatedRtt + alpha * sampleRtt);
        deviationRtt = (long) ((1 - beta) * deviationRtt + beta * Math.abs(sampleRtt - estimatedRtt));

        gotSample = true;
    }
}
