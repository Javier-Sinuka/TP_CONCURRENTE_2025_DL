package edu.unc.petri.monitor;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * The ConditionQueues class manages condition queues for transitions in a Petri net. It allows
 * threads to wait for specific transitions and wake them up when the transition is enabled.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public class ConditionQueues {

  /** Condition queues for each transition in the Petri net. */
  private final ArrayList<Semaphore> queues;

  /**
   * Constructor to initialize the condition queues for a given number of transitions.
   *
   * @param transitionsNumber of the petri net model
   */
  public ConditionQueues(int transitionsNumber) {
    if (transitionsNumber <= 0) {
      throw new IllegalArgumentException("Number of transitions must be greater than 0");
    }

    queues = new ArrayList<>();

    for (int i = 0; i < transitionsNumber; i++) {
      queues.add(new Semaphore(0));
    }
  }

  /**
   * Queues a thread into a transition's condition queue.
   *
   * @param transitionNumber the transition for which the thread is queued
   */
  void waitForTransition(int transitionNumber) {
    if (transitionNumber < 0 || transitionNumber >= queues.size()) {
      throw new IllegalArgumentException("Transition number is out of bounds");
    }

    try {
      queues.get(transitionNumber).acquire();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Wakes up a thread waiting for a specific transition.
   *
   * @param transitionNumber the transition for which the waiting thread is woken up
   */
  void wakeUpThread(int transitionNumber) {
    if (transitionNumber < 0 || transitionNumber >= queues.size()) {
      throw new IllegalArgumentException("Transition number is out of bounds");
    }
    queues.get(transitionNumber).release();
  }

  /**
   * Checks if there are any threads waiting in the condition queues.
   *
   * @return boolean[] with a representative flag in the transition place waiting
   */
  boolean[] areThereWaitingThreads() {
    boolean[] waitingThreads = new boolean[queues.size()];

    for (int i = 0; i < waitingThreads.length; i++) {
      waitingThreads[i] = queues.get(i).hasQueuedThreads();
    }

    return waitingThreads;
  }

  /**
   * Resets all condition queues to a clean state for a new simulation run by creating new
   * semaphores.
   */
  public void reset() {
    int size = queues.size();
    queues.clear();
    for (int i = 0; i < size; i++) {
      queues.add(new Semaphore(0));
    }
  }
}
