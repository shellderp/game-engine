package shellderp.game.ui;

/**
 * Created by: Mike
 */
public interface Coord {

  /**
   * @param containerLength The width or height (for x or y respectively) of the parent container.
   * @param myLength        If computing x or y, this is the computed width or height repsectively.
   *                        Otherwise this is -1.
   * @return The absolute length in pixels of this coordinate.
   */
  int computeAbsoluteLength(int containerLength, int myLength);

}
