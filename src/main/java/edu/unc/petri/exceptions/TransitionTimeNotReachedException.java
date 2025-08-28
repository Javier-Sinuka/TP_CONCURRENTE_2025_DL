package edu.unc.petri.core;

/**
 * Exception thrown when a required transition time has not yet been reached. Indicates how long to
 * sleep before retrying.
 */
public class TransitionTimeNotReachedException extends Exception {

  /** The amount of time (in milliseconds) to sleep before retrying. */
  private final long sleepTime;

  /**
   * Constructs a new TransitionTimeNotReachedException with the specified sleep time.
   *
   * @param sleepTime the time in milliseconds to sleep before retrying
   */
  public TransitionTimeNotReachedException(long sleepTime) {
    super();
    this.sleepTime = sleepTime;
  }

  /**
   * Returns the sleep time in milliseconds.
   *
   * @return the sleep time in milliseconds
   */
  public long getSleepTime() {
    return sleepTime;
  }
}
