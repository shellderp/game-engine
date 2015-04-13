package shellderp.game.network;

import java.util.Optional;

/**
 * Facilitates piggybacking ACKs on data messages.
 * <p>
 * This works as follows:
 * When the reliable stream wants to send an ACK in normal behavior, it indicates the sequence to ack.
 * This class then starts a timer if one isn't already active, with the hope that before the timer runs out,
 * one of the streams in the connection will try to send a data packet. We can then add the ack to the data
 * packet and skip sending an empty ack packet.
 * <br>
 * If the timer runs out, we send an ack with no payload.
 * <br>
 * If the reliable stream detects packet loss, it will skip the timer and immediately send an empty ack to
 * try to rectify the packet loss as fast as possible.
 * <p>
 * If a timer is already running and the reliable stream wants to send a new ack, we just update the ack value
 * and continue to run the timer from its old start time.
 * <p>
 * All methods are thread safe since this is intended to be called from the ReliableStream::packetReceived
 * which executes in ReceiveThread, and in Connection::send which can execute in any thread.
 * <p>
 * Created by Mike on 2015-04-12.
 */
public class PiggybackAck {

    /**
     * How long we wait after the first ack was set until sending a payload-less packet.
     * Since we expect the game to send frequent location packets, we keep this close to the game tick rate.
     * This should be kept low, since we are limiting what the other end can send by not sending an ack.
     */
    private final static long timeoutMs = 50;

    private boolean hasAck = false;

    private int ackSequence;

    private Timer timer = new Timer();

    public synchronized Optional<Integer> getAndClearAck() {
        if (!hasAck) {
            return Optional.empty();
        }

        hasAck = false;
        timer.stop();

        return Optional.of(ackSequence);
    }

    public synchronized Optional<Integer> getAndClearAckIfTimeoutPassed() {
        if (timer.hasPassed(timeoutMs)) {
            return getAndClearAck();
        }

        return Optional.empty();
    }

    public synchronized void setAckSequence(int ackSequence) {
        this.ackSequence = ackSequence;
        hasAck = true;

        if (!timer.isActive()) {
            timer.restart();
        }
    }

}
