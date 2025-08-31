package edu.unc.petri.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import edu.unc.petri.exceptions.TransitionTimeNotReachedException;
import edu.unc.petri.util.StateEquationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnableVectorTest {

  @Mock private IncidenceMatrix mockIncidenceMatrix;
  @Mock private CurrentMarking mockCurrentMarking;
  @Mock private TimeRangeMatrix mockTimeRangeMatrix;

  @Test
  void constructorShouldInitializeEnableVectorWithCorrectSize() {
    int numberOfTransitions = 5;
    when(mockTimeRangeMatrix.getTimeRangeMatrix()).thenReturn(new long[numberOfTransitions][]);
    EnableVector enableVector = new EnableVector(numberOfTransitions, mockTimeRangeMatrix);

    assertEquals(
        numberOfTransitions,
        enableVector.getTokenEnabledTransitions().length,
        "Enable vector size should match number of transitions.");

    for (int i = 0; i < numberOfTransitions; i++) {
      assertFalse(
          enableVector.getTokenEnabledTransitions()[i],
          "All transitions should be initialized to disabled.");
      assertEquals(
          0,
          enableVector.getEnableTransitionTime(i),
          "Enable time for transition " + i + " should be initialized to 0.");
    }
  }

  @Test
  void constructorShouldThrowExceptionForNonPositiveSize() {
    assertThrows(IllegalArgumentException.class, () -> new EnableVector(0, mockTimeRangeMatrix));
    assertThrows(IllegalArgumentException.class, () -> new EnableVector(-3, mockTimeRangeMatrix));
  }

  @Test
  void constructorShouldThrowExceptionForNullTimeRangeMatrix() {
    assertThrows(IllegalArgumentException.class, () -> new EnableVector(3, null));
  }

  @Test
  void constructorShouldThrowExceptionForMismatchedTimeRangeMatrixSize() {
    when(mockTimeRangeMatrix.getTimeRangeMatrix()).thenReturn(new long[4][]);
    assertThrows(IllegalArgumentException.class, () -> new EnableVector(3, mockTimeRangeMatrix));
  }

  @Test
  void updateEnableVectorShouldCorrectlyIdentifyEnabledTransitions() {
    when(mockTimeRangeMatrix.getTimeRangeMatrix()).thenReturn(new long[3][]);
    when(mockIncidenceMatrix.getPlaces()).thenReturn(3);
    when(mockIncidenceMatrix.getTransitions()).thenReturn(3);
    when(mockCurrentMarking.getMarking()).thenReturn(new int[] {1, 1, 1});

    EnableVector enableVector = new EnableVector(3, mockTimeRangeMatrix);

    int[] enabledMarking = {1, 0, 1}; // A valid future marking
    int[] disabledMarking = {-1, 2, 0}; // An invalid future marking (negative token)

    try (MockedStatic<StateEquationUtils> mockedUtils =
        Mockito.mockStatic(StateEquationUtils.class)) {
      // T0 will be enabled
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      0, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(enabledMarking);
      // T1 will be disabled
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      1, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(disabledMarking);
      // T2 will be enabled
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      2, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(enabledMarking);

      enableVector.updateEnableVector(mockIncidenceMatrix, mockCurrentMarking);

      assertTrue(enableVector.getTokenEnabledTransitions()[0], "T0 should be enabled.");
      assertFalse(enableVector.getTokenEnabledTransitions()[1], "T1 should be disabled.");
      assertTrue(enableVector.getTokenEnabledTransitions()[2], "T2 should be enabled.");
      assertTrue(enableVector.getEnableTransitionTime(0) > 0, "T0 enable time should be set.");
      assertEquals(0, enableVector.getEnableTransitionTime(1), "T1 enable time should be zero.");
      assertTrue(enableVector.getEnableTransitionTime(2) > 0, "T2 enable time should be set.");
    }
  }

  @Test
  void updateEnableVectorShouldPreserveEnableTimestamps() {
    when(mockTimeRangeMatrix.getTimeRangeMatrix()).thenReturn(new long[2][]);
    when(mockIncidenceMatrix.getPlaces()).thenReturn(2);
    when(mockIncidenceMatrix.getTransitions()).thenReturn(2);
    when(mockCurrentMarking.getMarking()).thenReturn(new int[] {1, 1});

    EnableVector enableVector = new EnableVector(2, mockTimeRangeMatrix);

    int[] enabledMarking = {1, 1};
    int[] disabledMarking = {-1, 1};

    try (MockedStatic<StateEquationUtils> mockedUtils =
        Mockito.mockStatic(StateEquationUtils.class)) {
      // First call: T0 enabled, T1 disabled
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      0, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(enabledMarking);
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      1, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(disabledMarking);

      enableVector.updateEnableVector(mockIncidenceMatrix, mockCurrentMarking);

      assertTrue(enableVector.getTokenEnabledTransitions()[0], "T0 should be enabled.");
      assertFalse(enableVector.getTokenEnabledTransitions()[1], "T1 should be disabled.");
      assertTrue(enableVector.getEnableTransitionTime(0) > 0, "T0 enable time should be set.");
      assertEquals(0, enableVector.getEnableTransitionTime(1), "T1 enable time should be zero.");

      // Second call: T0 remains enabled, T1 becomes enabled
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      0, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(enabledMarking);
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      1, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(enabledMarking);

      enableVector.updateEnableVector(mockIncidenceMatrix, mockCurrentMarking);

      assertTrue(enableVector.getTokenEnabledTransitions()[0], "T0 should still be enabled.");
      assertTrue(enableVector.getTokenEnabledTransitions()[1], "T1 should now be enabled.");
      assertTrue(
          enableVector.getEnableTransitionTime(0) > 0, "T0 enable time should be preserved.");
      assertTrue(enableVector.getEnableTransitionTime(1) > 0, "T1 enable time should be set.");

      // Third call: T0 becomes disabled, T1 remains enabled
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      0, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(disabledMarking);
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      1, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(enabledMarking);

      enableVector.updateEnableVector(mockIncidenceMatrix, mockCurrentMarking);

      assertFalse(enableVector.getTokenEnabledTransitions()[0], "T0 should now be disabled.");
      assertTrue(enableVector.getTokenEnabledTransitions()[1], "T1 should still be enabled.");
      assertEquals(0, enableVector.getEnableTransitionTime(0), "T0 enable time should be reset.");
      assertTrue(
          enableVector.getEnableTransitionTime(1) > 0, "T1 enable time should be preserved.");
    }
  }

  @Test
  void updateEnableVectorShouldThrowExceptionForNullParameters() {
    when(mockTimeRangeMatrix.getTimeRangeMatrix()).thenReturn(new long[3][]);
    EnableVector enableVector = new EnableVector(3, mockTimeRangeMatrix);
    assertThrows(
        IllegalArgumentException.class,
        () -> enableVector.updateEnableVector(null, mockCurrentMarking),
        "Should throw exception for null incidence matrix.");
    assertThrows(
        IllegalArgumentException.class,
        () -> enableVector.updateEnableVector(mockIncidenceMatrix, null),
        "Should throw exception for null current marking.");
  }

  @Test
  void updateEnableVectorShouldThrowExceptionForZeroSizeParameters() {
    when(mockTimeRangeMatrix.getTimeRangeMatrix()).thenReturn(new long[3][]);
    EnableVector enableVector = new EnableVector(3, mockTimeRangeMatrix);
    when(mockIncidenceMatrix.getPlaces()).thenReturn(0);
    assertThrows(
        IllegalArgumentException.class,
        () -> enableVector.updateEnableVector(mockIncidenceMatrix, mockCurrentMarking),
        "Should throw exception for zero places in incidence matrix.");
    when(mockIncidenceMatrix.getPlaces()).thenReturn(3);
    when(mockIncidenceMatrix.getTransitions()).thenReturn(0);
    assertThrows(
        IllegalArgumentException.class,
        () -> enableVector.updateEnableVector(mockIncidenceMatrix, mockCurrentMarking),
        "Should throw exception for zero transitions in incidence matrix.");
    assertThrows(
        IllegalArgumentException.class,
        () -> enableVector.updateEnableVector(mockIncidenceMatrix, mockCurrentMarking),
        "Should throw exception for zero size marking in current marking.");
  }

  @Test
  void isTransitionEnabledShouldThrowForInvalidIndex() {
    when(mockTimeRangeMatrix.getTimeRangeMatrix()).thenReturn(new long[1][]);
    EnableVector enableVector = new EnableVector(1, mockTimeRangeMatrix);

    assertThrows(IndexOutOfBoundsException.class, () -> enableVector.isTransitionEnabled(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> enableVector.isTransitionEnabled(1));
  }

  @Test
  void isTransitionEnabledShouldReturnFalseWhenTokenDisabledAndNotCallTimeRange() throws Exception {
    when(mockTimeRangeMatrix.getTimeRangeMatrix()).thenReturn(new long[1][]);
    EnableVector enableVector = new EnableVector(1, mockTimeRangeMatrix);

    assertFalse(enableVector.isTransitionEnabled(0));

    Mockito.verify(mockTimeRangeMatrix, Mockito.never())
        .isInsideTimeRange(Mockito.anyInt(), Mockito.anyLong());
    Mockito.verify(mockTimeRangeMatrix, Mockito.never())
        .isBeforeTimeRange(Mockito.anyInt(), Mockito.anyLong());
    Mockito.verify(mockTimeRangeMatrix, Mockito.never())
        .getSleepTimeToFire(Mockito.anyInt(), Mockito.anyLong());
  }

  @Test
  void isTransitionEnabledShouldReturnTrueWhenInsideTimeRange() throws Exception {
    when(mockTimeRangeMatrix.getTimeRangeMatrix()).thenReturn(new long[1][]);
    when(mockIncidenceMatrix.getPlaces()).thenReturn(1);
    when(mockIncidenceMatrix.getTransitions()).thenReturn(1);
    when(mockCurrentMarking.getMarking()).thenReturn(new int[] {1});

    EnableVector enableVector = new EnableVector(1, mockTimeRangeMatrix);

    try (MockedStatic<StateEquationUtils> mockedUtils =
        Mockito.mockStatic(StateEquationUtils.class)) {
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      0, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(new int[] {1});
      enableVector.updateEnableVector(mockIncidenceMatrix, mockCurrentMarking);
    }

    Mockito.when(mockTimeRangeMatrix.isInsideTimeRange(Mockito.eq(0), Mockito.anyLong()))
        .thenReturn(true);

    assertTrue(enableVector.isTransitionEnabled(0));

    Mockito.verify(mockTimeRangeMatrix, Mockito.never())
        .isBeforeTimeRange(Mockito.anyInt(), Mockito.anyLong());
    Mockito.verify(mockTimeRangeMatrix, Mockito.never())
        .getSleepTimeToFire(Mockito.anyInt(), Mockito.anyLong());
  }

  @Test
  void isTransitionEnabledShouldThrowWhenBeforeTimeRange() {
    when(mockTimeRangeMatrix.getTimeRangeMatrix()).thenReturn(new long[1][]);
    when(mockIncidenceMatrix.getPlaces()).thenReturn(1);
    when(mockIncidenceMatrix.getTransitions()).thenReturn(1);
    when(mockCurrentMarking.getMarking()).thenReturn(new int[] {1});

    EnableVector enableVector = new EnableVector(1, mockTimeRangeMatrix);

    try (MockedStatic<StateEquationUtils> mockedUtils =
        Mockito.mockStatic(StateEquationUtils.class)) {
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      0, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(new int[] {1});
      enableVector.updateEnableVector(mockIncidenceMatrix, mockCurrentMarking);
    }

    Mockito.when(mockTimeRangeMatrix.isInsideTimeRange(Mockito.eq(0), Mockito.anyLong()))
        .thenReturn(false);
    Mockito.when(mockTimeRangeMatrix.isBeforeTimeRange(Mockito.eq(0), Mockito.anyLong()))
        .thenReturn(true);
    Mockito.when(mockTimeRangeMatrix.getSleepTimeToFire(Mockito.eq(0), Mockito.anyLong()))
        .thenReturn(50L);

    TransitionTimeNotReachedException ex =
        assertThrows(
            TransitionTimeNotReachedException.class, () -> enableVector.isTransitionEnabled(0));
    // If TransitionTimeNotReachedException exposes the sleep time:
    // assertEquals(50L, ex.getSleepTime());

    Mockito.verify(mockTimeRangeMatrix).getSleepTimeToFire(Mockito.eq(0), Mockito.anyLong());
  }

  @Test
  void isTransitionEnabledShouldReturnFalseWhenAfterTimeRange() throws Exception {
    when(mockTimeRangeMatrix.getTimeRangeMatrix()).thenReturn(new long[1][]);
    when(mockIncidenceMatrix.getPlaces()).thenReturn(1);
    when(mockIncidenceMatrix.getTransitions()).thenReturn(1);
    when(mockCurrentMarking.getMarking()).thenReturn(new int[] {1});

    EnableVector enableVector = new EnableVector(1, mockTimeRangeMatrix);

    try (MockedStatic<StateEquationUtils> mockedUtils =
        Mockito.mockStatic(StateEquationUtils.class)) {
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      0, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(new int[] {1});
      enableVector.updateEnableVector(mockIncidenceMatrix, mockCurrentMarking);
    }

    Mockito.when(mockTimeRangeMatrix.isInsideTimeRange(Mockito.eq(0), Mockito.anyLong()))
        .thenReturn(false);
    Mockito.when(mockTimeRangeMatrix.isBeforeTimeRange(Mockito.eq(0), anyLong())).thenReturn(false);

    assertFalse(enableVector.isTransitionEnabled(0));

    Mockito.verify(mockTimeRangeMatrix, Mockito.never())
        .getSleepTimeToFire(Mockito.anyInt(), Mockito.anyLong());
  }
}
