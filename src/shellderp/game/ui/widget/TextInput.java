package shellderp.game.ui.widget;

import shellderp.game.ui.Bounds;
import shellderp.game.ui.Coord;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;

/**
 * TODO: selections with mouse / shift, handling text longer than width of field, ctrl+left/right
 * Created by: Mike
 */
public class TextInput extends Widget {

  private final Bounds bounds;
  private final Font font;
  private final boolean obscureText;
  private final Component cursorOwner;

  private int caret;
  public static final int CARET_DISPLAY_TOGGLE_MS = 500;
  private long caretDisplayTimer = 0;
  private boolean displayCaret = false;
  private boolean mouseDown = false;

  /**
   * Store the text contained in the input box as a builder since it can change often.
   */
  private StringBuilder text = new StringBuilder();

  private Runnable enterAction;

  public TextInput(Coord x, Coord y, Coord width, Font font, boolean obscureText, Component cursorOwner) {
    this.font = font;
    this.obscureText = obscureText;
    this.cursorOwner = cursorOwner;
    this.bounds = new Bounds(x, y, width,
        (containerLength, myLength) -> Bounds.fontHeight(font));
  }

  @Override
  public Bounds getBounds() {
    return bounds;
  }

  @Override
  protected boolean wantsFocus() {
    return true;
  }

  public void setEnterAction(Runnable enterAction) {
    this.enterAction = enterAction;
  }

  public String getText() {
    return text.toString();
  }

  public void setText(String newText) {
    text.replace(0, text.length(), newText);
    caret = newText.length();
  }

  private void forceShowCaret() {
    caretDisplayTimer = 0;
    displayCaret = true;
  }

  private String getDisplayText() {
    if (obscureText) {
      final StringBuilder builder = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
        builder.append("*");
      }
      return builder.toString();
    } else {
      return text.toString();
    }
  }

  @Override
  public void step(long timeDeltaMs) {
    if (hasFocus()) {
      caretDisplayTimer += timeDeltaMs;
      if (caretDisplayTimer > CARET_DISPLAY_TOGGLE_MS) {
        // Subtract instead of setting to 0, since we may have extra time.
        caretDisplayTimer -= CARET_DISPLAY_TOGGLE_MS;
        displayCaret = !displayCaret;
      }
    } else {
      displayCaret = false;
    }
  }

  @Override
  public void render(Graphics2D graphics) {
    graphics.setFont(font);

    final FontMetrics fm = graphics.getFontMetrics();
    final Rectangle r = new Rectangle(computeAbsoluteBounds().getSize());

    graphics.setColor(Color.WHITE);
    graphics.fill(r);
    graphics.setColor(Color.BLACK);
    graphics.draw(r);

    String displayText = getDisplayText();
    graphics.drawString(displayText, 1, fm.getAscent());

    if (displayCaret) {
      int caretX = fm.stringWidth(displayText.substring(0, caret)) + 2;
      graphics.drawLine(caretX, 2, caretX, fm.getHeight());
    }
  }

  @Override
  public final void mousePressed(MouseEvent e) {
    if (!SwingUtilities.isLeftMouseButton(e)) {
      return;
    }

    mouseDown = true;

    if (!hasFocus()) {
      takeFocus();
      forceShowCaret();
    }

    caret = getCaretForMouse(e.getX());
    forceShowCaret();
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (!SwingUtilities.isLeftMouseButton(e)) {
      return;
    }

    mouseDown = false;
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (mouseDown) {
      caret = getCaretForMouse(e.getX());
    }
  }

  private int getCaretForMouse(int x) {
    String displayText = getDisplayText();
    for (int i = displayText.length(); i >= 0; i--) {
      if (x > Bounds.forString(font, displayText.substring(0, i)).width) {
        return i;
      }
    }
    return 0;
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    cursorOwner.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
  }

  @Override
  public void mouseExited(MouseEvent e) {
    cursorOwner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  @Override
  public final void keyPressed(KeyEvent e) {
    final int key = e.getKeyCode();
    if (e.isControlDown()) {
      switch (key) {
        case KeyEvent.VK_C:
          if (!obscureText) {
            StringSelection ss = new StringSelection(text.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
          }
          break;
        case KeyEvent.VK_V:
          Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
          if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
              String clipboardText = (String) t.getTransferData(DataFlavor.stringFlavor);
              text.insert(caret, clipboardText);
              caret += clipboardText.length();
            } catch (UnsupportedFlavorException | IOException ex) {
              ex.printStackTrace();
            }
          }
          break;
        case KeyEvent.VK_BACK_SPACE:
          int start = Math.max(getDisplayText().lastIndexOf(" ", caret - 1), 0);
          text.delete(start, caret);
          caret = start;
          forceShowCaret();
          break;
        case KeyEvent.VK_LEFT:
          // TODO
          break;
        case KeyEvent.VK_RIGHT:
          // TODO
          break;
      }
    } else {
      if (key == KeyEvent.VK_BACK_SPACE) {
        if (text.length() > 0 && caret != 0) {
          text.deleteCharAt(--caret);
          forceShowCaret();
        }
      } else if (key == KeyEvent.VK_DELETE) {
        if (text.length() > caret) {
          text.deleteCharAt(caret);
          forceShowCaret();
        }
      } else if (key == KeyEvent.VK_LEFT) {
        if (e.isShiftDown()) {
          // TODO selection
        }
        if (caret > 0) {
          caret--;
        }
        forceShowCaret();
      } else if (key == KeyEvent.VK_RIGHT) {
        if (e.isShiftDown()) {
          // TODO selection
        }
        if (caret < text.length()) {
          caret++;
        }
        forceShowCaret();
      } else if (key == KeyEvent.VK_HOME) {
        caret = 0;
        forceShowCaret();
      } else if (key == KeyEvent.VK_END) {
        caret = text.length();
        forceShowCaret();
      } else if (key == KeyEvent.VK_TAB) {
        if (e.isShiftDown()) {
          getParent().focusPrevious();
        } else {
          getParent().focusNext();
        }
      } else if (key == KeyEvent.VK_ENTER) {
        enterAction.run();
      }
    }
  }

  @Override
  public final void keyTyped(KeyEvent e) {
    final char keyChar = e.getKeyChar();
    if (keyChar != KeyEvent.VK_ESCAPE && keyChar != KeyEvent.VK_BACK_SPACE &&
        keyChar != KeyEvent.VK_DELETE && keyChar != KeyEvent.VK_TAB && keyChar != KeyEvent.VK_ENTER &&
        !e.isControlDown() && !e.isAltDown() && !e.isMetaDown()) {
      text.insert(caret++, keyChar);
    }
  }
}
