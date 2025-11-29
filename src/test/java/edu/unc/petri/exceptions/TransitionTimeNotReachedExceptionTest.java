package edu.unc.petri.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TransitionTimeNotReachedExceptionTest {

  @Test
  void testConstructorAndGetter() {
    long sleepTime = 1000L;
    TransitionTimeNotReachedException exception = new TransitionTimeNotReachedException(sleepTime);
    assertEquals(sleepTime, exception.getSleepTimeNanos());
  }
}
