package shellderp.game.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Created by: Mike
 */
public interface Renderable {
    public void render(Graphics2D graphics);

    default void enableAntiAliasing(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
}
