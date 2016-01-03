package shellderp.game.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Created by: Mike
 */
public interface Renderable {
  void render(Graphics2D graphics);

  default void enableAntiAliasing(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // From trial and error, this looks to be the best for all fonts.
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
  }
}
