package edu.unc.petri.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import edu.unc.petri.core.CurrentMarking;
import edu.unc.petri.core.IncidenceMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StateEquationUtilsTest {

  @Mock private IncidenceMatrix mockIncidenceMatrix;
  @Mock private CurrentMarking mockCurrentMarking;

  @Test
  void calculateStateEquationShouldComputeCorrectNextMarking() {
    int[] currentMarkingArray = {3, 0, 1}; // M_old
    byte[] transitionColumn = {-1, 1, 0}; // Column for the fired transition

    when(mockIncidenceMatrix.getPlaces()).thenReturn(3);
    when(mockIncidenceMatrix.getTransitions()).thenReturn(3);
    when(mockCurrentMarking.getMarking()).thenReturn(currentMarkingArray);
    when(mockIncidenceMatrix.getColumn(anyInt())).thenReturn(transitionColumn);

    // M_new = M_old + I*E
    int[] nextMarking =
        StateEquationUtils.calculateStateEquation(0, mockIncidenceMatrix, mockCurrentMarking);

    int[] expectedMarking = {2, 1, 1}; // {3-1, 0+1, 1+0}
    assertArrayEquals(expectedMarking, nextMarking);
  }

  @Test
  void calculteStateEquationShoulThrowExceptionForNullParameters() {
    assertThrows(
        IllegalArgumentException.class,
        () -> StateEquationUtils.calculateStateEquation(0, null, mockCurrentMarking),
        "IncidenceMatrix should not be null.");
    assertThrows(
        IllegalArgumentException.class,
        () -> StateEquationUtils.calculateStateEquation(0, mockIncidenceMatrix, null),
        "CurrentMarking should not be null.");
  }

  @Test
  void calculateStateEquationShouldThrowExceptionForInvalidTransitionIndex() {
    when(mockIncidenceMatrix.getTransitions()).thenReturn(3);
    assertThrows(
        IllegalArgumentException.class,
        () -> StateEquationUtils.calculateStateEquation(3, mockIncidenceMatrix, mockCurrentMarking),
        "Transition index is out of bounds.");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            StateEquationUtils.calculateStateEquation(-1, mockIncidenceMatrix, mockCurrentMarking),
        "Transition index is out of bounds.");
  }

  @Test
  void calculateStateEquationShouldThrowExceptionForZeroSizeParameters() {
    when(mockIncidenceMatrix.getPlaces()).thenReturn(0);
    when(mockIncidenceMatrix.getTransitions()).thenReturn(3);
    assertThrows(
        IllegalArgumentException.class,
        () -> StateEquationUtils.calculateStateEquation(0, mockIncidenceMatrix, mockCurrentMarking),
        "Parameters size cannot be 0.");
    when(mockIncidenceMatrix.getTransitions()).thenReturn(0);
    assertThrows(
        IllegalArgumentException.class,
        () -> StateEquationUtils.calculateStateEquation(0, mockIncidenceMatrix, mockCurrentMarking),
        "Parameters size cannot be 0.");
    assertThrows(
        IllegalArgumentException.class,
        () -> StateEquationUtils.calculateStateEquation(0, mockIncidenceMatrix, mockCurrentMarking),
        "Parameters size cannot be 0.");
  }
}
