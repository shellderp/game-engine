package shellderp.game.ui.widget;

import shellderp.game.ui.Bounds;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A widget that can contain other widgets. Dispatches input events to children as needed.
 * It is assumed that events are dispatched on the same thread as step(),
 * which is guaranteed when using a RootContainer.
 * <p>
 * Created by: Mike
 */
public class Container extends Parent {

  /**
   * Enable to show bounding boxes around each child.
   * This can be using for debugging bounds or clipping issues.
   */
  public static boolean debugShowBoundingBoxes = false;
  public static boolean debugShowFocus = false;

  private final Bounds bounds;
  private final List<Widget> children = new ArrayList<>();
  private Widget focusedChild = null;

  /**
   * We keep track of which widgets received mouse presses for each button. This way, we can call
   * mouseReleased to the right widget even if the user moves the mouse out of the widget's bounds.
   */
  private final Map<Integer, Widget> mousePressReceiverByButton = new HashMap<>();

  /**
   * The last child that had the mouse in it's bounds.
   */
  private Widget lastMouseEnterChild = null;

  /**
   * We provide a default constructor for subclasses with bounds set to null. This means subclasses must
   * override getBounds() or getAbsoluteBounds().
   */
  protected Container() {
    this.bounds = null;
  }

  public Container(Bounds bounds) {
    this.bounds = bounds;
  }

  @Override
  public Bounds getBounds() {
    return bounds;
  }

  @Override
  protected boolean wantsFocus() {
    // If any child wants focus, we want focus.
    for (Widget child : children) {
      if (child.wantsFocus()) {
        return true;
      }
    }
    return false;
  }

  protected Widget getFocusedChild() {
    return focusedChild;
  }

  protected void setFocusedChild(Widget focusedChild) {
    this.focusedChild = focusedChild;
  }

  protected final void focusNext() {
    // Thinking recursively, this won't be set only when this is called by the previous container's
    // focusNext(), where we check for instanceof Parent below.
    if (hasFocus()) {
      final int currentFocusedIndex = children.indexOf(getFocusedChild());
      for (int i = currentFocusedIndex + 1; i < children.size(); i++) {
        final Widget child = children.get(i);
        if (child.wantsFocus()) {
          if (child instanceof Parent) {
            ((Parent) child).focusNext();
          }
          setFocusedChild(child);
          return;
        }
      }
      // Reached last child, so tell the parent to unfocus us and focus our neighbour.
      if (getParent() != null) {
        getParent().focusNext();
        return;
      }
    }
    // Find the first child that wants focus. We are guaranteed to have one, since wantsFocus()
    // returned true (otherwise we wouldn't be here). The only other case is for RootContainer, but if
    // we find nothing, we have no parent to divert to anyway.
    for (Widget child : children) {
      if (child.wantsFocus()) {
        if (child instanceof Parent) {
          ((Parent) child).focusNext();
        }
        setFocusedChild(child);
        return;
      }
    }
  }

  @Override
  protected void focusPrevious() {
    // TODO remove code duplication between focusNext and focusPrevious
    if (hasFocus()) {
      final int currentFocusedIndex = children.indexOf(getFocusedChild());
      for (int i = currentFocusedIndex - 1; i >= 0; i--) {
        final Widget child = children.get(i);
        if (child.wantsFocus()) {
          if (child instanceof Parent) {
            ((Parent) child).focusPrevious();
          }
          setFocusedChild(child);
          return;
        }
      }
      // Reached last child, so tell the parent to unfocus us and focus our neighbour.
      if (getParent() != null) {
        getParent().focusPrevious();
        return;
      }
    }
    // Find the last child that wants focus. We are guaranteed to have one, since wantsFocus()
    // returned true (otherwise we wouldn't be here). The only other case is for RootContainer, but if
    // we find nothing, we have no parent to divert to anyway.
    final ListIterator<Widget> iterator = children.listIterator(children.size());
    while (iterator.hasPrevious()) {
      final Widget child = iterator.previous();
      if (child.wantsFocus()) {
        if (child instanceof Parent) {
          ((Parent) child).focusPrevious();
        }
        setFocusedChild(child);
        return;
      }
    }
  }

  public final void add(Widget widget) {
    if (children.contains(widget)) {
      throw new IllegalStateException("widget already added to this container");
    }

    children.add(widget);
    widget.setParent(this);
  }

  public final void remove(Widget widget) {
    children.remove(widget);
    widget.setParent(null);
    if (widget == getFocusedChild()) {
      // Give focus to the first child.
      for (Widget child : children) {
        if (child.wantsFocus()) {
          if (child instanceof Parent) {
            ((Parent) child).focusNext();
          }
          setFocusedChild(child);
          return;
        }
      }
    }
  }

  @Override
  public void step(long timeDeltaMs) {
    for (Widget child : children) {
      child.step(timeDeltaMs);
    }
  }

  /**
   * Render all children at their relative locations.
   * <p>
   * This method is final; if a subclass wants to render something under all the widgets, it should add
   * a widget as the first child with 100% width and height then render to that.
   */
  @Override
  public final void render(Graphics2D graphics) {
    final Dimension mySize = computeAbsoluteBounds().getSize();

    for (final Widget child : children) {
      renderChild(child, graphics, mySize);

      if (debugShowBoundingBoxes) {
        final Rectangle childBounds = child.getBounds().computeAbsolute(mySize);
        graphics.setColor(Color.WHITE);
        graphics.draw(childBounds);
      }
      if (debugShowFocus && hasFocus() && child == getFocusedChild()) {
        final Rectangle childBounds = child.getBounds().computeAbsolute(mySize);
        graphics.setColor(Color.RED);
        graphics.draw(childBounds);
      }
    }
  }

  /**
   * Determines which child should receive the event for an event at point.
   *
   * @param point The location of the event relative to this container's bounds.
   * @return The child that should receive the event, or null if no child should receive it.
   */
  private Widget getTopChildAt(final Point point) {
    final Dimension mySize = computeAbsoluteBounds().getSize();

    // We iterate in reverse, since the last child gets rendered above all others.
    final ListIterator<Widget> iterator = children.listIterator(children.size());
    while (iterator.hasPrevious()) {
      final Widget child = iterator.previous();
      final Rectangle childBounds = child.getBounds().computeAbsolute(mySize);
      if (childBounds.contains(point)) {
        return child;
      }
    }

    return null;
  }

  @Override
  public final void mouseClicked(MouseEvent e) {
    final Widget child = getTopChildAt(e.getPoint());
    if (child != null) {
      final Rectangle childBounds = child.computeAbsoluteBounds();
      e.translatePoint(-childBounds.x, -childBounds.y);
      child.mouseClicked(e);
      e.translatePoint(childBounds.x, childBounds.y);
    }
  }

  @Override
  public final void mousePressed(MouseEvent e) {
    final Widget child = getTopChildAt(e.getPoint());
    if (child != null) {
      mousePressReceiverByButton.put(e.getButton(), child);
      final Rectangle childBounds = child.computeAbsoluteBounds();
      e.translatePoint(-childBounds.x, -childBounds.y);
      child.mousePressed(e);
      e.translatePoint(childBounds.x, childBounds.y);
    }
  }

  @Override
  public final void mouseReleased(MouseEvent e) {
    final Widget childReleased;
    if (mousePressReceiverByButton.containsKey(e.getButton())) {
      childReleased = mousePressReceiverByButton.remove(e.getButton());
      final Rectangle childBounds = childReleased.computeAbsoluteBounds();
      e.translatePoint(-childBounds.x, -childBounds.y);
      childReleased.mouseReleased(e);
      e.translatePoint(childBounds.x, childBounds.y);
    } else {
      childReleased = null;
    }

    // This is a workaround for the specific case when the above code triggers a Button which removes
    // us from our parent.
    if (getParent() == null) {
      return;
    }

    // The mouse was just being dragged, so if the mouse left the child that was pressed, only that
    // child will have received mouseDragged events. We want to also send a mouseEntered if the mouse
    // was just released over a different child.
    // Note this is only done for the left mouse button.
    if (e.getButton() == MouseEvent.BUTTON1) {
      // Note: we send a mouseMove to the child, not mouseRelease, because to this child there was
      // never a initial mousePressed event so in its perspective the mouse only moved.
      final Widget childHere = getTopChildAt(e.getPoint());
      updateMouseEnterExit(childHere);

      if (childHere != null && childReleased != childHere) {
        final Rectangle childBounds = childHere.computeAbsoluteBounds();
        e.translatePoint(-childBounds.x, -childBounds.y);
        childHere.mouseMoved(e);
        e.translatePoint(childBounds.x, childBounds.y);
      }
    }
  }

  @Override
  public final void mouseEntered(MouseEvent e) {
    // mouseMoved should handle this, by detecting the next movement when lastMousePoint is null
  }

  @Override
  public final void mouseExited(MouseEvent e) {
    if (lastMouseEnterChild == null) {
      return;
    }
    // We don't need to do much here, but its possible in some cases we need to notify a child,
    // for example when there is a widget right on the edge of our container - thus the mouse left this
    // one and the child.
    lastMouseEnterChild.mouseExited(e);
    lastMouseEnterChild = null;
  }

  @Override
  public final void mouseMoved(MouseEvent e) {
    mouseMovedOrDragged(e, false);
  }

  @Override
  public final void mouseDragged(MouseEvent e) {
    // If this drag was initiated on a child, we send that child events even when we leave its bounds.
    // For example think about dragging a scroll bar and moving the mouse away.
    // For simplicity we only look at the left mouse button (button 1).
    if (mousePressReceiverByButton.containsKey(MouseEvent.BUTTON1)) {
      final Widget child = mousePressReceiverByButton.get(MouseEvent.BUTTON1);
      final Rectangle childBounds = child.computeAbsoluteBounds();

      e.translatePoint(-childBounds.x, -childBounds.y);
      child.mouseDragged(e);
      e.translatePoint(childBounds.x, childBounds.y);
      return;
    }

    mouseMovedOrDragged(e, true);
  }

  private void mouseMovedOrDragged(MouseEvent e, boolean drag) {
    final Widget childHere = getTopChildAt(e.getPoint());

    updateMouseEnterExit(childHere);

    if (childHere != null) {
      final Dimension mySize = computeAbsoluteBounds().getSize();
      final Rectangle childBounds = childHere.getBounds().computeAbsolute(mySize);

      e.translatePoint(-childBounds.x, -childBounds.y);
      if (drag) {
        childHere.mouseDragged(e);
      } else {
        childHere.mouseMoved(e);
      }
      e.translatePoint(childBounds.x, childBounds.y);
    }
  }

  private void updateMouseEnterExit(final Widget childHere) {
    if (lastMouseEnterChild != childHere) {
      if (lastMouseEnterChild != null) {
        lastMouseEnterChild.mouseExited(null);
      }
      if (childHere != null) {
        childHere.mouseEntered(null);
      }
    }
    lastMouseEnterChild = childHere;
  }

  @Override
  public final void mouseWheelMoved(MouseWheelEvent e) {
    final Widget child = getTopChildAt(e.getPoint());
    if (child != null) {
      final Rectangle childBounds = child.computeAbsoluteBounds();
      e.translatePoint(-childBounds.x, -childBounds.y);
      child.mouseWheelMoved(e);
      e.translatePoint(childBounds.x, childBounds.y);
    }
  }
}
