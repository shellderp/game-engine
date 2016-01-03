package shellderp.game;

import shellderp.game.ui.Renderable;

import java.awt.Graphics2D;

/**
 * Wraps the currently active state so that states can be changed
 * without modifying the game canvas or the game loop.
 * <p>
 * Created by: Mike
 */
public final class StateManager implements GameStep, Renderable {

  private GameState state;

  /**
   * Set the currently active state. If called from the current state,
   * this should only be called during the state's step() method.
   *
   * @param newState
   */
  public void setState(GameState newState) {
    if (this.state != null) {
      this.state.dispose();
    }
    this.state = newState;
    newState.activate();
  }

  @Override
  public void step(long timeDeltaMs) {
    if (state != null) {
      state.step(timeDeltaMs);
    }
  }

  @Override
  public void render(Graphics2D graphics) {
    if (state != null) {
      state.render(graphics);
    }
  }
}
