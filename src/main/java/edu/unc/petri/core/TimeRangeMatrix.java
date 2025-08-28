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
  /** Matrix of time ranges for each transition. */
  private final long[][] timeRangeMatrix;

  /**
   * Constructs a TimeRangeMatrix from a given config file. The matrix is initialized with the
   * number of transitions in the path.
   *
   * @param timeRangeMatrix A 2D long array representing the time ranges for each transition.
   */
  public TimeRangeMatrix(long[][] timeRangeMatrix) {
    if (timeRangeMatrix == null || timeRangeMatrix.length == 0) {
      throw new IllegalArgumentException("Time range matrix cannot be null or empty");
    }

    for (long[] range : timeRangeMatrix) {
      if (range.length != 2) {
        throw new IllegalArgumentException("Each time range must have exactly two elements");
      }
      if (range[0] < 0 || range[1] < 0) {
        throw new IllegalArgumentException("Time ranges must be non-negative");
      }
      if (range[0] > range[1]) {
        throw new IllegalArgumentException("Start time must be less than or equal to end time");
      }
    }

    this.timeRangeMatrix = timeRangeMatrix;
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
   * @throws IndexOutOfBoundsException if the transition index is out of bounds.
   */
  public boolean isInsideTimeRange(int transition, long enabledTime) {
    if (transition < 0 || transition >= timeRangeMatrix.length) {
      throw new IndexOutOfBoundsException("Invalid transition index: " + transition);
    }

    long currentTime = System.currentTimeMillis();
    long timePassed = currentTime - enabledTime;

    long startRange = timeRangeMatrix[transition][0];
    long endRange = timeRangeMatrix[transition][1];

    // Return true for instantaneous transitions [0,0]
    if (startRange == 0 && endRange == 0) {
      return true;
    }

    return timePassed >= startRange && timePassed <= endRange;
  }

  /**
   * Checks if the current system time is before the start time of the specified transition.
   *
   * @param transition the index of the transition in the timeRangeMatrix
   * @return true if the current time is before the start time of the transition, false otherwise
   * @throws IndexOutOfBoundsException if the transition index is out of bounds
   */
  public boolean isBeforeTimeRange(int transition, long enabledTime) {
    if (transition < 0 || transition >= timeRangeMatrix.length) {
      throw new IndexOutOfBoundsException("Invalid transition index: " + transition);
    }
    // Assuming timeRangeMatrix is a 2D array with [start, end] times for each transition
    long startTime = timeRangeMatrix[transition][0];
    long currentTime = System.currentTimeMillis();
    long timePassed = currentTime - enabledTime;

    return timePassed < startTime;
  }

  /**
   * Calculates the remaining sleep time before a transition can fire.
   *
   * @param transition the index of the transition
   * @param enabledTime the time when the transition was enabled
   * @return the sleep time in milliseconds before the transition can fire, or 0 if already eligible
   * @throws IndexOutOfBoundsException if the transition index is out of bounds
   */
  public long getSleepTimeToFire(int transition, long enabledTime) {
    if (transition < 0 || transition >= timeRangeMatrix.length) {
      throw new IndexOutOfBoundsException("Invalid transition index: " + transition);
    }
    long currentTime = System.currentTimeMillis();
    long timePassed = currentTime - enabledTime;
    long startRange = timeRangeMatrix[transition][0];
    if (timePassed < startRange) {
      return startRange - timePassed; // Time to wait until the start of the range
    } else {
      return 0; // Already within or past the time range
    }
  }
}
