package edu.unc.petri.workers;

import edu.unc.petri.monitor.MonitorInterface;
import edu.unc.petri.simulation.InvariantTracker;
import edu.unc.petri.util.Segment;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

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
  private final InvariantTracker invariantTracker;

  /** The monitor used to interact with the Petri net. */
  private final MonitorInterface monitor;

  /** The segment this worker is responsible for, containing the transitions it can fire. */
  private final Segment segment;

  /** The barrier to synchronize the start of all workers in a simulation run. */
  private final CyclicBarrier startBarrier;

  /** The latch to signal when this worker has completed its execution. */
  private final CountDownLatch doneSignal;

  /**
   * Constructs a new Worker thread.
   *
   * @param monitor the monitor to interact with the Petri net
   * @param segment the segment this worker is responsible for
   * @param startBarrier the barrier to wait on before starting execution
   */
  public Worker(
      InvariantTracker invariantTracker,
      MonitorInterface monitor,
      Segment segment,
      int segmentThreadIndex,
      CyclicBarrier startBarrier,
      CountDownLatch doneSignal) {
    super(segment.name + "-Worker-" + segmentThreadIndex);
    this.invariantTracker = invariantTracker;
    this.monitor = monitor;
    this.segment = segment;
    this.startBarrier = startBarrier;
    this.doneSignal = doneSignal;
  }

  /**
   * The main execution loop of the worker thread. It continuously selects transitions from its
   * assigned segment in order and attempts to fire them through the monitor.
   */
  @Override
  public void run() {
    try {
      // Wait for all other workers to reach this point before starting.
      if (startBarrier != null) {
        startBarrier.await();
      }
    } catch (InterruptedException e) {
      // If interrupted while waiting, restore the interrupted status and exit.
      Thread.currentThread().interrupt();
      return;
    } catch (BrokenBarrierException e) {
      // If another thread fails or is interrupted while waiting, the barrier breaks.
      // This worker should also exit.
      return;
    }

    int index = 0;
    while (!invariantTracker.isInvariantLimitReached() && !Thread.currentThread().isInterrupted()) {
      int transition = segment.transitions[index];
      try {
        boolean hasFired = monitor.fireTransition(transition);

        if (!hasFired) {
          doneSignal.countDown(); // Signal that this worker is done
          return; // Segment is done, exit the while loop
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
