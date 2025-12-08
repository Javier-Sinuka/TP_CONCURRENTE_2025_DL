package edu.unc.petri.policy;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * ProbabilisticPriorityPolicy chooses the next transition using configured firing probabilities.
 *
 * <p>Given a list of candidate transitions, it normalizes their configured probabilities and
 * performs a weighted random selection. This preserves non-determinism while allowing explicit bias
 * toward specific transitions.
 */
public class ProbabilisticPriorityPolicy implements PolicyInterface {

  /** Configured firing probabilities per transition. */
  private final Map<Integer, Integer> transitionProbabilities;

  /** Random number generator for weighted selection. */
  private final Random random;

  /**
   * Creates a policy using the provided transition probabilities.
   *
   * @param transitionProbabilities map from transition id to its configured probability weight
   */
  public ProbabilisticPriorityPolicy(Map<Integer, Integer> transitionProbabilities) {
    this(transitionProbabilities, new Random());
  }

  /**
   * Creates a policy using the provided transition probabilities and random source.
   *
   * @param transitionProbabilities map from transition id to its configured probability weight
   * @param random random generator to use
   */
  public ProbabilisticPriorityPolicy(Map<Integer, Integer> transitionProbabilities, Random random) {
    if (transitionProbabilities == null) {
      throw new IllegalArgumentException("transitionProbabilities map cannot be null");
    }
    if (transitionProbabilities.isEmpty()) {
      throw new IllegalArgumentException("transitionProbabilities map cannot be empty");
    }
    if (random == null) {
      throw new IllegalArgumentException("Random instance cannot be null");
    }
    this.transitionProbabilities = transitionProbabilities;
    this.random = random;
  }

  /**
   * Selects a transition from the candidates using weighted random choice.
   *
   * @param transitions enabled transitions that have a waiting thread
   * @return selected transition id
   */
  @Override
  public int choose(List<Integer> transitions) {
    if (transitions == null) {
      throw new IllegalArgumentException("The given list of transitions is null");
    }
    if (transitions.isEmpty()) {
      throw new IllegalArgumentException("The given list of transitions is empty");
    }
    if (transitions.size() == 1) {
      return transitions.get(0); // Only one option; no weighting needed.
    }

    double conflictTotal = 0.0;
    List<Integer> weightedCandidates = new java.util.ArrayList<>();
    List<Integer> nonConflictCandidates = new java.util.ArrayList<>();
    for (Integer t : transitions) {
      Integer weight = transitionProbabilities.get(t);
      if (weight != null) { // Only conflict transitions are expected to be configured
        if (weight <= 0) {
          throw new IllegalArgumentException(
              "Transition " + t + " has a non-positive probability weight");
        }
        weightedCandidates.add(t);
        conflictTotal += weight;
      } else {
        nonConflictCandidates.add(t);
      }
    }

    // If no configured conflicts are present, pick uniformly at random.
    if (weightedCandidates.isEmpty()) {
      return transitions.get(random.nextInt(transitions.size()));
    }

    // Normalize over present conflict transitions only; non-conflicts do not participate.
    double r = random.nextDouble() * conflictTotal; // uniform in [0, sum(conflict weights))
    double cumulative = 0.0;
    for (Integer t : weightedCandidates) {
      cumulative += transitionProbabilities.get(t);
      if (r < cumulative) {
        return t;
      }
    }

    // Defensive fallback; logic should always return inside the loop.
    return weightedCandidates.get(weightedCandidates.size() - 1);
  }
}
