package shellderp.game.ui.widget;

import shellderp.game.ui.Bounds;
import shellderp.game.ui.GameCanvas;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A container that is displayed at the root of the canvas and attaches listeners to it.
 * Only one should be used at a time per canvas.
 * Note that all events will be dispatched during a call to step() instead of the AWT event thread
 * to simplify widget implementation
 * <p>
 * Created by: Mike
 */
public class RootContainer extends Container {

  private final GameCanvas gameCanvas;

  /**
   * When we receive events from the event thread, we add them to this queue to be run on the next step().
   */
  private final ConcurrentLinkedQueue<Runnable> events = new ConcurrentLinkedQueue<>();
  private final MouseListener mouseListener;
  private final MouseMotionListener mouseMotionListener;
  private final MouseWheelListener mouseWheelListener;
  private final KeyListener keyListener;

  public RootContainer(GameCanvas gameCanvas) {
    this.gameCanvas = gameCanvas;

    mouseListener = new MouseListener() {
      @Override
      public void mouseClicked(MouseEvent e) {
        events.add(() -> RootContainer.this.mouseClicked(e));
      }

      @Override
      public void mousePressed(MouseEvent e) {
        events.add(() -> RootContainer.this.mousePressed(e));
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        events.add(() -> RootContainer.this.mouseReleased(e));
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        events.add(() -> RootContainer.this.mouseEntered(e));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        events.add(() -> RootContainer.this.mouseExited(e));
      }
    };
    mouseMotionListener = new MouseMotionListener() {
      @Override
      public void mouseDragged(MouseEvent e) {
        events.add(() -> RootContainer.this.mouseDragged(e));
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        events.add(() -> RootContainer.this.mouseMoved(e));
      }
    };
    mouseWheelListener = new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        events.add(() -> RootContainer.this.mouseWheelMoved(e));
      }
    };
    keyListener = new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e) {
        events.add(() -> RootContainer.this.keyTyped(e));
      }

      @Override
      public void keyPressed(KeyEvent e) {
        events.add(() -> RootContainer.this.keyPressed(e));
      }

      @Override
      public void keyReleased(KeyEvent e) {
        events.add(() -> RootContainer.this.keyReleased(e));
      }
    };
  }

  public final void activate() {
    gameCanvas.addMouseListener(mouseListener);
    gameCanvas.addMouseMotionListener(mouseMotionListener);
    gameCanvas.addMouseWheelListener(mouseWheelListener);
    gameCanvas.addKeyListener(keyListener);
  }

  public final void dispose() {
    gameCanvas.removeMouseListener(mouseListener);
    gameCanvas.removeMouseMotionListener(mouseMotionListener);
    gameCanvas.removeMouseWheelListener(mouseWheelListener);
    gameCanvas.removeKeyListener(keyListener);
    mouseExited(null);
  }

  @Override
  public void step(long timeDeltaMs) {
    while (true) {
      Runnable event = events.poll();
      if (event == null) {
        break;
      }

      event.run();
    }

    super.step(timeDeltaMs);
  }

  @Override
  public final Bounds getBounds() {
    throw new AssertionError(
        "should never call RootContainer getBounds(), always computeAbsoluteBounds()");
  }

  @Override
  public final Rectangle computeAbsoluteBounds() {
    return gameCanvas.getBounds();
  }

  @Override
  protected final boolean hasFocus() {
    // Root container is always focused since it's the only one.
    return true;
  }

  @Override
  public final void takeFocus() {
    // Nothing needs to be done since we always have focus.
  }
}
