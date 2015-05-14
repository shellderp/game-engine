package shellderp.game.ui;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Created by: Mike
 */
public class FpsOverlay implements Renderable {

    private long frameTimerMs;
    private int frames;
    private int lastFps;

    @Override
    public void render(Graphics g) {
        if (System.currentTimeMillis() - frameTimerMs > 1000) {
            frameTimerMs = System.currentTimeMillis();
            lastFps = frames;
            frames = 0;
        } else {
            frames++;
        }

        g.setColor(Color.BLACK);
        g.drawString("FPS: " + lastFps, 0, g.getFontMetrics().getHeight());
    }
}
