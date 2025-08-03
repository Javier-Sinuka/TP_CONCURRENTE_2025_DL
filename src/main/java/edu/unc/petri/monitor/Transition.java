package edu.unc.petri.monitor;

import java.util.concurrent.Semaphore;

public class Transition {
  private final int PERMITS = 0;
  private int transitionNumber;
  private final Semaphore condition;

  public Transition() {
    condition = new Semaphore(PERMITS);
  }

  public int getTransitionNumber() {
    return transitionNumber;
  }

  public void setTransitionNumber(int transitionNumber) {
    this.transitionNumber = transitionNumber;
  }

  public Semaphore getCondition() {
    return condition;
  }
}
