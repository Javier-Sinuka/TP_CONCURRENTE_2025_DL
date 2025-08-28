package edu.unc.petri.workers;

import edu.unc.petri.monitor.MonitorInterface;
import edu.unc.petri.simulation.SimulationManager;
import edu.unc.petri.util.Segment;

/**
 * The Worker class represents a thread that actively attempts to fire transitions in a Petri net.
 * Each worker is assigned a specific segment and will continuously try to fire the transitions
 * within that segment.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-14-08
 */
public class Worker extends Thread {

  /** The simulation manager to check for shutdown signals. */
  private final SimulationManager simulationManager;

  /** The monitor used to interact with the Petri net. */
  private final MonitorInterface monitor;

  /** The segment this worker is responsible for, containing the transitions it can fire. */
  private final Segment segment;

  /**
   * Constructs a new Worker thread.
   *
   * @param monitor the monitor to interact with the Petri net
   * @param segment the segment this worker is responsible for
   */
  public Worker(
      SimulationManager simulationManager,
      MonitorInterface monitor,
      Segment segment,
      int segmentThreadIndex) {
    super(segment.name + "-Worker-" + segmentThreadIndex);
    this.simulationManager = simulationManager;
    this.monitor = monitor;
    this.segment = segment;
  }

  /**
   * The main execution loop of the worker thread. It continuously selects transitions from its
   * assigned segment in order and attempts to fire them through the monitor.
   */
  @Override
  public void run() {
    int index = 0;
    while (!simulationManager.isInvariantLimitReached()
        && !Thread.currentThread().isInterrupted()) {
      int transition = segment.transitions[index];
      try {
        boolean hasFired = monitor.fireTransition(transition);

        if (!hasFired) {
          break; // Segment is done, exit the while loop
        }
      } catch (RuntimeException ex) {
        // Exit silently on shutdown interrupt
        if (causedByInterruptedException(ex)) {
          Thread.currentThread().interrupt(); // preserve interrupt status
          break; // leave the while loop quietly
        }
        throw ex; // real bug -> keep the stack trace
      }
      index = (index + 1) % segment.transitions.length;
    }

    return;
  }

  private static boolean causedByInterruptedException(Throwable ex) {
    for (Throwable t = ex; t != null && t.getCause() != t; t = t.getCause()) {
      if (t instanceof InterruptedException) {
        return true;
      }
    }
    return false;
  }
}
