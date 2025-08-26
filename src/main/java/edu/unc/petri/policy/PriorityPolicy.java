package edu.unc.petri.policy;

import java.util.ArrayList;
import java.util.List;
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

  /** A map that associates each transition with its corresponding weight (priority). */
  private final Map<Integer, Integer> weightTransitions;

  /** Random number generator for tie-breaking. */
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
    if (weightTransitions == null || weightTransitions.isEmpty()) {
      throw new IllegalArgumentException("The weightTransitions map cannot be null or empty");
    }
    random = new Random();
    this.weightTransitions = weightTransitions;
  }

  /**
   * Selects a transition based on its associated priority weights.
   *
   * @param transitions a list of integers representing the enabled transitions. Each element
   *     corresponds to the identifier of an enabled transition.
   * @return the identifier of the chosen transition
   */
  @Override
  public int choose(ArrayList<Integer> transitions) {

    if (transitions == null || transitions.size() == 0) {
      throw new IllegalArgumentException("The given array is null or empty");
  public int choose(List<Integer> transitions) {
    }

    int initialMaxValue = weightTransitions.get(transitions.get(0));
    int index = 0;
    boolean randomFlag = true;

    if (transitions == null || transitions.size() == 0) {
      throw new IllegalArgumentException("The given array is null or empty");
    } else {
      for (int i = 0; i < transitions.size(); i++) {
        int newValue = weightTransitions.get(transitions.get(i));
        if (initialMaxValue < newValue) {
          initialMaxValue = newValue;
          index = i;
          randomFlag = false;
        }
      }
      if (randomFlag) {
        index = random.nextInt(transitions.size());
      }
    }
    return transitions.get(index);
  }
}
