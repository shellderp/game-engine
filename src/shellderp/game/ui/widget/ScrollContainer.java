package shellderp.game.ui.widget;

import shellderp.game.ui.Bounds;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Created by: Mike
 */
public class ScrollContainer {
  /**
   * Creates a container that embeds the child widget and allows scrolling to show only the selected part
   * of the contents at a time.
   * Currently only vertical scrolling is supported, thus the child bounds.width should be 100%.
   */
  public static Container create(final Widget child, final Bounds bounds) {
    Container scrollContainer = new Container(bounds);
    // TODO: also add the scrollbars and adjust ScrollParent's width/height
    scrollContainer.add(new ScrollParent(child, Bounds.FULL_SIZE));
    return scrollContainer;
  }

  private static class ScrollParent extends Parent {
    private final Widget child;
    private final Bounds bounds;

    private int scrollX = 0, scrollY = 0;

    public ScrollParent(final Widget child, final Bounds bounds) {
      this.child = child;
      this.bounds = bounds;
      child.setParent(this);
    }

    @Override
    protected Widget getFocusedChild() {
      return child;
    }

    @Override
    protected void setFocusedChild(Widget focusedChild) {
      if (focusedChild != child) {
        throw new AssertionError("scroll container focusing on non child");
      }
    }

    @Override
    protected void focusNext() {
      // If we have focus we are being called from the child, otherwise we are called from Container
      // since we are a Parent so ignore.
      if (hasFocus()) {
        getParent().focusNext();
      } else if (child instanceof Parent) {
        ((Parent) child).focusNext();
      }
    }

    @Override
    protected void focusPrevious() {
      if (hasFocus()) {
        getParent().focusPrevious();
      } else if (child instanceof Parent) {
        ((Parent) child).focusNext();
      }
    }

    @Override
    public Bounds getBounds() {
      return bounds;
    }

    @Override
    protected boolean wantsFocus() {
      return child.wantsFocus();
    }

    @Override
    public void step(long timeDeltaMs) {
      child.step(timeDeltaMs);
    }

    @Override
    public void render(Graphics2D graphics) {
      // Note we don't need to do any clipping because the scroll viewport takes up the size of the
      // container.
      graphics.translate(-scrollX, -scrollY);
      child.render(graphics);
      graphics.translate(scrollX, scrollY);
    }

    private void translateMouseEvent(MouseEvent mouseEvent) {
      mouseEvent.translatePoint(scrollX, scrollY);
    }

    private void untranslateMouseEvent(MouseEvent mouseEvent) {
      mouseEvent.translatePoint(-scrollX, -scrollY);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      translateMouseEvent(e);
      child.mouseClicked(e);
      untranslateMouseEvent(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
      translateMouseEvent(e);
      child.mousePressed(e);
      untranslateMouseEvent(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      translateMouseEvent(e);
      child.mouseReleased(e);
      untranslateMouseEvent(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      child.mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
      child.mouseExited(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      translateMouseEvent(e);
      child.mouseDragged(e);
      untranslateMouseEvent(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      translateMouseEvent(e);
      child.mouseMoved(e);
      untranslateMouseEvent(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
      // For now just steal this event to scroll. Later, we may want to change this to allow scroll
      // containers within this one to also receive these events. We can do this by consuming the
      // event in the deepest scroll container only if it scrolls (ie. it isn't at its end).
      final Dimension mySize = computeAbsoluteBounds().getSize();
      final Dimension childSize = child.getBounds().computeAbsolute(mySize).getSize();
      int maxScrollY = childSize.height - mySize.height;
      if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
        scrollY += e.getUnitsToScroll();
      } else if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
        // The platform mouse setting is to scroll down an entire screen for every tick.
        scrollY += e.getWheelRotation() * computeAbsoluteBounds().height;
      }
      scrollY = Math.max(0, Math.min(scrollY, maxScrollY));
    }
  }
}
