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

  /** EnableVector instance to manage enabled transitions. */
  private EnableVector enableVector;

  /**
   * Constructs a TimeRangeMatrix from a given config file. The matrix is initialized with the
   * number of transitions in the path.
   *
   * @param timeRangeMatrix A 2D long array representing the time ranges for each transition.
   */
  public TimeRangeMatrix(long[][] timeRangeMatrix, EnableVector enableVector) {
    if (timeRangeMatrix == null || timeRangeMatrix.length == 0) {
      throw new IllegalArgumentException("Time range matrix cannot be null or empty");
    }

    if (enableVector == null) {
      throw new IllegalArgumentException("EnableVector cannot be null");
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

    if (timeRangeMatrix.length != enableVector.getEnableVector().length) {
      throw new IllegalArgumentException(
          "Time range matrix size must match the number of transitions in EnableVector");
    }

    this.timeRangeMatrix = timeRangeMatrix;
    this.enableVector = enableVector;
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
   * @return true if the transition is inside the time range, false otherwise.
   */
  public boolean isInsideTimeRange(int transition) {
    if (transition < 0 || transition >= timeRangeMatrix.length) {
      throw new IndexOutOfBoundsException("Invalid transition index: " + transition);
    }

    long currentTime = System.currentTimeMillis();
    long startTime = enableVector.getEnableTransitionTime(transition);
    long timePassed = currentTime - startTime;

    long startRange = timeRangeMatrix[transition][0];
    long endRange = timeRangeMatrix[transition][1];

    // Return true for instantaneous transitions [0,0]
    if (startRange == 0 && endRange == 0) {
      return true;
    }

    return timePassed >= startRange && timePassed <= endRange;
  }
}
