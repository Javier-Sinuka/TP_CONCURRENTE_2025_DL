package edu.unc.petri.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvariantAnalyzerTest {

  private InvariantAnalyzer analyzer;

  @BeforeEach
  void setUp() {
    analyzer = new InvariantAnalyzer();
  }

  // A simple producer-consumer style Petri net
  // T-invariant: {1, 1, 1}
  // P-invariant: {1, 1, 1}
  private Matrix createProducerConsumerMatrix() {
    int[][] data = {{-1, 1, 0}, {1, -1, 0}, {0, 1, -1}};
    // A small correction to make it a valid cycle for T-invariant
    data[0][2] = 1; // T_end -> P_wait
    data[2][1] = -1; // P_done -> T_change
    data[1][2] = 1; // T_end -> P_inside (incorrect, should be from P_done)
    data[2][2] = -1; // T_end consumes from P_done

    int[][] correctData = {
      {-1, 0, 1}, // P_wait
      {1, -1, 0}, // P_inside
      {0, 1, -1} // P_done
    };
    return new Matrix(correctData);
  }

  @Test
  void calculateTransitionInvariants_producerConsumer() {
    Matrix incidenceMatrix = createProducerConsumerMatrix();
    List<int[]> tInvariants = analyzer.calculateTransitionInvariants(incidenceMatrix);
    assertEquals(1, tInvariants.size());
    int[] expected = {1, 1, 1};
    // The result can be a multiple of the fundamental invariant
    int gcd = tInvariants.get(0)[0]; // Assuming first element is non-zero
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i] * gcd, tInvariants.get(0)[i]);
    }
  }

  @Test
  void calculatePlaceInvariants_producerConsumer() {
    Matrix incidenceMatrix = createProducerConsumerMatrix();
    List<int[]> pInvariants = analyzer.calculatePlaceInvariants(incidenceMatrix);
    assertEquals(1, pInvariants.size());
    int[] expected = {1, 1, 1};
    int gcd = pInvariants.get(0)[0];
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i] * gcd, pInvariants.get(0)[i]);
    }
  }

  @Test
  void getPlaceInvariantEquations_producerConsumer() {
    Matrix incidenceMatrix = createProducerConsumerMatrix();
    int[] initialMarking = {1, 0, 0};
    List<PlaceInvariantEquation> equations =
        analyzer.getPlaceInvariantEquations(incidenceMatrix, initialMarking);
    assertEquals(1, equations.size());
    PlaceInvariantEquation eq = equations.get(0);
    assertEquals(1, eq.getResult());
    assertEquals(3, eq.getCoefficients().size()); // P0, P1, P2
  }

  @Test
  void getPlaceInvariantEquations_invalidMarking_throwsException() {
    Matrix incidenceMatrix = createProducerConsumerMatrix();
    int[] invalidMarking = {1, 0};
    assertThrows(
        IllegalArgumentException.class,
        () -> analyzer.getPlaceInvariantEquations(incidenceMatrix, invalidMarking));
  }

  @Test
  void calculateTransitionInvariants_noInvariants() {
    Matrix incidenceMatrix = new Matrix(new int[][] {{-1, 0}, {1, -1}});
    List<int[]> tInvariants = analyzer.calculateTransitionInvariants(incidenceMatrix);
    assertTrue(tInvariants.isEmpty());
  }

  @Test
  void calculatePlaceInvariants_noInvariants() {
    Matrix incidenceMatrix = new Matrix(new int[][] {{-1, 1}, {0, -1}});
    List<int[]> pInvariants = analyzer.calculatePlaceInvariants(incidenceMatrix);
    assertTrue(pInvariants.isEmpty());
  }
}
