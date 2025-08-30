package edu.unc.petri.core;

import edu.unc.petri.exceptions.TransitionTimeNotReachedException;
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
  /**
   * Indicates which transitions are enabled token wise. Each index in the array corresponds to a
   * transition; true means enabled, false means disabled.
   */
  private boolean[] tokenEnabledTransitions;

  /** The times in nanoseconds when each transition was enabled token wise. */
  private long[] transitionTokenEnablementTimes;

  /** The TimeRangeMatrix associated with this EnableVector. */
  private TimeRangeMatrix timeRangeMatrix;

  /**
   * Constructs an EnableVector with a specified number of transitions.
   *
   * @param numberOfTransitions The number of transitions in the Petri net.
   */
  public EnableVector(int numberOfTransitions, TimeRangeMatrix timeRangeMatrix) {
    if (numberOfTransitions <= 0) {
      throw new IllegalArgumentException("Number of transitions must be greater than 0");
    }
    if (timeRangeMatrix == null) {
      throw new IllegalArgumentException("TimeRangeMatrix cannot be null");
    }
    if (timeRangeMatrix.getTimeRangeMatrix().length != numberOfTransitions) {
      throw new IllegalArgumentException(
          "TimeRangeMatrix size must match the number of transitions");
    }

    this.tokenEnabledTransitions = new boolean[numberOfTransitions];
    this.transitionTokenEnablementTimes = new long[numberOfTransitions];

    for (int i = 0; i < numberOfTransitions; i++) {
      tokenEnabledTransitions[i] = false;
      transitionTokenEnablementTimes[i] = 0;
    }

    this.timeRangeMatrix = timeRangeMatrix;
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

    long now = System.nanoTime();

    // Recompute enabled state and preserve/adjust timestamps correctly
    for (int i = 0; i < incidenceMatrix.getTransitions(); i++) {
      boolean isNowEnable =
          checkMarking(
              StateEquationUtils.calculateStateEquation(i, incidenceMatrix, currentMarking));

      if (isNowEnable) {
        if (!tokenEnabledTransitions[i]) {
          // Transition becomes enabled now
          tokenEnabledTransitions[i] = true;
          transitionTokenEnablementTimes[i] = now;
        }
        // If it was already enabled, keep previous timestamp
      } else {
        // Transition is not enabled: clear state and timestamp
        tokenEnabledTransitions[i] = false;
        transitionTokenEnablementTimes[i] = 0L;
      }
    }
  }

  /**
   * Retrieves the current enabled transitions vector.
   *
   * @return The vector of enabled transitions.
   */
  public boolean[] getEnableVector() {
    return tokenEnabledTransitions;
  }

  /**
   * Checks if a specific transition is enabled.
   *
   * @param transitionIndex The index of the transition to check.
   * @return true if the transition is enabled, false otherwise.
   */
  public boolean isTransitionEnabled(int transitionIndex) throws TransitionTimeNotReachedException {
    if (transitionIndex < 0 || transitionIndex >= tokenEnabledTransitions.length) {
      throw new IndexOutOfBoundsException("Transition index out of bounds");
    }

    boolean isTokenEnabled = tokenEnabledTransitions[transitionIndex];

    // If the transition is not token-wise enabled, return false immediately
    if (!isTokenEnabled) {
      return false;
    }

    // If the transition is token-wise enabled, check the time range
    boolean isTimeEnabled =
        timeRangeMatrix.isInsideTimeRange(
            transitionIndex, transitionTokenEnablementTimes[transitionIndex]);

    if (!isTimeEnabled) {
      if (timeRangeMatrix.isBeforeTimeRange(
          transitionIndex, transitionTokenEnablementTimes[transitionIndex])) {
        long sleepNanos =
            timeRangeMatrix.getSleepTimeToFire(
                transitionIndex, transitionTokenEnablementTimes[transitionIndex]);
        throw new TransitionTimeNotReachedException(sleepNanos);
      } else {
        return false; // Transition has passed its time range
      }
    }

    return true; // Transition is both token-wise and time-wise enabled
  }

  /**
   * Retrieves the time when a specific transition was enabled.
   *
   * @param transitionIndex The index of the transition.
   * @return The time when the transition was enabled.
   */
  public long getEnableTransitionTime(int transitionIndex) {
    if (transitionIndex < 0 || transitionIndex >= transitionTokenEnablementTimes.length) {
      throw new IndexOutOfBoundsException("Transition index out of bounds");
    }

    return transitionTokenEnablementTimes[transitionIndex];
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
