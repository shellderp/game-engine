package shellderp.game.ui.widget;

import shellderp.game.ui.Bounds;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Add as the first child of a container to render a background for the others.
 * <p>
 * Created by: Mike
 */
public class Backdrop extends Widget {

  private final Color background;
  private final boolean rounded;

  public Backdrop(Color background, boolean rounded) {
    this.background = background;
    this.rounded = rounded;
  }

  @Override
  public Bounds getBounds() {
    return Bounds.FULL_SIZE;
  }

  @Override
  protected boolean wantsFocus() {
    return false;
  }

  @Override
  public void render(Graphics2D graphics) {
    Rectangle r = graphics.getClipBounds();
    graphics.setColor(background);
    if (rounded) {
      graphics.fillRoundRect(r.x, r.y, r.width, r.height, 15, 15);
    } else {
      graphics.fill(r);
    }
  }

  @Override
  public void step(long timeDeltaMs) {

  }
}
