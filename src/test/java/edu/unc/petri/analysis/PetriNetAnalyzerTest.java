package edu.unc.petri.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unc.petri.core.CurrentMarking;
import edu.unc.petri.core.IncidenceMatrix;
import edu.unc.petri.exceptions.NotEqualToPlaceInvariantEquationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PetriNetAnalyzerTest {

  private InvariantAnalyzer mockInvariantAnalyzer;
  private IncidenceMatrix incidenceMatrix;
  private CurrentMarking initialMarking;
  private PetriNetAnalyzer analyzer;

  @BeforeEach
  void setUp() {
    mockInvariantAnalyzer = mock(InvariantAnalyzer.class);
    incidenceMatrix = new IncidenceMatrix(new byte[][] {{1, -1}, {-1, 1}});
    initialMarking = new CurrentMarking(new int[] {1, 0});
    analyzer = new PetriNetAnalyzer(mockInvariantAnalyzer, incidenceMatrix, initialMarking);
  }

  @Test
  void constructor_nullIncidenceMatrix_throwsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PetriNetAnalyzer(mockInvariantAnalyzer, null, initialMarking));
  }

  @Test
  void constructor_nullInitialMarking_throwsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PetriNetAnalyzer(mockInvariantAnalyzer, incidenceMatrix, null));
  }

  @Test
  void getIncidenceMatrix_returnsCorrectMatrix() {
    assertEquals(incidenceMatrix, analyzer.getIncidenceMatrix());
  }

  @Test
  void getTransitionInvariants_calculatesAndCaches() {
    List<int[]> rawInvariants = new ArrayList<>();
    rawInvariants.add(new int[] {1, 1});
    when(mockInvariantAnalyzer.calculateTransitionInvariants(org.mockito.ArgumentMatchers.any()))
        .thenReturn(rawInvariants);

    // First call - should calculate
    List<ArrayList<Integer>> invariants = analyzer.getTransitionInvariants();
    assertEquals(1, invariants.size());
    assertEquals(2, invariants.get(0).size());
    verify(mockInvariantAnalyzer, times(1))
        .calculateTransitionInvariants(org.mockito.ArgumentMatchers.any());

    // Second call - should be cached
    analyzer.getTransitionInvariants();
    verify(mockInvariantAnalyzer, times(1))
        .calculateTransitionInvariants(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void getPlaceInvariants_calculatesAndCaches() {
    List<int[]> rawInvariants = new ArrayList<>();
    rawInvariants.add(new int[] {1, 1});
    when(mockInvariantAnalyzer.calculatePlaceInvariants(org.mockito.ArgumentMatchers.any()))
        .thenReturn(rawInvariants);

    // First call
    List<ArrayList<Integer>> invariants = analyzer.getPlaceInvariants();
    assertEquals(1, invariants.size());
    assertEquals(2, invariants.get(0).size());
    verify(mockInvariantAnalyzer, times(1))
        .calculatePlaceInvariants(org.mockito.ArgumentMatchers.any());

    // Second call
    analyzer.getPlaceInvariants();
    verify(mockInvariantAnalyzer, times(1))
        .calculatePlaceInvariants(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void getPlaceInvariantEquations_calculatesAndCaches() {
    PlaceInvariantEquation mockEquation = mock(PlaceInvariantEquation.class);
    List<PlaceInvariantEquation> equations = Collections.singletonList(mockEquation);
    when(mockInvariantAnalyzer.getPlaceInvariantEquations(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(equations);

    // First call
    List<PlaceInvariantEquation> result = analyzer.getPlaceInvariantEquations();
    assertEquals(equations, result);
    verify(mockInvariantAnalyzer, times(1))
        .getPlaceInvariantEquations(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

    // Second call
    analyzer.getPlaceInvariantEquations();
    verify(mockInvariantAnalyzer, times(1))
        .getPlaceInvariantEquations(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void checkPlaceInvariants_validMarking_returnsTrue() throws Exception {
    PlaceInvariantEquation mockEquation = mock(PlaceInvariantEquation.class);
    when(mockEquation.testPlaceInvariant(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    List<PlaceInvariantEquation> equations = Collections.singletonList(mockEquation);
    when(mockInvariantAnalyzer.getPlaceInvariantEquations(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(equations);

    assertTrue(analyzer.checkPlaceInvariants(new int[] {1, 0}));
  }

  @Test
  void checkPlaceInvariants_invalidMarking_throwsException() {
    PlaceInvariantEquation mockEquation = mock(PlaceInvariantEquation.class);
    when(mockEquation.testPlaceInvariant(org.mockito.ArgumentMatchers.any())).thenReturn(false);
    List<PlaceInvariantEquation> equations = Collections.singletonList(mockEquation);
    when(mockInvariantAnalyzer.getPlaceInvariantEquations(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(equations);

    assertThrows(
        NotEqualToPlaceInvariantEquationException.class,
        () -> analyzer.checkPlaceInvariants(new int[] {1, 1}));
  }
}
