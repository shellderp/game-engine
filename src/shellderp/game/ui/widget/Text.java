package shellderp.game.ui.widget;

import shellderp.game.ui.Bounds;
import shellderp.game.ui.Coord;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Created by: Mike
 */
public class Text extends Widget {
  private String text;
  private final Bounds bounds;
  private final Color foreground;
  private final Font font;

  public Text(String text, Coord x, Coord y, Color foreground, Font font) {
    this.text = text;
    this.foreground = foreground;
    this.font = font;
    // Bounds are determined by the size of the text
    bounds = new Bounds(x, y,
        (containerLength, myLength) -> Bounds.forString(font, this.text).width,
        (containerLength, myLength) -> Bounds.forString(font, this.text).height);
  }

  public void setText(String text) {
    this.text = text;
  }

  @Override
  public Bounds getBounds() {
    return bounds;
  }

  @Override
  protected boolean wantsFocus() {
    return false;
  }

  @Override
  public void render(Graphics2D graphics) {
    graphics.setFont(font);
    graphics.setColor(foreground);
    final FontMetrics fontMetrics = graphics.getFontMetrics();
    graphics.drawString(text, 0, fontMetrics.getAscent());
  }

  @Override
  public void step(long timeDeltaMs) {
  }
}
