package edu.unc.petri.monitor;

import java.util.concurrent.Semaphore;

/**
 * The Transition class is a container of a thread that needs wait for this transition number.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public class Transition {
  /** Transition identifier number. */
  private int transitionNumber;
  /** Place where the thread waits for the condition */
  private final Semaphore condition;

  /**
   * Constructor of the Transition class.
   */
  public Transition() {
    int PERMITS = 0;
    condition = new Semaphore(PERMITS);
  }

  /** Obtains the identifier for this transition.
   *
   * @return Obtains the identifier for this transition.
   */
  public int getTransitionNumber() {
    return transitionNumber;
  }

  /** Set the identifier number of this transition.
   *
   * @param transitionNumber for this transition.
   */
  public void setTransitionNumber(int transitionNumber) {
    this.transitionNumber = transitionNumber;
  }

  /** This method is what the thread uses to wait for the transition.
   *
   * @return an instance of Semaphore class.
   * */
  public Semaphore getCondition() {
    return condition;
  }
}
