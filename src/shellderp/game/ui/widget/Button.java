package shellderp.game.ui.widget;

import shellderp.game.ui.Bounds;
import shellderp.game.ui.TimedColorFade;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * A widget that displays any other widget inside (eg. text, image) and can be pressed by the user.
 * The child widget cannot be interacted with.
 * <p>
 * Created by: Mike
 */
public class Button extends ParentWidget {
  private final Widget child;
  private final Bounds bounds;
  private final Runnable callback;

  private boolean mouseHeldDown = false;
  private boolean mouseOver = false;
  private final TimedColorFade timedColorFade;

  /**
   * Used to indicate when enter is pressed on the button, and is decremented every frame while above 0.
   */
  private int enterPressFrames = 0;

  public Button(Widget child, Bounds bounds, Color color, Runnable callback) {
    this.child = child;
    this.bounds = bounds;
    this.callback = callback;
    child.setParent(this);
    timedColorFade = new TimedColorFade(color, new Color(200, 0, 0), 250, () -> mouseOver);
  }

  @Override
  public Bounds getBounds() {
    return bounds;
  }

  @Override
  protected boolean wantsFocus() {
    return true;
  }

  @Override
  public void step(long timeDeltaMs) {
    child.step(timeDeltaMs);

    if (enterPressFrames > 0) {
      enterPressFrames--;
    }

    timedColorFade.step(timeDeltaMs);
  }

  @Override
  public void render(Graphics2D graphics) {
    final Rectangle r = new Rectangle(computeAbsoluteBounds().getSize());

    if (mouseHeldDown || enterPressFrames > 0) {
      graphics.setColor(new Color(0, 98, 201));
    } else {
      graphics.setColor(timedColorFade.getCurrentColor());
    }

    graphics.fillRoundRect(r.x, r.y, r.width, r.height, 15, 15);

    if (hasFocus()) {
      graphics.setColor(Color.WHITE);
      graphics.drawRoundRect(r.x, r.y, r.width, r.height, 15, 15);
    }

    renderChild(child, graphics, computeAbsoluteBounds().getSize());
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (!SwingUtilities.isLeftMouseButton(e)) {
      return;
    }
    takeFocus();
    mouseHeldDown = true;
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (!SwingUtilities.isLeftMouseButton(e)) {
      return;
    }
    mouseHeldDown = false;

    final Rectangle rectangle = new Rectangle(computeAbsoluteBounds().getSize());
    // If the user presses and releases the mouse outside of our bounds, we still get the event.
    // Users expect button presses like this to be "cancelled".
    if (rectangle.contains(e.getPoint())) {
      callback.run();
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    mouseOver = true;
  }

  @Override
  public void mouseExited(MouseEvent e) {
    mouseOver = false;
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
      enterPressFrames = 10;
      callback.run();
    } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
      if (e.isShiftDown()) {
        getParent().focusPrevious();
      } else {
        getParent().focusNext();
      }
    }
  }

  @Override
  protected void focusNext() {
  }

  @Override
  protected void focusPrevious() {
  }

  @Override
  protected Widget getFocusedChild() {
    // We don't want any events reaching the child, since it's for display only.
    return null;
  }

  @Override
  protected void setFocusedChild(Widget focusedChild) {
    throw new UnsupportedOperationException("button child cannot take focus");
  }
}
