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
      {0, 0} // T3 - instantaneous
    };

    timeRangeMatrix = new TimeRangeMatrix(ranges);
  }

  @Test
  void isInsideTimeRangeShouldReturnTrueWhenTimePassedIsInInterval() throws InterruptedException {
    int transition = 0; // [50, 150]
    long enabledTime = System.currentTimeMillis();

    // Simulate that 100ms have passed since the transition was enabled.
    Thread.sleep(100);

    assertTrue(
        timeRangeMatrix.isInsideTimeRange(transition, enabledTime),
        "Should be inside time range as ~100ms is between 50 and 150.");
  }

  @Test
  void isInsideTimeRangeShouldReturnTrueForInstantaneousTransition() {
    int transition = 3; // [0, 0]
    long enabledTime = System.currentTimeMillis();

    assertTrue(
        timeRangeMatrix.isInsideTimeRange(transition, enabledTime),
        "Should be inside time range for instantaneous transition [0,0].");
  }

  @Test
  void isInsideTimeRangeShouldReturnFalseWhenTimePassedIsBeforeInterval() {
    int transition = 1; // [200, 300]
    long enabledTime = System.currentTimeMillis();

    assertFalse(
        timeRangeMatrix.isInsideTimeRange(transition, enabledTime),
        "Should be outside time range as ~0ms is less than 200.");
  }

  @Test
  void isInsideTimeRangeShouldReturnFalseWhenTimePassedIsAfterInterval()
      throws InterruptedException {
    int transition = 2; // [0, 50]
    long enabledTime = System.currentTimeMillis();

    // Simulate that 80ms have passed.
    Thread.sleep(80);

    assertFalse(
        timeRangeMatrix.isInsideTimeRange(transition, enabledTime),
        "Should be outside time range as ~80ms is greater than 50.");
  }
}
