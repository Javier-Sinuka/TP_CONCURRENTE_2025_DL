package edu.unc.petri.monitor;

import edu.unc.petri.core.PetriNet;
import edu.unc.petri.exceptions.TransitionTimeNotReachedException;
import edu.unc.petri.policy.PolicyInterface;
import edu.unc.petri.util.Log;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * The Monitor class is responsible for monitoring the Petri net simulation. It implements the
 * MonitorInterface, which defines the methods for monitoring the simulation's progress and
 * significant events.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public class Monitor implements MonitorInterface {

  /** The number of permits for the semaphore used for mutual exclusion. */
  private final int permits = 1;

  /** The Petri net being monitored. */
  private PetriNet petriNet;

  /** The policy used to choose transitions. */
  private PolicyInterface policy;

  /** The mutex used for mutual exclusion. */
  private Semaphore mutex;

  /** The condition queues used for synchronization. */
  private ConditionQueues conditionQueues;

  /** The log for recording events in the simulation. */
  private Log log;

  /**
   * Constructor for the Monitor class.
   *
   * @param petriNet the Petri net to be monitored
   * @param policy the policy used to choose transitions
   * @param log the log for recording events in the simulation
   */
  public Monitor(
      PetriNet petriNet, ConditionQueues conditionQueues, PolicyInterface policy, Log log) {
    this.petriNet = petriNet;
    this.conditionQueues = conditionQueues;
    this.policy = policy;
    this.mutex = new Semaphore(permits);
    this.log = log;
  }

  /**
   * Fires a transition in the Petri net. This method is called to execute a transition based on the
   * policy defined.
   *
   * @param t the transition to fire
   * @return true if the transition was successfully fired, false otherwise
   */
  @Override
  public boolean fireTransition(int t) {

    try {
      mutex.acquire();
      log.logDebug("Thread " + Thread.currentThread().getName() + " enters monitor to fire " + t);
      while (true) {
        try {
          if (petriNet.fire(t)) {
            log.logDebug("Thread " + Thread.currentThread().getName() + " successfully fired " + t);

            boolean[] waitingThreads = conditionQueues.areThereWaitingThreads();
            boolean[] enableTransitions = petriNet.getEnableTransitions();

            ArrayList<Integer> transitionsThatCouldBeFired =
                getTransitionsThatCouldBeFired(waitingThreads, enableTransitions);

            log.logDebug(
                "Thread "
                    + Thread.currentThread().getName()
                    + " checks if there are waiting threads for enabled transitions");

            if (transitionsThatCouldBeFired.size() > 0) {
              log.logDebug(
                  "Thread "
                      + Thread.currentThread().getName()
                      + " finds waiting threads for enabled transitions");
              int transition = policy.choose(transitionsThatCouldBeFired);

              log.logDebug(
                  "Thread "
                      + Thread.currentThread().getName()
                      + " chooses to wake up the thread waiting for "
                      + transition);

              conditionQueues.wakeUpThread(transition);

              log.logDebug("Thread " + Thread.currentThread().getName() + " leaves monitor");
              return true;
            } else {
              log.logDebug(
                  "Thread "
                      + Thread.currentThread().getName()
                      + " finds no waiting threads for enabled transitions");
              break;
            }
          } else {
            log.logDebug("Thread " + Thread.currentThread().getName() + " could not fire " + t);
            mutex.release();
            log.logDebug("Thread " + Thread.currentThread().getName() + " goes to wait for " + t);
            conditionQueues.waitForTransition(t);
            log.logDebug(
                "Thread "
                    + Thread.currentThread().getName()
                    + " wakes up and re-enters monitor to fire "
                    + t);

            // When a thread wakes up, it is already inside the monitor and tries to fire the
            // transition
            // again, but it doesn't reacquire the mutex because it was handed to it by the
            // signaler.
          }
        } catch (TransitionTimeNotReachedException e) {
          log.logDebug(
              "Thread "
                  + Thread.currentThread().getName()
                  + " could not fire "
                  + t
                  + " because the transition time has not been reached. Sleeping for "
                  + e.getSleepTime()
                  + " ms");
          mutex.release(); // Release the mutex before sleeping
          try {
            Thread.sleep(e.getSleepTime()); // Sleeps outside the monitor
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            return false; // Exit if interrupted during sleep
          }
          mutex.acquire(); // Reacquire the mutex after waking up
          log.logDebug(
              "Thread "
                  + Thread.currentThread().getName()
                  + " wakes up and re-enters monitor to fire "
                  + t);
        }
      }

      log.logDebug("Thread " + Thread.currentThread().getName() + " leaves monitor");
      mutex.release();
      return true;
    } catch (InterruptedException e) {
      mutex.release(); // Ensure the mutex is released if an interruption occurs
      return false;
    }
  }

  private ArrayList<Integer> getTransitionsThatCouldBeFired(
      boolean[] waitingThreads, boolean[] enableTransitions) {
    ArrayList<Integer> transitions = new ArrayList<>();

    for (int i = 0; i < petriNet.getNumberOfTransitions(); i++) {
      if (waitingThreads[i] && enableTransitions[i]) {
        transitions.add(i);
      }
    }

    return transitions;
  }
}
