package edu.unc.petri.exceptions;

/**
 * Exception thrown when a required transition time has not yet been reached. Indicates how long to
 * sleep before retrying.
 */
public class TransitionTimeNotReachedException extends Exception {

  /** The amount of time (in nanoseconds) to sleep before retrying. */
  private final long sleepTimeNanos;

  /**
   * Constructs a new TransitionTimeNotReachedException with the specified sleep time.
   *
   * @param sleepTimeNanos the time in nanoseconds to sleep before retrying
   */
  public TransitionTimeNotReachedException(long sleepTimeNanos) {
    super();
    this.sleepTimeNanos = sleepTimeNanos;
  }

  /**
   * Returns the sleep time in nanoseconds.
   *
   * @return the sleep time in nanoseconds
   */
  public long getSleepTimeNanos() {
    return sleepTimeNanos;
  }
}
