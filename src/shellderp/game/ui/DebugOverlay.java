package shellderp.game.ui;

import shellderp.game.Timer;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Displays debug messages on the screen.
 * This is a singleton so that any class can append a debug message.
 * <p>
 * Created by: Mike
 */
public class DebugOverlay implements Renderable {
  private final Color textColor;

  private final Timer secondTimer = new Timer();
  private int frames;
  private int lastFps;

  private List<DebugString> debugStrings = new ArrayList<>();
  private LinkedHashMap<String, String[]> staticDebugStrings = new LinkedHashMap<>();

  private static class DebugString {
    final String line;
    int framesLeft;

    public DebugString(String line, int numFramesToDisplay) {
      this.line = line;
      this.framesLeft = numFramesToDisplay;
    }
  }

  private DebugOverlay(Color textColor) {
    this.textColor = textColor;
  }

  private static DebugOverlay singleton = null;

  public static DebugOverlay global() {
    if (singleton == null) {
      singleton = new DebugOverlay(Color.WHITE);
    }
    return singleton;
  }

  public void addDebugText(String text) {
    addDebugText(text, 1);
  }

  public void addDebugText(String text, int numFramesToDisplay) {
    if (numFramesToDisplay < 1) {
      throw new IllegalArgumentException("numFramesToDisplay must be >= 1");
    }
    for (String line : text.split("\n")) {
      debugStrings.add(new DebugString(line, numFramesToDisplay));
    }
  }

  public void updateStatic(String key, String text) {
    staticDebugStrings.put(key, text.split("\n"));
  }

  @Override
  public void render(Graphics2D g) {
    if (!secondTimer.isActive()) {
      secondTimer.restart();
    }

    if (secondTimer.hasPassed(1000)) {
      secondTimer.restart();
      lastFps = frames;
      frames = 0;
    } else {
      frames++;
    }

    final AffineTransform originalTransform = g.getTransform();
    final FontMetrics fontMetrics = g.getFontMetrics();

    g.translate(2, fontMetrics.getAscent());
    g.setColor(textColor);
    g.drawString("FPS: " + lastFps, 0, 0);

    staticDebugStrings.forEach((key, text) -> {
      for (String line : text) {
        g.translate(0, fontMetrics.getHeight());
        g.drawString(key + ": " + line, 0, 0);
      }
    });

    for (Iterator<DebugString> iterator = debugStrings.iterator(); iterator.hasNext(); ) {
      DebugString debugString = iterator.next();
      if (debugString.framesLeft-- == 0) {
        iterator.remove();
      } else {
        g.translate(0, fontMetrics.getHeight());
        g.drawString(debugString.line, 0, 0);
      }
    }

    g.setTransform(originalTransform);
  }
}
