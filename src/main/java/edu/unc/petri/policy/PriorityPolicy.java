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
  private final Map<Integer, Integer> transitionsWeights;

  /** Random number generator for tie-breaking. */
  private final Random random;

  /**
   * Constructor that receives a HashMap with the weights referring to the transitions, referencing
   * the "key" as the transition to which you want to assign its weight (the weight being the
   * "value" of said key).
   *
   * @param transitionsWeights a Map where the key is the transition number and the value is its
   *     weight
   */
  public PriorityPolicy(Map<Integer, Integer> transitionsWeights) {
    if (transitionsWeights == null) {
      throw new IllegalArgumentException("The weightTransitions map cannot be null");
    }
    if (transitionsWeights.isEmpty()) {
      throw new IllegalArgumentException("The weightTransitions map cannot be empty");
    }
    random = new Random();
    this.transitionsWeights = transitionsWeights;
  }

  /**
   * Selects a transition from the provided list of enabled transitions using their priority
   * weights.
   *
   * @param transitions a list of integers, each representing the identifier of an enabled
   *     transition. The selection is influenced by the priority weights associated with each
   *     transition.
   * @return the identifier of the selected transition
   */
  @Override
  public int choose(List<Integer> transitions) {
    if (transitions == null) {
      throw new IllegalArgumentException("The given list of transitions is null");
    }
    if (transitions.isEmpty()) {
      throw new IllegalArgumentException("The given list of transitions is empty");
    }

    int maxTransitionWeight = Integer.MIN_VALUE;
    List<Integer> bestTransitions = new ArrayList<>();

    for (Integer transition : transitions) {
      Integer transitionWeight = transitionsWeights.get(transition);
      if (transitionWeight == null) {
        throw new IllegalArgumentException(
            "Transition " + transition + " does not exist in the weight map");
      }

      if (transitionWeight > maxTransitionWeight) {
        maxTransitionWeight = transitionWeight;
        bestTransitions.clear(); // We have a new best, clear previous bests
        bestTransitions.add(transition); // Add the new best transition
      } else if (transitionWeight == maxTransitionWeight) {
        bestTransitions.add(transition); // We have a tie, add to the list
      }
    }

    // Tie-break only among the best transitions
    if (bestTransitions.size() == 1) {
      return bestTransitions.get(0);
    }

    return bestTransitions.get(
        random.nextInt(bestTransitions.size())); // Randomly select among the best
  }
}
