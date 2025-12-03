package edu.unc.petri.core;

/**
 * The TimeRangeMatrix class represents a matrix of time ranges for each transition in a Petri net.
 * It is used to determine if a transition is within its defined time range.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public class TimeRangeMatrix {

  /** Matrix of time ranges in nanoseconds for each transition. */
  private final long[][] timeRangeMatrix;

  /** Constant for converting milliseconds to nanoseconds. */
  private static final long MILLIS_TO_NANOS = 1_000_000L;

  /**
   * Constructs a TimeRangeMatrix with the specified time ranges for each transition.
   *
   * <p>Validates that the input matrix is not null or empty, and that each time range contains
   * exactly two non-negative elements where the start time is less than or equal to the end time.
   *
   * <p>Converts the input time ranges from milliseconds to nanoseconds.
   *
   * @param timeRangeMatrix a 2D long array where each sub-array represents a time range in
   *     milliseconds [start, end] for a transition
   * @throws IllegalArgumentException if the matrix is null, empty, contains invalid ranges, or
   *     negative values
   */
  public TimeRangeMatrix(long[][] timeRangeMatrix) {
    if (timeRangeMatrix == null || timeRangeMatrix.length == 0) {
      throw new IllegalArgumentException("Time range matrix cannot be null or empty");
    }

    long[][] nanoTimeRangeMatrix = new long[timeRangeMatrix.length][2];
    for (int i = 0; i < timeRangeMatrix.length; i++) {
      long[] range = timeRangeMatrix[i];
      if (range.length != 2) {
        throw new IllegalArgumentException("Each time range must have exactly two elements");
      }
      if (range[0] < 0 || range[1] < 0) {
        throw new IllegalArgumentException("Time ranges must be non-negative");
      }
      if (range[0] > range[1]) {
        throw new IllegalArgumentException("Start time must be less than or equal to end time");
      }
      // Convert milliseconds to nanoseconds
      nanoTimeRangeMatrix[i][0] = range[0] * MILLIS_TO_NANOS;
      nanoTimeRangeMatrix[i][1] = range[1] * MILLIS_TO_NANOS;
    }

    this.timeRangeMatrix = nanoTimeRangeMatrix;
  }

  /**
   * Retrieves the time range matrix.
   *
   * @return The time range matrix.
   */
  public long[][] getTimeRangeMatrix() {
    return timeRangeMatrix;
  }

  /**
   * Checks if the given transition is inside the time range defined for it.
   *
   * @param transition The transition to check.
   * @param enabledTime The time when the transition was enabled.
   * @return true if the transition is inside the time range, false otherwise.
   */
  public boolean isInsideTimeRange(int transition, long enabledTime, long currentTime) {
    if (transition < 0 || transition >= timeRangeMatrix.length) {
      throw new IndexOutOfBoundsException("Invalid transition index: " + transition);
    }

    long timePassed = currentTime - enabledTime;

    long startRange = timeRangeMatrix[transition][0];
    long endRange = timeRangeMatrix[transition][1];

    // Case 1: Instantaneous transitions [0, 0]
    if (startRange == 0 && endRange == 0) {
      return true;
    }

    // Case 2: Fixed-time transitions [a, a] where a > 0
    if (startRange > 0 && startRange == endRange) {
      // For fixed time, we check if AT LEAST that much time has passed to
      // account for scheduling delays.
      return timePassed >= startRange;
    }

    // Case 3: Normal time intervals [a, b]
    return timePassed >= startRange && timePassed <= endRange;
  }

  /**
   * Checks if the elapsed time is before the start time of the specified transition.
   *
   * @param transition the index of the transition.
   * @param enabledTime The time when the transition was enabled.
   * @return true if the elapsed time is less than the transition's start time.
   */
  public boolean isBeforeTimeRange(int transition, long enabledTime, long currentTime) {
    if (transition < 0 || transition >= timeRangeMatrix.length) {
      throw new IndexOutOfBoundsException("Invalid transition index: " + transition);
    }

    long startRange = timeRangeMatrix[transition][0];
    long timePassed = currentTime - enabledTime;

    return timePassed < startRange;
  }

  /**
   * Calculates the remaining sleep time in nanoseconds before a transition can fire.
   *
   * @param transition the index of the transition.
   * @param enabledTime the time when the transition was enabled.
   * @return the sleep time in nanoseconds, or 0 if already eligible.
   */
  public long getSleepTimeToFire(int transition, long enabledTime) {
    if (transition < 0 || transition >= timeRangeMatrix.length) {
      throw new IndexOutOfBoundsException("Invalid transition index: " + transition);
    }

    long timePassed = System.nanoTime() - enabledTime;
    long startRange = timeRangeMatrix[transition][0];

    if (timePassed < startRange) {
      return startRange - timePassed; // Return precise nanoseconds
    } else {
      return 0; // Already within or past the time range start.
    }
  }
}
