package edu.youruni.petri.core;

/**
 * The EnableVector class represents a vector of enabled transitions in a Petri net simulation. It
 * is used to manage the state of transitions, indicating which transitions are enabled and when
 * they were enabled.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public class EnableVector {
  /** The vector of enabled transitions, where each index corresponds to a transition. */
  private boolean[] enabledTransitions;

  /** The times when each transition was enabled, corresponding to the enabledTransitions vector. */
  private long[] enabledTransitionTimes;

  /**
   * Constructs an EnableVector with a specified number of transitions.
   *
   * @param numberOfTransitions The number of transitions in the Petri net.
   */
  public EnableVector(int numberOfTransitions) {
    this.enabledTransitions = new boolean[numberOfTransitions];
    this.enabledTransitionTimes = new long[numberOfTransitions];
  }

  /**
   * Sets the enabled transitions vector to a new vector.
   *
   * @param newEnabledTransitions The new vector of enabled transitions.
   */
  void setEnableTransitionVector(boolean[] newEnabledTransitions) {
    // TODO: Implement logic to update the enabled transitions vector
    this.enabledTransitions = newEnabledTransitions;
  }

  /**
   * Enables a specific transition at a given time.
   *
   * @param transitionIndex The index of the transition to enable.
   * @param time The time at which the transition is enabled.
   */
  void enableTransition(int transitionIndex, long time) {
    // TODO: Add logic to enable the transition and update the enabledTransitionTimes
  }

  /**
   * Retrieves the current enabled transitions vector.
   *
   * @return The vector of enabled transitions.
   */
  public boolean[] getEnableVector() {
    return enabledTransitions;
  }

  /**
   * Checks if a specific transition is enabled.
   *
   * @param transitionIndex The index of the transition to check.
   * @return true if the transition is enabled, false otherwise.
   */
  public boolean isTransitionEnabled(int transitionIndex) {
    // TODO: Implement logic to check if a specific transition is enabled
    return enabledTransitions[transitionIndex];
  }

  /**
   * Retrieves the time when a specific transition was enabled.
   *
   * @param transitionIndex The index of the transition.
   * @return The time when the transition was enabled.
   */
  public long getEnableTransitionTime(int transitionIndex) {
    // TODO: Add logic to handle cases where the transition index is invalid
    return enabledTransitionTimes[transitionIndex];
  }
}
