package shellderp.game.ui;

import shellderp.game.GameStep;

import java.awt.Color;
import java.util.function.BooleanSupplier;

/**
 * Implements fading (interpolating) between two colors over a given time scale.
 * <p>
 * Created by: Mike
 */
public class TimedColorFade implements GameStep {
  private final Color normalColor;
  private final Color fadeToColor;
  private final int fadeMaxTimeMs;
  private final BooleanSupplier whenToFade;

  private int fadeTimerMs = 0;
  private Color currentColor;

  public TimedColorFade(Color normalColor, Color fadeToColor, int fadeMaxTimeMs,
      BooleanSupplier whenToFade) {
    this.normalColor = normalColor;
    this.fadeToColor = fadeToColor;
    this.fadeMaxTimeMs = fadeMaxTimeMs;
    this.whenToFade = whenToFade;

    currentColor = normalColor;
  }

  public Color getCurrentColor() {
    return currentColor;
  }

  @Override
  public void step(long timeDeltaMs) {
    if (whenToFade.getAsBoolean()) {
      if (fadeTimerMs < fadeMaxTimeMs) {
        fadeTimerMs = Math.min(fadeMaxTimeMs, fadeTimerMs + (int) timeDeltaMs);
        computeColor();
      }
    } else if (fadeTimerMs > 0) {
      fadeTimerMs = Math.max(0, fadeTimerMs - (int) timeDeltaMs);
      computeColor();
    }
  }

  private void computeColor() {
    final float[] colorComponents = normalColor.getRGBColorComponents(null);
    final float[] targetComponents = fadeToColor.getRGBColorComponents(null);
    final float fadePercent = (float) fadeTimerMs / fadeMaxTimeMs;
    for (int i = 0; i < colorComponents.length; i++) {
      float diff = (targetComponents[i] - colorComponents[i]);
      colorComponents[i] += diff * fadePercent;
    }
    float alpha = normalColor.getAlpha() / 255f;
    float alphaDiff = fadeToColor.getAlpha() / 255f - alpha;
    alpha += alphaDiff * fadePercent;
    currentColor = new Color(normalColor.getColorSpace(), colorComponents, alpha);
  }
}
