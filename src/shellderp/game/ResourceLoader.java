package shellderp.game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by: Mike
 */
public abstract class ResourceLoader {

  private static ResourceLoader defaultResourceLoader;

  public static ResourceLoader global() {
    if (defaultResourceLoader == null) {
      throw new AssertionError("Application did not set up a ResourceLoader");
    }
    return defaultResourceLoader;
  }

  public static void set(ResourceLoader defaultResourceLoader) {
    ResourceLoader.defaultResourceLoader = defaultResourceLoader;
  }

  public abstract InputStream getStream(Resource resource) throws FileNotFoundException;

  public static ResourceLoader fromDirectory(String rootDirectory) {
    return new ResourceLoader() {
      @Override
      public InputStream getStream(Resource resource) throws FileNotFoundException {
        return new FileInputStream(new File(rootDirectory, resource.getPath()));
      }
    };
  }

  public static ResourceLoader fromJar(String rootDirectory) {
    return new ResourceLoader() {
      @Override
      public InputStream getStream(Resource resource) throws FileNotFoundException {
        final InputStream stream = getClass().getResourceAsStream(
            "/" + rootDirectory + "/" + resource.getPath());
        if (stream == null) {
          throw new FileNotFoundException("missing resource file: " + resource.getPath());
        }
        return stream;
      }
    };
  }
}
