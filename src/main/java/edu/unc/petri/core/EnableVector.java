package edu.unc.petri.core;

import edu.unc.petri.util.StateEquationUtils;

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
   * @param incidenceMatrix The incidence matrix
   * @param currentMarking The current marking of the net
   */
  void updateEnableVector(IncidenceMatrix incidenceMatrix, CurrentMarking currentMarking) {
    if (incidenceMatrix == null || currentMarking == null) {
      throw new IllegalArgumentException("The parameter is null");
    }
    if (incidenceMatrix.getPlaces() == 0
        || incidenceMatrix.getTransitions() == 0
        || currentMarking.getMarking().length == 0) {
      throw new IllegalArgumentException("Parameters size cannot be 0");
    }
    for (int i = 0; i < incidenceMatrix.getTransitions(); i++) {
      enabledTransitions[i] = false;
    }
    for (int i = 0; i < incidenceMatrix.getTransitions(); i++) {
      if (checkMarking(
          StateEquationUtils.calculateStateEquation(i, incidenceMatrix, currentMarking))) {
        enabledTransitions[i] = true;
        enabledTransitionTimes[i] = System.currentTimeMillis();
      }
    }
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

    return enabledTransitions[transitionIndex];
  }

  /**
   * Retrieves the time when a specific transition was enabled.
   *
   * @param transitionIndex The index of the transition.
   * @return The time when the transition was enabled.
   */
  public long getEnableTransitionTime(int transitionIndex) {

    return enabledTransitionTimes[transitionIndex];
  }

  /**
   * Checks if the given marking is valid, meaning all places have non-negative tokens.
   *
   * @param marking The marking to check.
   * @return true if the marking is valid, false otherwise.
   */
  private boolean checkMarking(int[] marking) {

    for (int i = 0; i < marking.length; i++) {
      if (marking[i] < 0) {
        return false;
      }
    }
    return true;
  }
}

