package shellderp.game.ui;

import shellderp.game.Resource;
import shellderp.game.ResourceLoader;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by: Mike
 */
public final class Sprite implements Renderable {

  public enum RenderMode {
    SINGLE,
    // To use the TILED mode, note it is necessary to set the clip on the Graphics2D since this is
    // used to compute the size of the tiling.
    TILED,
    TILED_CENTERED // Same as tiled unless the view is too small, then try to centre in both axes.
    ;
  }

  private final Image image;

  private final RenderMode renderMode;

  private final int width, height;

  static {
    ImageIO.setUseCache(false);
  }

  private static Sprite missingSprite;

  public static Sprite load(Resource resource, RenderMode renderMode) {
    try (InputStream stream = ResourceLoader.global().getStream(resource)) {
      return new Sprite(ImageIO.read(stream), renderMode);
    } catch (IOException e) {
      System.err.println(e);

      if (missingSprite == null) {
        final BufferedImage image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        g.setColor(new Color(255, 0, 220));
        g.fillRect(0, 0, 30, 30);
        g.fillRect(30, 30, 30, 30);
        g.setColor(Color.BLACK);
        g.fillRect(30, 0, 30, 30);
        g.fillRect(0, 30, 30, 30);
        missingSprite = Sprite.fromImage(image, RenderMode.TILED_CENTERED);
      }
      return missingSprite;
    }
  }

  public static Sprite fromImage(final BufferedImage sourceImage, RenderMode renderMode) {
    return new Sprite(sourceImage, renderMode);
  }

  private Sprite(final BufferedImage sourceImage, final RenderMode renderMode) {
    GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().
        getDefaultScreenDevice().getDefaultConfiguration();
    this.image = gc.createCompatibleImage(sourceImage.getWidth(),
        sourceImage.getHeight(),
        Transparency.TRANSLUCENT);
    this.image.getGraphics().drawImage(sourceImage, 0, 0, null);

    this.renderMode = renderMode;
    this.width = sourceImage.getWidth();
    this.height = sourceImage.getHeight();
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  @Override
  public void render(Graphics2D graphics) {
    final Rectangle bounds = graphics.getClipBounds();

    if (renderMode == RenderMode.SINGLE) {
      graphics.drawImage(image, 0, 0, null);
    } else if (renderMode == RenderMode.TILED) {
      renderTiled(graphics, bounds, 0, 0, width, height);
    } else if (renderMode == RenderMode.TILED_CENTERED) {
      int sx = 0, sy = 0;
      if (bounds.width < width) {
        sx = (width - bounds.width) / 2;
      }
      if (bounds.height < height) {
        sy = (height - bounds.height) / 2;
      }
      renderTiled(graphics, bounds, sx, sy, width - 2 * sx, height - 2 * sy);
    }
  }

  public void dispose() {
    image.flush();
  }

  private void renderTiled(Graphics2D graphics, Rectangle bounds, int sx, int sy, int sw, int sh) {
    for (int x = 0; x < bounds.width; x += sw) {
      for (int y = 0; y < bounds.height; y += sh) {
        graphics.drawImage(image, x, y, x + sw, y + sh, sx, sy, sx + sw, sy + sh, null);
      }
    }
  }
}
