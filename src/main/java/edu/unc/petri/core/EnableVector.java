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

  /** The id of the threads that are waiting for each transition. -1 means no thread is waiting. */
  private long[] waitingThreadsIds;

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
    this.waitingThreadsIds = new long[numberOfTransitions];

    for (int i = 0; i < numberOfTransitions; i++) {
      tokenEnabledTransitions[i] = false;
      transitionTokenEnablementTimes[i] = 0;
      waitingThreadsIds[i] = -1L;
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
  public boolean[] getTokenEnabledTransitions() {
    // Return a defensive copy to avoid external mutation of internal state
    return tokenEnabledTransitions.clone();
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
      if (isThereThreadWaitingForTransition(transitionIndex)) {
        if (getWaitingThreadId(transitionIndex) == Thread.currentThread().getId()) {
          clearWaitingThreadId(transitionIndex);
        }
      }
      return false;
    }

    long currentTime = System.nanoTime();

    // If the transition is token-wise enabled, check the time range
    boolean isTimeEnabled =
        timeRangeMatrix.isInsideTimeRange(
            transitionIndex, transitionTokenEnablementTimes[transitionIndex], currentTime);

    if (!isTimeEnabled) {
      if (timeRangeMatrix.isBeforeTimeRange(
          transitionIndex, transitionTokenEnablementTimes[transitionIndex], currentTime)) {
        if (!isThereThreadWaitingForTransition(transitionIndex)) {
          waitingThreadsIds[transitionIndex] =
              Thread.currentThread()
                  .getId(); // if there is not a thread waiting for the transition, set the current
          // thread as waiting
        } else if (getWaitingThreadId(transitionIndex) != Thread.currentThread().getId()) {
          return false; // Another thread is already waiting for this transition
        }

        long sleepNanos =
            timeRangeMatrix.getSleepTimeToFire(
                transitionIndex, transitionTokenEnablementTimes[transitionIndex]);
        throw new TransitionTimeNotReachedException(
            sleepNanos); // Transition is not yet enabled time-wise
      } else {
        return false; // Transition has passed its time range
      }
    }

    if (isThereThreadWaitingForTransition(transitionIndex)) {
      if (getWaitingThreadId(transitionIndex) == Thread.currentThread().getId()) {
        // This transition is enabled both in terms of tokens and timing, and the current thread was
        // previously waiting for it to become time enabled
        return true;
      } else {
        return false; // Another thread is waiting for this transition
      }
    }

    // The transition is enabled both by tokens and timing, and no other thread is currently waiting
    return true;
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
   * Checks if any thread is currently waiting for the specified transition.
   *
   * @param transitionIndex the index of the transition to check
   * @return {@code true} if at least one thread is waiting for the transition; {@code false}
   *     otherwise
   * @throws IndexOutOfBoundsException if the transition index is out of bounds
   */
  public boolean isThereThreadWaitingForTransition(int transitionIndex) {
    if (transitionIndex < 0 || transitionIndex >= waitingThreadsIds.length) {
      throw new IndexOutOfBoundsException("Transition index out of bounds");
    }

    return waitingThreadsIds[transitionIndex] != -1L;
  }

  /**
   * Returns the ID of the thread that is currently waiting for the specified transition.
   *
   * @param transitionIndex the index of the transition to query
   * @return the ID of the waiting thread for the given transition
   * @throws IndexOutOfBoundsException if the transition index is out of bounds
   */
  public long getWaitingThreadId(int transitionIndex) {
    if (transitionIndex < 0 || transitionIndex >= waitingThreadsIds.length) {
      throw new IndexOutOfBoundsException("Transition index out of bounds");
    }

    return waitingThreadsIds[transitionIndex];
  }

  /**
   * Clears the waiting thread ID for the specified transition index.
   *
   * <p>Sets the thread ID at the given index in the {@code waitingThreadsIds} array to {@code -1L},
   * indicating that no thread is waiting for this transition.
   *
   * @param transitionIndex the index of the transition whose waiting thread ID should be cleared
   * @throws IndexOutOfBoundsException if {@code transitionIndex} is less than 0 or greater than or
   *     equal to the length of {@code waitingThreadsIds}
   */
  public void clearWaitingThreadId(int transitionIndex) {
    if (transitionIndex < 0 || transitionIndex >= waitingThreadsIds.length) {
      throw new IndexOutOfBoundsException("Transition index out of bounds");
    }
    if (waitingThreadsIds[transitionIndex] != Thread.currentThread().getId()) {
      throw new IllegalStateException("Current thread is not the one waiting for this transition");
    }

    waitingThreadsIds[transitionIndex] = -1L;
  }

  /**
   * Resets all waiting thread IDs.
   *
   * <p>This method iterates through the {@code waitingThreadsIds} array and sets each element to
   * {@code -1L}, effectively clearing any thread IDs that may have been stored. After calling this
   * method, no thread will be marked as waiting for any transition.
   *
   * @throws NullPointerException if {@code waitingThreadsIds} is {@code null}
   */
  public void resetWaitingThreads() {
    for (int i = 0; i < waitingThreadsIds.length; i++) {
      waitingThreadsIds[i] = -1L;
    }
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
