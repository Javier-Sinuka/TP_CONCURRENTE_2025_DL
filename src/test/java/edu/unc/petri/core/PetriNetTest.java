package edu.unc.petri.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unc.petri.util.StateEquationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class) // Automatically initializes mocks
class PetriNetTest {

  // Dependencies
  @Mock private IncidenceMatrix mockIncidenceMatrix;
  @Mock private CurrentMarking mockCurrentMarking;
  @Mock private TimeRangeMatrix mockTimeRangeMatrix;
  @Mock private EnableVector mockEnableVector;

  private PetriNet petriNet;

  @BeforeEach
  void setUp() {
    petriNet =
        new PetriNet(
            mockIncidenceMatrix, mockCurrentMarking, mockTimeRangeMatrix, mockEnableVector);

    when(mockIncidenceMatrix.getTransitions()).thenReturn(10); // Mock total transitions
  }

  @Test
  void fireShouldSucceedWhenTransitionIsEnabledAndInTimeRange() {

    int transitionToFire = 3;

    when(mockEnableVector.isTransitionEnabled(transitionToFire)).thenReturn(true);
    when(mockTimeRangeMatrix.isInsideTimeRange(transitionToFire)).thenReturn(true);

    int[] nextMarking = {1, 0, 1};

    // Return a mock next marking when the state equation is calculated
    try (MockedStatic<StateEquationUtils> mockedUtils =
        Mockito.mockStatic(StateEquationUtils.class)) {
      mockedUtils
          .when(
              () ->
                  StateEquationUtils.calculateStateEquation(
                      transitionToFire, mockIncidenceMatrix, mockCurrentMarking))
          .thenReturn(nextMarking);

      boolean result = petriNet.fire(transitionToFire);

      assertTrue(result, "Fire should return true on success.");

      verify(mockCurrentMarking).setMarking(nextMarking);
      verify(mockEnableVector, Mockito.times(2))
          .updateEnableVector(mockIncidenceMatrix, mockCurrentMarking);
    }
  }

  @Test
  void fireShouldFailWhenTransitionIsNotEnabled() {
    int transitionToFire = 5;

    when(mockEnableVector.isTransitionEnabled(transitionToFire)).thenReturn(false);

    boolean result = petriNet.fire(transitionToFire);

    assertFalse(result, "Fire should return false if transition is not enabled.");

    verify(mockCurrentMarking, never()).setMarking(any());
    verify(mockEnableVector, Mockito.times(1)).updateEnableVector(any(), any());
  }

  @Test
  void fireShouldFailWhenTransitionIsOutOfTimeRange() {
    int transitionToFire = 7;

    when(mockEnableVector.isTransitionEnabled(transitionToFire))
        .thenReturn(true); // It's enabled token-wise
    when(mockTimeRangeMatrix.isInsideTimeRange(transitionToFire))
        .thenReturn(false); // It's not enabled time-wise

    boolean result = petriNet.fire(transitionToFire);

    assertFalse(result, "Fire should return false if transition is out of its time range.");

    verify(mockCurrentMarking, never()).setMarking(any());
    verify(mockEnableVector, Mockito.times(1)).updateEnableVector(any(), any());
  }
}
