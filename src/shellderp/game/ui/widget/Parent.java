package shellderp.game.ui.widget;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;

/**
 * A parent is a widget that can contain children.
 * <p>
 * Created by: Mike
 */
public abstract class Parent extends Widget {

  protected abstract Widget getFocusedChild();

  protected abstract void setFocusedChild(Widget focusedChild);

  @Override
  public void keyTyped(KeyEvent e) {
    if (getFocusedChild() != null) {
      getFocusedChild().keyTyped(e);
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (getFocusedChild() != null) {
      getFocusedChild().keyPressed(e);
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    if (getFocusedChild() != null) {
      getFocusedChild().keyReleased(e);
    }
  }

  /**
   * Renders a child widget with the appropriate translation and clipping.
   */
  protected static void renderChild(Widget child, Graphics2D graphics, Dimension mySize) {
    final Shape originalClip = graphics.getClip();
    final Rectangle childBounds = child.getBounds().computeAbsolute(mySize);
    graphics.translate(childBounds.x, childBounds.y);

    // Note we must use clipRect below, not setClip, because setClip will ignore the previous clip
    // and allow the child to exceed our parent's bounds. clipRect adds these bounds to the existing clip.
    // Adding one to both width and height lets us do graphics.draw(Shape), otherwise it would be clipped.
    graphics.clipRect(0, 0, childBounds.width + 1, childBounds.height + 1);
    child.render(graphics);

    graphics.translate(-childBounds.x, -childBounds.y);
    graphics.setClip(originalClip);
  }

  protected abstract void focusNext();

  protected abstract void focusPrevious();

}
