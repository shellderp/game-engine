package shellderp.game.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

/**
 * Created by: Mike
 */
public class Bounds {
  private final Coord x, y, w, h;

  public Bounds(Coord x, Coord y, Coord w, Coord h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  /**
   * Compute the bounds in pixels.
   *
   * @param containerSize The computed size of the widget that contains this one.
   * @return The bounds in pixels.
   */
  public Rectangle computeAbsolute(Dimension containerSize) {
    final int width = w.computeAbsoluteLength(containerSize.width, -1);
    final int height = h.computeAbsoluteLength(containerSize.height, -1);
    return new Rectangle(x.computeAbsoluteLength(containerSize.width, width),
        y.computeAbsoluteLength(containerSize.height, height),
        width, height);
  }

  public static final Bounds FULL_SIZE = new Bounds(Coords.fixed(0), Coords.fixed(0),
      Coords.percent(1), Coords.percent(1));

  public static Rectangle forString(Font font, String text) {
    return font.getStringBounds(text, getFontRenderContext()).getBounds();
  }

  public static int fontHeight(Font font) {
    return font.getMaxCharBounds(getFontRenderContext()).getBounds().height;
  }

  private static FontRenderContext getFontRenderContext() {
    return new FontRenderContext(new AffineTransform(), true, false);
  }

}
