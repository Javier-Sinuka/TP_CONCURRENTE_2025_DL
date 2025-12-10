package edu.unc.petri.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.Test;

class ConditionQueuesTest {

  @Test
  void constructorShouldCreateCorrectNumberOfQueues() {
    int numTransitions = 15;

    ConditionQueues conditionQueues = new ConditionQueues(numTransitions);

    // We use reflection to inspect the private 'queues' field to verify its size.
    try {
      java.lang.reflect.Field field = ConditionQueues.class.getDeclaredField("queues");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      ArrayList<Semaphore> queues = (ArrayList<Semaphore>) field.get(conditionQueues);
      assertEquals(
          numTransitions,
          queues.size(),
          "Should have created one semaphore queue for each transition.");
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail("Could not access the 'queues' field for testing.", e);
    }
  }

  @Test
  void constructorShouldThrowExceptionForNonPositiveSize() {
    assertThrows(IllegalArgumentException.class, () -> new ConditionQueues(0));
    assertThrows(IllegalArgumentException.class, () -> new ConditionQueues(-5));
  }

  @Test
  void waitForTransitionShouldBlockAndRelease() throws InterruptedException {
    ConditionQueues conditionQueues = new ConditionQueues(1);
    Thread waitingThread =
        new Thread(
            () -> {
              conditionQueues.waitForTransition(0);
            });
    waitingThread.start();
    // Give the thread a moment to start and attempt to acquire the semaphore
    Thread.sleep(100);
    // The thread should be blocked, so it should still be alive
    assertTrue(waitingThread.isAlive(), "Thread should be blocked and waiting.");
    // Now wake up the thread
    conditionQueues.wakeUpThread(0);
    // Wait for the thread to finish
    waitingThread.join(1000);
    // After being woken up, the thread should have finished execution
    assertFalse(waitingThread.isAlive(), "Thread should have been woken up and finished.");
  }

  @Test
  void waitForTransitionShouldThrowExceptionForInvalidIndex() {
    ConditionQueues conditionQueues = new ConditionQueues(3);
    assertThrows(IllegalArgumentException.class, () -> conditionQueues.waitForTransition(-1));
    assertThrows(IllegalArgumentException.class, () -> conditionQueues.waitForTransition(3));
  }

  @Test
  void wakeUpThreadShouldReleaseWaitingThread() throws InterruptedException {
    ConditionQueues conditionQueues = new ConditionQueues(1);
    Thread waitingThread =
        new Thread(
            () -> {
              conditionQueues.waitForTransition(0);
            });
    waitingThread.start();
    // Give the thread a moment to start and attempt to acquire the semaphore
    Thread.sleep(100);
    // The thread should be blocked, so it should still be alive
    assertTrue(waitingThread.isAlive(), "Thread should be blocked and waiting.");
    // Now wake up the thread
    conditionQueues.wakeUpThread(0);
    // Wait for the thread to finish
    waitingThread.join(1000);
    // After being woken up, the thread should have finished execution
    assertFalse(waitingThread.isAlive(), "Thread should have been woken up and finished.");
  }

  @Test
  void wakeUpThreadShouldThrowExceptionForInvalidIndex() {
    ConditionQueues conditionQueues = new ConditionQueues(3);
    assertThrows(IllegalArgumentException.class, () -> conditionQueues.wakeUpThread(-1));
    assertThrows(IllegalArgumentException.class, () -> conditionQueues.wakeUpThread(3));
  }

  @Test
  void areThereWaitingThreadsShouldReturnAllFalseForNewQueues() {
    ConditionQueues conditionQueues = new ConditionQueues(10);

    boolean[] waitingThreads = conditionQueues.areThereWaitingThreads();

    assertEquals(10, waitingThreads.length);
    for (int i = 0; i < waitingThreads.length; i++) {
      assertFalse(
          waitingThreads[i], "No threads should be waiting in a newly created queue at index " + i);
    }
  }
}
