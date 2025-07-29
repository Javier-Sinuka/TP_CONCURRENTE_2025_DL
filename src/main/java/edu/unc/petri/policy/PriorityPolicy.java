package edu.youruni.petri.policy;

/**
 * The PriorityPolicy class implements a policy for choosing the next transition. It is designed to
 * select transitions based on their priority. This class implements the IPolicy interface.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public class PriorityPolicy implements PolicyInterface {

  /**
   * Chooses a transition based on its priority.
   *
   * @param n an array of integers representing the enabled transitions. Each element contains the
   *     number of the enabled transition.
   * @return the number of the chosen transition
   */
  @Override
  public int choose(int[] n) {
    // TODO: Implement logic to select a transition based on its priority
    return 0;
  }
}
