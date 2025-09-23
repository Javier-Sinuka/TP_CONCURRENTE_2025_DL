package edu.unc.petri.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaceInvariantEquationTest {

  @Test
  void constructor_nullCoefficients_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> new PlaceInvariantEquation(null, 1));
  }

  @Test
  void constructor_emptyCoefficients_throwsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PlaceInvariantEquation(Collections.emptyMap(), 1));
  }

  @Test
  void constructor_negativeResult_throwsException() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(0, 1);
    assertThrows(IllegalArgumentException.class, () -> new PlaceInvariantEquation(coeffs, -1));
  }

  @Test
  void constructor_nullKeyInCoefficients_throwsException() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(null, 1);
    assertThrows(NullPointerException.class, () -> new PlaceInvariantEquation(coeffs, 1));
  }

  @Test
  void constructor_nullValueInCoefficients_throwsException() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(0, null);
    assertThrows(NullPointerException.class, () -> new PlaceInvariantEquation(coeffs, 1));
  }

  @Test
  void constructor_negativePlaceIndex_throwsException() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(-1, 1);
    assertThrows(IllegalArgumentException.class, () -> new PlaceInvariantEquation(coeffs, 1));
  }

  @Test
  void constructor_filtersZeroCoefficients() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(0, 1);
    coeffs.put(1, 0);
    PlaceInvariantEquation equation = new PlaceInvariantEquation(coeffs, 1);
    assertEquals(1, equation.getCoefficients().size());
    assertTrue(equation.getCoefficients().containsKey(0));
  }

  @Test
  void testPlaceInvariant_validMarking_returnsTrue() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(0, 2);
    coeffs.put(1, 1);
    PlaceInvariantEquation equation = new PlaceInvariantEquation(coeffs, 5);
    assertTrue(equation.testPlaceInvariant(new int[] {2, 1}));
  }

  @Test
  void testPlaceInvariant_invalidMarking_returnsFalse() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(0, 2);
    coeffs.put(1, 1);
    PlaceInvariantEquation equation = new PlaceInvariantEquation(coeffs, 5);
    assertFalse(equation.testPlaceInvariant(new int[] {1, 1}));
  }

  @Test
  void testPlaceInvariant_nullMarking_throwsException() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(0, 1);
    PlaceInvariantEquation equation = new PlaceInvariantEquation(coeffs, 1);
    assertThrows(IllegalArgumentException.class, () -> equation.testPlaceInvariant(null));
  }

  @Test
  void testPlaceInvariant_emptyMarking_throwsException() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(0, 1);
    PlaceInvariantEquation equation = new PlaceInvariantEquation(coeffs, 1);
    assertThrows(IllegalArgumentException.class, () -> equation.testPlaceInvariant(new int[0]));
  }

  @Test
  void testPlaceInvariant_markingTooSmall_throwsException() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(1, 1);
    PlaceInvariantEquation equation = new PlaceInvariantEquation(coeffs, 1);
    assertThrows(IllegalArgumentException.class, () -> equation.testPlaceInvariant(new int[] {1}));
  }

  @Test
  void getCoefficients_isUnmodifiable() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(0, 1);
    PlaceInvariantEquation equation = new PlaceInvariantEquation(coeffs, 1);
    assertThrows(UnsupportedOperationException.class, () -> equation.getCoefficients().put(1, 2));
  }

  @Test
  void getResult_returnsCorrectResult() {
    PlaceInvariantEquation equation = new PlaceInvariantEquation(Collections.singletonMap(0, 1), 5);
    assertEquals(5, equation.getResult());
  }

  @Test
  void toString_formatsCorrectly() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(0, 2);
    coeffs.put(2, 1);
    coeffs.put(3, -1);
    PlaceInvariantEquation equation = new PlaceInvariantEquation(coeffs, 10);
    assertEquals("2*M(P0) + M(P2) + -M(P3) = 10", equation.toString());
  }

  @Test
  void formatEquation_withCustomNames() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(0, 1);
    coeffs.put(1, 2);
    PlaceInvariantEquation equation = new PlaceInvariantEquation(coeffs, 3);
    String[] placeNames = {"Input", "Buffer"};
    assertEquals("M(Input) + 2*M(Buffer) = 3", equation.formatEquation(placeNames));
  }

  @Test
  void formatEquation_nullNames() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(0, 1);
    PlaceInvariantEquation equation = new PlaceInvariantEquation(coeffs, 1);
    assertEquals("M(p0) = 1", equation.formatEquation(null));
  }

  @Test
  void formatEquation_namesTooSmall() {
    Map<Integer, Integer> coeffs = new HashMap<>();
    coeffs.put(1, 1);
    PlaceInvariantEquation equation = new PlaceInvariantEquation(coeffs, 1);
    String[] placeNames = {"OnlyOneName"};
    assertEquals("M(p1) = 1", equation.formatEquation(placeNames));
  }
}
