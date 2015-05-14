package shellderp.game;

import java.util.logging.Logger;

/**
 * Note: this "mixin" design is not ideal because we expose a public logger() method, but it is convenient
 * enough to warrant it. Having a function call is slower than a static field lookup, but the actual stream
 * logging will take far longer.
 * <p>
 * Created by: Mike
 */
public interface Loggable {
    default Logger logger() {
        return Logger.getLogger(getClass().getName());
    }
}
