package edu.unc.petri.monitor;

import edu.unc.petri.core.PetriNet;
import edu.unc.petri.policy.PolicyInterface;
import edu.unc.petri.util.Log;
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

    private final int PERMITS = 1;

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
    this.mutex = new Semaphore(PERMITS);
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
          while (true) {
              if(petriNet.fire(t)){
                  boolean[] waitingThreads = conditionQueues.areThereWaitingThreads();
                  boolean[] enableTransitions = petriNet.getEnableTransitions();

                  int[] transitions = getNextTransitionsCouldFire(waitingThreads, enableTransitions);

                  if(transitions.length != 0){
                      int transition = policy.choose(transitions);

                      conditionQueues.wakeUpThread(transition);

                      log.logTransition(t, Thread.currentThread().getName());
                      return true;
                  }
                  else {
                      break;
                  }
              }
              else {
                  mutex.release();
                  conditionQueues.waitForTransition(t);
              }
          }

          mutex.release();
          return true;
      } catch (InterruptedException e) {
          return  false;
      }
  }

  private int[] getNextTransitionsCouldFire(boolean[] waitingThreads, boolean[] enableTransitions) {
      int numberOfTransitions = waitingThreads.length;
      int[] transitions = new int[numberOfTransitions];

      for (int i = 0; i < numberOfTransitions; i++) {
          if(waitingThreads[i] && enableTransitions[i]){
              transitions[i] = i;
          }
      }

    return transitions;
  }
}
