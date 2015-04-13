package shellderp.game;

/**
 * Created by: Mike
 */
public interface GameStep {

    /**
     * Take a single step of the game. This is called every iteration of the game eventloop.
     *
     * @param timeDeltaMs The time in milliseconds since the last step.
     */
    public void step(long timeDeltaMs);

}
