package shellderp.game.ui.widget;

import shellderp.game.ui.Renderable;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Created by: Mike
 */
public class ProgressBar implements Renderable {
  private final Color fill;
  private final Color empty;
  private final Color border;
  private final Color textcolor;
  private double value;
  private double max;
  private String text;
  private Rectangle bounds;

  public ProgressBar(Color fill, Color empty, Color border, Color textcolor) {
    this.fill = fill;
    this.empty = empty;
    this.border = border;
    this.textcolor = textcolor;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public void setMax(double max) {
    this.max = max;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setBounds(Rectangle bounds) {
    this.bounds = bounds;
  }

  public void render(Graphics2D g) {
    g.setColor(empty);
    g.fill(bounds);

    int barWidth = (int) ((value / max) * bounds.width);
    Rectangle fillRect = new Rectangle(bounds.x, bounds.y, barWidth, bounds.height);
    g.setColor(fill);
    g.fill(fillRect);

    g.setColor(border);
    g.draw(bounds);

    if (text != null) {
      FontMetrics fm = g.getFontMetrics();
      int x = bounds.x + (bounds.width - fm.stringWidth(text)) / 2, y = bounds.y + fm.getHeight();
      char[] chars = text.toCharArray();
      g.setColor(textcolor);
      for (int i = 0; i < chars.length; i++) {
        if (x - bounds.x > barWidth) {
          g.setColor(fill);
        }
        g.drawChars(new char[]{chars[i]}, 0, 1, x, y);
        x += fm.charWidth(chars[i]);
      }
    }
  }
}
