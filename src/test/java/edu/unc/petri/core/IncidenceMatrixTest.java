package edu.unc.petri.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IncidenceMatrixTest {

  private IncidenceMatrix incidenceMatrix;
  private final byte[][] matrix = {
    // T0   T1   T2
    {-1, 1, 0}, // P0
    {1, -1, -1}, // P1
    {0, 0, 1} // P2
  };

  @BeforeEach
  void setUp() {
    incidenceMatrix = new IncidenceMatrix(matrix);
  }

  @Test
  void constructorShouldSetDimensionsCorrectly() {
    assertEquals(3, incidenceMatrix.getPlaces(), "Number of places (rows) should be 3.");
    assertEquals(
        3, incidenceMatrix.getTransitions(), "Number of transitions (columns) should be 3.");
  }

  @Test
  void getColumnShouldReturnCorrectData() {
    byte[] expectedCol1 = {1, -1, 0};
    assertArrayEquals(expectedCol1, incidenceMatrix.getColumn(1));
  }

  @Test
  void getRowShouldReturnCorrectData() {
    byte[] expectedRow1 = {1, -1, -1};
    assertArrayEquals(expectedRow1, incidenceMatrix.getRow(1));
  }

  @Test
  void getInPlacesForTransitionShouldReturnCorrectPlaces() {
    // For T1, places with a negative value in the column are input places.
    // P1 has a value of -1 at T1.
    List<Integer> inPlaces = incidenceMatrix.getInPlacesForTransition(1);
    assertEquals(1, inPlaces.size());
    assertTrue(inPlaces.contains(1));
  }

  @Test
  void getOutPlacesForTransitionShouldReturnCorrectPlaces() {
    // For T1, places with a positive value in the column are output places.
    // P0 has a value of 1 at T1.
    List<Integer> outPlaces = incidenceMatrix.getOutPlacesForTransition(1);
    assertEquals(1, outPlaces.size());
    assertTrue(outPlaces.contains(0));
  }
}
