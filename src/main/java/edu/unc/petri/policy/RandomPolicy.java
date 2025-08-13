package edu.unc.petri.policy;

import java.util.Random;

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
   * An instance of {@link Random} used to generate random values for policy decisions.
   * This random generator is initialized once and used throughout the lifetime of the policy.
   */

   private final Random random = new Random();
  /**
   * Chooses a transition to fire based on the enabled transitions with a random selection.
   *
   * @param n an array of integers representing the enabled transitions. Each element contains the
   *     number of the enabled transition.
   * @return the number of the chosen transition
   */
  
  @Override
  public int choose(int[] n) {
   
    if (n == null || n.length == 0) {
      throw new IllegalArgumentException("The parameter is null or empty");
    }
    int index = random.nextInt(n.length);
    return n[index];
   
  }
}
