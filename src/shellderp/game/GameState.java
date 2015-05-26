package shellderp.game;

import shellderp.game.ui.Renderable;

/**
 * Created by: Mike
 */
public interface GameState extends GameStep, Renderable {
    /**
     * Called by StateManager when the state becomes active.
     */
    public void activate();

    /**
     * Called by StateManager when the state stops being active. All listeners should be removed at this time.
     */
    public void dispose();
}
