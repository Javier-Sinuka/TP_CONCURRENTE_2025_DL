package edu.unc.petri.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimeRangeMatrixTest {

  @Mock private EnableVector mockEnableVector;

  private TimeRangeMatrix timeRangeMatrix;

  @BeforeEach
  void setUp() {
    long[][] ranges = {
      {50, 150}, // T0
      {200, 300}, // T1
      {0, 50}, // T2
      {0, 0}, // T3 - instantaneous
      {100, 100} // T4 - fixed time
    };

    timeRangeMatrix = new TimeRangeMatrix(ranges);
  }

  @Test
  void isInsideTimeRangeShouldReturnTrueForInstantaneousTransition() {
    int transition = 3; // [0, 0]
    long enabledTime = System.currentTimeMillis();
    long currentTime = System.nanoTime();

    assertTrue(
        timeRangeMatrix.isInsideTimeRange(transition, enabledTime, currentTime),
        "Should be inside time range for instantaneous transition [0,0].");
  }

  @Test
  void isInsideTimeRangeShouldReturnTrueForFixedTimeTransitions() throws InterruptedException {
    int transition = 4; // [100, 100]
    long enabledTime = System.nanoTime();

    // Simulate that 100ms and 20ns have passed since the transition was enabled.
    Thread.sleep(100, 20);

    long currentTime = System.nanoTime();

    assertTrue(
        timeRangeMatrix.isInsideTimeRange(transition, enabledTime, currentTime),
        "Should be inside time range as ~100ms is equal to 100.");
  }

  @Test
  void isInsideTimeRangeShouldReturnTrueWhenTimePassedIsInInterval() throws InterruptedException {
    int transition = 0; // [50, 150]
    long enabledTime = System.nanoTime();

    // Simulate that 100ms have passed since the transition was enabled.
    Thread.sleep(100);

    long currentTime = System.nanoTime();

    assertTrue(
        timeRangeMatrix.isInsideTimeRange(transition, enabledTime, currentTime),
        "Should be inside time range as ~100ms is between 50 and 150.");
  }

  @Test
  void isInsideTimeRangeShouldReturnFalseWhenTimePassedIsBeforeIntervalForIntervalTransitions() {
    int transition = 1; // [200, 300]
    long enabledTime = System.currentTimeMillis();
    long currentTime = System.nanoTime();

    assertFalse(
        timeRangeMatrix.isInsideTimeRange(transition, enabledTime, currentTime),
        "Should be outside time range as ~0ms is less than 200.");
  }

  @Test
  void isInsideTimeRangeShouldReturnFalseWhenTimePassedIsAfterIntervalForIntervalTransitions()
      throws InterruptedException {
    int transition = 2; // [0, 50]
    long enabledTime = System.currentTimeMillis();

    // Simulate that 80ms have passed.
    Thread.sleep(80);

    long currentTime = System.nanoTime();

    assertFalse(
        timeRangeMatrix.isInsideTimeRange(transition, enabledTime, currentTime),
        "Should be outside time range as ~80ms is greater than 50.");
  }

  @Test
  void isBeforeTimeRangeShouldReturnTrueWhenTimePassedIsBeforeInterval()
      throws InterruptedException {
    int transition = 0; // [50, 150]
    long enabledTime = System.nanoTime();

    // Simulate that 30ms have passed since the transition was enabled.
    Thread.sleep(30);

    long currentTime = System.nanoTime();

    assertTrue(
        timeRangeMatrix.isBeforeTimeRange(transition, enabledTime, currentTime),
        "Should be before time range as ~30ms is less than 50.");
  }

  @Test
  void isBeforeTimeRangeShouldReturnFalseWhenTimePassedIsInInterval() throws InterruptedException {
    int transition = 0; // [50, 150]
    long enabledTime = System.nanoTime();

    // Simulate that 100ms have passed since the transition was enabled.
    Thread.sleep(100);

    long currentTime = System.nanoTime();

    assertFalse(
        timeRangeMatrix.isBeforeTimeRange(transition, enabledTime, currentTime),
        "Should not be before time range as ~100ms is between 50 and 150.");
  }

  @Test
  void isBeforeTimeRangeShouldReturnFalseWhenTimePassedIsAfterInterval()
      throws InterruptedException {
    int transition = 0; // [50, 150]
    long enabledTime = System.nanoTime();

    // Simulate that 200ms have passed since the transition was enabled.
    Thread.sleep(200);

    long currentTime = System.nanoTime();

    assertFalse(
        timeRangeMatrix.isBeforeTimeRange(transition, enabledTime, currentTime),
        "Should not be before time range as ~200ms is greater than 150.");
  }

  @Test
  void getSleepTimeToFireShouldReturnCorrectSleepTime() throws InterruptedException {
    int transition = 0; // [50, 150] ms
    long enabledTime = System.nanoTime();

    // Sleep for 30ms (less than startRange)
    Thread.sleep(30);

    long sleepTime = timeRangeMatrix.getSleepTimeToFire(transition, enabledTime);

    // Start range in nanoseconds
    long expectedStartNanos = 50 * 1_000_000L;
    long timePassed = System.nanoTime() - enabledTime;
    long expectedSleepTime = expectedStartNanos - timePassed;

    // Allow a small tolerance for scheduling delays
    long tolerance = 2_000_000L; // 2ms in nanoseconds
    assertTrue(
        sleepTime > 0 && Math.abs(sleepTime - expectedSleepTime) < tolerance,
        "Sleep time should be close to expected value and positive.");
  }

  @Test
  void getSleepTimeToFireShouldReturnZeroIfInRange() throws InterruptedException {
    int transition = 0; // [50, 150] ms
    long enabledTime = System.nanoTime();

    // Sleep for 100ms (between start and end)
    Thread.sleep(100);

    long sleepTime = timeRangeMatrix.getSleepTimeToFire(transition, enabledTime);

    assertTrue(sleepTime == 0, "Sleep time should be zero when already in the time range.");
  }

  @Test
  void getSleepTimeToFireShouldReturnZeroIfAfterRange() throws InterruptedException {
    int transition = 0; // [50, 150] ms
    long enabledTime = System.nanoTime();

    // Sleep for 200ms (after end)
    Thread.sleep(200);

    long sleepTime = timeRangeMatrix.getSleepTimeToFire(transition, enabledTime);

    assertTrue(sleepTime == 0, "Sleep time should be zero when after the time range.");
  }
}
