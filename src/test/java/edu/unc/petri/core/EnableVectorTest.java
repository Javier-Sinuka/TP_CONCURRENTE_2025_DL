package edu.unc.petri.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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

  @Test
  void constructorShouldInitializeEnableVectorWithCorrectSize() {
    int numberOfTransitions = 5;
    EnableVector enableVector = new EnableVector(numberOfTransitions);

    assertEquals(
        numberOfTransitions,
        enableVector.getEnableVector().length,
        "Enable vector size should match number of transitions.");

    for (int i = 0; i < numberOfTransitions; i++) {
      assertFalse(
          enableVector.isTransitionEnabled(i),
          "Transition " + i + " should be initialized as disabled.");
      assertEquals(
          0,
          enableVector.getEnableTransitionTime(i),
          "Enable time for transition " + i + " should be initialized to 0.");
    }
  }

  @Test
  void constructorShouldThrowExceptionForNonPositiveSize() {
    assertThrows(IllegalArgumentException.class, () -> new EnableVector(0));
    assertThrows(IllegalArgumentException.class, () -> new EnableVector(-3));
  }

  @Test
  void updateEnableVectorShouldCorrectlyIdentifyEnabledTransitions() {
    when(mockIncidenceMatrix.getPlaces()).thenReturn(3);
    when(mockIncidenceMatrix.getTransitions()).thenReturn(3);
    when(mockCurrentMarking.getMarking()).thenReturn(new int[] {1, 1, 1});

    EnableVector enableVector = new EnableVector(3);

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

      assertTrue(enableVector.isTransitionEnabled(0), "T0 should be enabled.");
      assertFalse(enableVector.isTransitionEnabled(1), "T1 should be disabled.");
      assertTrue(enableVector.isTransitionEnabled(2), "T2 should be enabled.");
      assertNotNull(enableVector.getEnableTransitionTime(0));
      assertNotNull(enableVector.getEnableTransitionTime(2));
    }
  }

  @Test
  void updateEnableVectorShouldPreserveEnableTimestamps() {
    when(mockIncidenceMatrix.getPlaces()).thenReturn(2);
    when(mockIncidenceMatrix.getTransitions()).thenReturn(2);
    when(mockCurrentMarking.getMarking()).thenReturn(new int[] {1, 1});

    EnableVector enableVector = new EnableVector(2);

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
      long t0EnableTime = enableVector.getEnableTransitionTime(0);

      assertTrue(enableVector.isTransitionEnabled(0), "T0 should be enabled.");
      assertFalse(enableVector.isTransitionEnabled(1), "T1 should be disabled.");
      assertTrue(t0EnableTime > 0, "T0 enable time should be set.");
      long t1EnableTime = enableVector.getEnableTransitionTime(1);
      assertEquals(0, t1EnableTime, "T1 enable time should be zero.");

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
      long t0EnableTime2 = enableVector.getEnableTransitionTime(0);

      assertTrue(enableVector.isTransitionEnabled(0), "T0 should still be enabled.");
      assertTrue(enableVector.isTransitionEnabled(1), "T1 should now be enabled.");
      assertEquals(t0EnableTime, t0EnableTime2, "T0 enable time should be preserved.");
      long t1EnableTime2 = enableVector.getEnableTransitionTime(1);
      assertTrue(t1EnableTime2 > 0, "T1 enable time should be set.");

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
      long t0EnableTime3 = enableVector.getEnableTransitionTime(0);

      assertFalse(enableVector.isTransitionEnabled(0), "T0 should now be disabled.");
      assertTrue(enableVector.isTransitionEnabled(1), "T1 should still be enabled.");
      assertEquals(0, t0EnableTime3, "T0 enable time should be reset to zero.");
      long t1EnableTime3 = enableVector.getEnableTransitionTime(1);
      assertEquals(t1EnableTime2, t1EnableTime3, "T1 enable time should be preserved.");
    }
  }

  @Test
  void updateEnableVectorShouldThrowExceptionForNullParameters() {
    EnableVector enableVector = new EnableVector(3);
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
    EnableVector enableVector = new EnableVector(3);
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
}
