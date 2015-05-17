package shellderp.game.network;

import shellderp.game.Time;
import shellderp.game.Timer;

import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is not thread safe. All access is expected to be synchronized externally.
 * <p>
 * Created by: Mike
 */
class GoBackNWindow {

    private static class WindowEntry {
        final Packet packet;
        Time lastSentTime;

        private WindowEntry(Packet packet) {
            this.packet = packet;
            this.lastSentTime = Time.now();
        }

        public void markResent() {
            lastSentTime = Time.now();
        }
    }

    private static final int MIN_WINDOW_SIZE = 5;
    private static final int MAX_WINDOW_SIZE = 100;

    /**
     * The max number of packets we can send without receiving ACK.
     */
    private int windowSize = MIN_WINDOW_SIZE;

    /**
     * List of packets sent but unacked. The length is at most windowSize, unless windowSize was recently
     * decreased.
     * <p>
     * We could use a circular array for this to remove en masse, but we need to look at each entry as we
     * remove it anyway so a linked list is fine.
     */
    private LinkedList<WindowEntry> sent = new LinkedList<>();

    /**
     * Sequence number offset; the sequence of the first packet in the window.
     */
    private int sequenceOut;

    /**
     * This timer keeps track of the oldest sent packet that is not yet acked.
     * When an ACK is received, it is restarted if more packets remain, or set to 0 if empty.
     * When there is a timeout and packets must be resent, it is restarted.
     */
    private final Timer retransmitTimer = new Timer();

    private final VariableTimeout variableTimeout;

    /**
     * This timer keeps track of the last useful ACK received. We use this to determine if the connection is
     * dead when ACKs are expected but none have come in for some time.
     * The only difference between this and the retransmit timer is that we don't reset this one when we
     * retransmit.
     */
    private final Timer closeTimer = new Timer();

    /**
     * If enabled, we retransmit the window when we see three ACKs in a row with the same sequence.
     */
    private boolean fastRetransmit = true;

    /**
     * When fast retransmit is enabled, this stores the number of ACKs we've seen in a row with the same
     * sequence.
     */
    private int numRepeatedAcks = 0;

    public GoBackNWindow(int initialSequenceOut, long initialTimeoutMs) {
        this.sequenceOut = initialSequenceOut;
        variableTimeout = new VariableTimeout(initialTimeoutMs);
    }

    public void setFastRetransmit(boolean fastRetransmit) {
        this.fastRetransmit = fastRetransmit;
    }

    public boolean isFull() {
        return sent.size() >= windowSize;
    }

    /**
     * Check if there is a timeout and packets need to be resent.
     * If true, the timer is restarted.
     */
    public boolean needToRetransmit() {
        if (fastRetransmit && numRepeatedAcks >= 3) {
            numRepeatedAcks = 0;

            // Since we are resending now, we don't want the current timer to trigger again if one is active.
            if (retransmitTimer.isActive()) {
                retransmitTimer.restart();
            }

            return true;
        }

        final boolean timeout = retransmitTimer.hasPassed(variableTimeout.getTimeoutMs());

        if (timeout) {
            retransmitTimer.restart();

            // Reduce the window size, since we have lost packets.
            windowSize = Math.max(windowSize / 2, MIN_WINDOW_SIZE);
        }

        return timeout;
    }

    public boolean hasPassedSinceLastUsefulAck(long timeoutMs) {
        return closeTimer.hasPassed(timeoutMs);
    }

    public List<Packet> packetsToRetransmit() {
        List<Packet> packets = new ArrayList<>();
        for (WindowEntry windowEntry : sent) {
            windowEntry.markResent();
            packets.add(windowEntry.packet);
        }
        return packets;
    }

    /**
     * Called when an ACK is received. Shifts the window forward the appropriate amount and updates
     * the timer.
     */
    public void ackReceived(int ackSequence) throws ProtocolException {
        if (!Packet.newerThanExpected(sequenceOut, ackSequence)) {
            return; // Received a delayed ACK for previously ACKed packet.
        }

        int numToRemove = ackSequence - sequenceOut;
        if (numToRemove < 0) {
            numToRemove += Packet.MAX_SEQUENCE;
        }

        if (numToRemove > sent.size()) {
            throw new ProtocolException(
                    String.format("#ACKed > window size; exp: %d, ack: %d, dist: %d, window size: %d%n",
                                  sequenceOut, ackSequence, numToRemove, windowSize));
        }

        if (fastRetransmit) {
            if (ackSequence == sequenceOut) {
                numRepeatedAcks++;
            } else {
                numRepeatedAcks = 0;
            }
        }

        for (int i = 0; i < numToRemove; i++) {
            long roundTripTimeMs = Time.now().millisSince(sent.getFirst().lastSentTime);
            variableTimeout.updateFromSample(roundTripTimeMs);

            sent.removeFirst();

            // Increase the window size on a successful ack.
            windowSize = Math.min(windowSize + 1, MAX_WINDOW_SIZE);
        }

        sequenceOut = ackSequence;

        // We got a useful ack, now we can restart the timers to keep track of the first unacked packet.
        if (sent.isEmpty()) {
            retransmitTimer.stop();
            closeTimer.stop();
        } else {
            retransmitTimer.restart();
            closeTimer.restart();
        }
    }

    /**
     * Called when a packet is sent, to be stored until it is ACKed.
     * The timer is set unless it is already set.
     */
    public void packetSent(Packet packet) {
        if (!Packet.newerThanExpected(sequenceOut, packet.getSequence())) {
            throw new IllegalArgumentException(
                    "send window is expecting a higher sequence number; got " + packet.getSequence() +
                    ", expected at least: " + sequenceOut);
        }

        // If there's no timer active, start one now. If there is one active, we want to time the first
        // packet in the window, so don't restart it.
        if (!retransmitTimer.isActive()) {
            retransmitTimer.restart();
        }
        if (!closeTimer.isActive()) {
            closeTimer.restart();
        }

        sent.add(new WindowEntry(packet));
    }

}
