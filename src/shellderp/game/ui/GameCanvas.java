package shellderp.game.ui;

import shellderp.game.GameStep;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;

/**
 * Created by: Mike
 */
public final class GameCanvas extends Canvas implements GameStep {

  private final Renderable renderable;
  private BufferStrategy strategy;

  public GameCanvas(Renderable renderable) {
    this.renderable = renderable;

    setIgnoreRepaint(true);
  }

  /**
   * Called when the canvas is first displayable (isDisplayable() is true) to create the buffer strategy.
   */
  private void setupOffscreenBuffer() {
    createBufferStrategy(2);
    strategy = getBufferStrategy();
  }

  /**
   * Called when we change between fullscreen and windowed to signal we need to recreate the buffer.
   */
  public void invalidateBuffer() {
    if (strategy != null) {
      strategy.dispose();
    }
    strategy = null;
  }

  @Override
  public void step(long timeDeltaMs) {
    if (!isDisplayable()) {
      return;
    }

    // We are now displayable for the first time since being invalidated, recreate our buffer strategy.
    if (strategy == null) {
      setupOffscreenBuffer();
    }

    // Render single frame, repeating if the image is lost.
    do {
      // The following loop ensures that the contents of the drawing buffer
      // are consistent in case the underlying surface was recreated
      do {
        // Get a new graphics context every time through the loop
        // to make sure the strategy is validated
        Graphics graphics = strategy.getDrawGraphics();

        // Clear the previous rendering.
        graphics.clearRect(0, 0, getWidth(), getHeight());
        // Render the renderable onto the buffer graphics.
        renderable.render((Graphics2D) graphics);

        graphics.dispose();

        // Repeat the rendering if the drawing buffer contents
        // were restored
      } while (strategy.contentsRestored());

      // Display the buffer on screen
      strategy.show();

      // Repeat the rendering if the drawing buffer was lost
    } while (strategy.contentsLost());
  }

  @Override
  public void update(Graphics g) {
    // We handle repainting so this should never be called.
    throw new IllegalStateException();
  }

  @Override
  public void paint(Graphics g) {
    // We handle repainting so this should never be called.
    throw new IllegalStateException();
  }
}
