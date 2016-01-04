package shellderp.game.ui.widget;

import shellderp.game.GameStep;
import shellderp.game.ui.Bounds;
import shellderp.game.ui.Renderable;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Superclass of all ui widgets.
 * <p>
 * When the top level widget is a RootContainer, all events are called in the same thread as step().
 * <p>
 * Created by: Mike
 */
public abstract class Widget implements GameStep, Renderable, KeyListener,
    MouseListener, MouseMotionListener, MouseWheelListener {

  private Parent parent;

  protected final Parent getParent() {
    return parent;
  }

  protected final void setParent(Parent parent) {
    this.parent = parent;
    onParentChange();
  }

  protected void onParentChange() {

  }

  public abstract Bounds getBounds();

  public Rectangle computeAbsoluteBounds() {
    if (getParent() == null) {
      throw new AssertionError("component has no parent");
    }
    return getBounds().computeAbsolute(getParent().computeAbsoluteBounds().getSize());
  }

  protected boolean hasFocus() {
    return getParent().hasFocus() && getParent().getFocusedChild() == this;
  }

  public void takeFocus() {
    getParent().setFocusedChild(this);
    getParent().takeFocus();
  }

  protected abstract boolean wantsFocus();

  // The following listener methods are implemented here to avoid clutter in subclasses
  // so they can override only the ones they need.
  @Override
  public void keyTyped(KeyEvent e) {

  }

  @Override
  public void keyPressed(KeyEvent e) {

  }

  @Override
  public void keyReleased(KeyEvent e) {

  }

  @Override
  public void mouseClicked(MouseEvent e) {

  }

  @Override
  public void mousePressed(MouseEvent e) {

  }

  @Override
  public void mouseReleased(MouseEvent e) {

  }

  @Override
  public void mouseEntered(MouseEvent e) {

  }

  @Override
  public void mouseExited(MouseEvent e) {

  }

  @Override
  public void mouseDragged(MouseEvent e) {

  }

  @Override
  public void mouseMoved(MouseEvent e) {

  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {

  }
}
