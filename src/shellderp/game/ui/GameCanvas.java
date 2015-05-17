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

    /**
     * This can't be final because we can only create it after we are displayable.
     */
    private BufferStrategy strategy;

    public GameCanvas(Renderable renderable) {
        this.renderable = renderable;

        setIgnoreRepaint(true);
    }

    /**
     * Called when the canvas is first displayable (isDisplayable() is true) to create the buffer strategy.
     */
    private void setupOnceDisplayable() {
        createBufferStrategy(2);
        strategy = getBufferStrategy();
    }

    @Override
    public void step(long timeDeltaMs) {
        if (!isDisplayable()) {
            return;
        }

        // Displayable for the first time.
        if (strategy == null) {
            setupOnceDisplayable();
        }

        // Render single frame
        do {
            // The following loop ensures that the contents of the drawing buffer
            // are consistent in case the underlying surface was recreated
            do {
                // Get a new graphics context every time through the loop
                // to make sure the strategy is validated
                Graphics graphics = strategy.getDrawGraphics();

                // Clear the previous rendering.
                graphics.clearRect(0, 0, getWidth(), getHeight());
                // Render to graphics.
                renderable.render((Graphics2D) graphics);

                // Dispose the graphics
                graphics.dispose();

                // Repeat the rendering if the drawing buffer contents
                // were restored
            } while (strategy.contentsRestored());

            // Display the buffer
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
