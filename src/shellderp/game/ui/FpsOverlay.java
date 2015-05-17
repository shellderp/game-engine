package shellderp.game.ui;

import shellderp.game.Timer;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Created by: Mike
 */
public class FpsOverlay implements Renderable {
    private final Color textColor;

    private final Timer secondTimer = new Timer();
    private int frames;
    private int lastFps;

    public FpsOverlay(Color textColor) {
        this.textColor = textColor;
    }

    @Override
    public void render(Graphics2D g) {
        if (!secondTimer.isActive()) {
            secondTimer.restart();
        }

        if (secondTimer.hasPassed(1000)) {
            secondTimer.restart();
            lastFps = frames;
            frames = 0;
        } else {
            frames++;
        }

        g.setColor(textColor);
        g.drawString("FPS: " + lastFps, 2, g.getFontMetrics().getAscent());
    }
}
