package edu.unc.petri.policy;

import java.util.Map;
import java.util.Random;

/**
 * The PriorityPolicy class implements a policy for choosing the next transition. It is designed to
 * select transitions based on their priority. This class implements the IPolicy interface.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-05-08
 */
public class PriorityPolicy implements PolicyInterface {

  private final Map<Integer, Integer> weightTransitions;
  private final Random random;

  /**
   * Constructor that receives a HashMap with the weights referring to the transitions, referencing
   * the "key" as the transition to which you want to assign its weight (the weight being the
   * "value" of said key).
   *
   * @param weightTransitions a Map where the key is the transition number and the value is its
   *     weight
   */
  public PriorityPolicy(Map<Integer, Integer> weightTransitions) {
    random = new Random();
    this.weightTransitions = weightTransitions;
  }

  /**
   * Chooses a transition based on its priority.
   *
   * @param n an array of integers representing the enabled transitions. Each element contains the
   *     number of the enabled transition.
   * @return the number of the chosen transition
   */
  @Override
  public int choose(int[] n) {
    int initialMaxValue = weightTransitions.get(n[0]);
    int index = 0;
    boolean randomFlag = true;

    if (n == null || n.length == 0) {
      throw new IllegalArgumentException("The given array is null or empty");
    } else {
      for (int i = 0; i < n.length; i++) {
        int newValue = weightTransitions.get(n[i]);
        if (initialMaxValue < newValue) {
          initialMaxValue = newValue;
          index = i;
          randomFlag = false;
        }
      }
      if (randomFlag) {
        index = random.nextInt(n.length);
      }
    }
    return n[index];
  }
}
