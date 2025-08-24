package edu.unc.petri.policy;

import java.util.ArrayList;
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

  /** A Random instance utilized for generating random values to guide policy decisions. */
  private final Random random = new Random();

  /**
   * Selects a transition to fire based on the enabled transitions using a random selection process.
   *
   * @param transitions a list of integers representing the enabled transitions. Each element
   *     corresponds to an enabled transition identifier.
   * @return the identifier of the chosen transition
   */
  @Override
  public int choose(ArrayList<Integer> transitions) {

    if (transitions == null || transitions.size() == 0) {
      throw new IllegalArgumentException("The parameter is null or empty");
    }

    int index = random.nextInt(transitions.size());

    return transitions.get(index);
  }
}
