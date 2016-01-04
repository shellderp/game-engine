package shellderp.game.ui.widget;

import shellderp.game.ui.Bounds;
import shellderp.game.ui.Coord;
import shellderp.game.ui.Coords;
import shellderp.game.ui.Sprite;

import java.awt.Graphics2D;

/**
 * Created by: Mike
 */
public class Icon extends Widget {
  private final Sprite sprite;
  private final Bounds bounds;

  public Icon(Sprite sprite, Coord x, Coord y) {
    this.sprite = sprite;
    // Bounds are determined by the size of the sprite
    bounds = new Bounds(x, y, Coords.fixed(sprite.getWidth()), Coords.fixed(sprite.getHeight()));
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
    sprite.render(graphics);
  }

  @Override
  public void step(long timeDeltaMs) {
  }
}
