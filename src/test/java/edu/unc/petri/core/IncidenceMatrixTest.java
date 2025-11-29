package edu.unc.petri.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
