package edu.youruni.petri.policy;

/**
 * RandomPolicy is a policy that randomly chooses an enabled transition. It implements the IPolicy
 * interface.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public class RandomPolicy implements PolicyInterface {

  /**
   * Chooses a transition to fire based on the enabled transitions with a random selection.
   *
   * @param n an array of integers representing the enabled transitions. Each element contains the
   *     number of the enabled transition.
   * @return the number of the chosen transition
   */
  @Override
  public int choose(int[] n) {
    // TODO: Implement the logic to randomly select an enabled transition
    return 0;
  }
}
