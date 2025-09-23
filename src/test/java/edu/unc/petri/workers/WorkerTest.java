package edu.unc.petri.workers;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unc.petri.monitor.MonitorInterface;
import edu.unc.petri.simulation.InvariantTracker;
import edu.unc.petri.util.Segment;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkerTest {

  @Mock private InvariantTracker invariantTracker;
  @Mock private MonitorInterface monitor;
  @Mock private CyclicBarrier startBarrier;
  @Mock private CountDownLatch doneSignal;

  private Segment segment;
  private Worker worker;

  @BeforeEach
  void setUp() {
    segment = new Segment("test-segment", 1, new int[] {0, 1});
  }

  @Test
  void run_firesTransitionsUntilLimitReached() throws BrokenBarrierException, InterruptedException {
    // Given
    when(invariantTracker.isInvariantLimitReached()).thenReturn(false, false, true);
    when(monitor.fireTransition(0)).thenReturn(true);
    when(monitor.fireTransition(1)).thenReturn(true);
    worker = new Worker(invariantTracker, monitor, segment, 0, startBarrier, doneSignal);

    // When
    worker.run();

    // Then
    verify(startBarrier).await();
    verify(monitor).fireTransition(0);
    verify(monitor).fireTransition(1);
  }

  @Test
  void run_stopsWhenFireTransitionReturnsFalse()
      throws BrokenBarrierException, InterruptedException {
    // Given
    when(invariantTracker.isInvariantLimitReached()).thenReturn(false);
    when(monitor.fireTransition(0)).thenReturn(false);
    worker = new Worker(invariantTracker, monitor, segment, 0, startBarrier, doneSignal);

    // When
    worker.run();

    // Then
    verify(startBarrier).await();
    verify(monitor).fireTransition(0);
    verify(doneSignal).countDown();
    verify(monitor, never()).fireTransition(1);
  }

  @Test
  void run_interruptedExceptionBeforeBarrier() throws BrokenBarrierException, InterruptedException {
    // Given
    doThrow(new InterruptedException()).when(startBarrier).await();
    worker = new Worker(invariantTracker, monitor, segment, 0, startBarrier, doneSignal);

    // When
    worker.run();

    // Then
    verify(monitor, never()).fireTransition(0);
  }

  @Test
  void run_brokenBarrierException() throws BrokenBarrierException, InterruptedException {
    // Given
    doThrow(new BrokenBarrierException()).when(startBarrier).await();
    worker = new Worker(invariantTracker, monitor, segment, 0, startBarrier, doneSignal);

    // When
    worker.run();

    // Then
    verify(monitor, never()).fireTransition(0);
  }

  @Test
  void run_interruptedInLoop() {
    // Given
    when(invariantTracker.isInvariantLimitReached()).thenReturn(false);
    worker = new Worker(invariantTracker, monitor, segment, 0, startBarrier, doneSignal);

    // When
    Thread.currentThread().interrupt();
    worker.run();

    // Then
    verify(monitor, never()).fireTransition(0);
  }

  @Test
  void run_runtimeExceptionInLoop() {
    // Given
    when(invariantTracker.isInvariantLimitReached()).thenReturn(false);
    when(monitor.fireTransition(0)).thenThrow(new RuntimeException("Test Exception"));
    worker = new Worker(invariantTracker, monitor, segment, 0, startBarrier, doneSignal);

    // When
    try {
      worker.run();
    } catch (RuntimeException e) {
      // Expected
    }

    // Then
    verify(monitor).fireTransition(0);
  }

  @Test
  void run_runtimeExceptionCausedByInterruptedException() {
    // Given
    when(invariantTracker.isInvariantLimitReached()).thenReturn(false);
    when(monitor.fireTransition(0)).thenThrow(new RuntimeException(new InterruptedException()));
    worker = new Worker(invariantTracker, monitor, segment, 0, startBarrier, doneSignal);

    // When
    worker.run();

    // Then
    verify(monitor).fireTransition(0);
  }
}
