package edu.unc.petri.monitor;

import java.util.ArrayList;

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
  private final ArrayList<Transition> queues;

  /** Number of transitions there can be */
  private final int transitionsNumber;

  /**
   * Constructor to initialize the condition queues for a given number of transitions.
   *
   * @param transitionsNumber of the petri net model
   */
  public ConditionQueues(int transitionsNumber) {
    queues = new ArrayList<>();
    this.transitionsNumber = transitionsNumber;
  }

  /**
   * Queues a thread into a transition's condition queue.
   *
   * @param transitionNumber the transition for which the thread is queued
   */
  void waitForTransition(int transitionNumber) {
    Transition transition = new Transition();
    transition.setTransitionNumber(transitionNumber);

    queues.add(transition);

    try {
      transition.getCondition().acquire();
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
    Transition transition = null;

    for (Transition t : queues) {
      if (t.getTransitionNumber() == transitionNumber) {
        transition = t;
        break;
      }
    }

    if (transition != null) {
      queues.remove(transition);
      transition.getCondition().release();
    }
  }

  /**
   * Checks if there are any threads waiting in the condition queues.
   *
   * @return int[] with the number of threads waiting for each transition
   */
  int[] areThereWaintingThreads() {
    int[] waitingThreads = new int[transitionsNumber];

    for (Transition transition : queues) {
      int transitionNumber = transition.getTransitionNumber();
      waitingThreads[transitionNumber]++;
    }

    return waitingThreads;
  }
}
