package edu.unc.petri.analysis;

import edu.unc.petri.core.CurrentMarking;
import edu.unc.petri.core.IncidenceMatrix;
import edu.unc.petri.exceptions.NotEqualToPlaceInvariantEquationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Facade for performing various analyses on a Petri net model.
 *
 * <p>This class provides high-level methods to access analysis results, such as T-invariants and
 * P-invariants, abstracting the underlying calculation logic.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-28-08
 */
public class PetriNetAnalyzer {

  /** The incidence matrix representing the Petri net structure. */
  private final IncidenceMatrix incidenceMatrix;

  /** Analyzer for computing invariants in the Petri net. */
  private final InvariantAnalyzer invariantAnalyzer;

  /** The initial marking of the Petri net. */
  private final CurrentMarking initialMarking;

  /** Cache for computed transition invariants. */
  private List<ArrayList<Integer>> cachedTransitionInvariants;

  /** Cache for computed place invariants. */
  private List<ArrayList<Integer>> cachedPlaceInvariants;

  /** Cache for computed place invariant equations. */
  private List<PlaceInvariantEquation> cachedPlaceInvariantEquations;

  /**
   * Constructs a PetriNetAnalyzer for a given incidence matrix.
   *
   * @param invariantAnalyzer The invariant analyzer to use for calculations.
   * @param incidenceMatrix The incidence matrix of the Petri net to analyze.
   */
  public PetriNetAnalyzer(
      InvariantAnalyzer invariantAnalyzer,
      IncidenceMatrix incidenceMatrix,
      CurrentMarking initialMarking) {
    if (incidenceMatrix == null) {
      throw new IllegalArgumentException("Incidence matrix cannot be null.");
    }
    if (initialMarking == null) {
      throw new IllegalArgumentException("Current marking cannot be null.");
    }

    this.invariantAnalyzer = invariantAnalyzer;
    this.incidenceMatrix = incidenceMatrix;
    this.initialMarking = initialMarking;
  }

  /**
   * Returns the incidence matrix used by this analyzer.
   *
   * @return The IncidenceMatrix instance.
   */
  public IncidenceMatrix getIncidenceMatrix() {
    return incidenceMatrix;
  }

  /**
   * Calculates and returns the transition invariants (T-invariants) of the Petri net.
   *
   * <p>An invariant is represented as a list of 0-based indices corresponding to the transitions
   * that form the invariant. The calculation is performed once and cached for subsequent calls.
   *
   * @return A list where each element is an ArrayList of transition indices representing a
   *     T-invariant.
   */
  public List<ArrayList<Integer>> getTransitionInvariants() {
    if (cachedTransitionInvariants == null) {
      Matrix intMatrix = convertToIntegerMatrix(incidenceMatrix.getMatrix());
      List<int[]> invariantVectors = invariantAnalyzer.calculateTransitionInvariants(intMatrix);
      cachedTransitionInvariants = formatInvariants(invariantVectors);
    }

    return cachedTransitionInvariants;
  }

  /**
   * Calculates and returns the place invariants (P-invariants) of the Petri net.
   *
   * <p>An invariant is represented as a list of 0-based indices corresponding to the places that
   * form the invariant. The calculation is performed once and cached for subsequent calls.
   *
   * @return A list where each element is an ArrayList of place indices representing a P-invariant.
   */
  public List<ArrayList<Integer>> getPlaceInvariants() {
    if (cachedPlaceInvariants == null) {
      Matrix intMatrix = convertToIntegerMatrix(incidenceMatrix.getMatrix());
      List<int[]> invariantVectors = invariantAnalyzer.calculatePlaceInvariants(intMatrix);
      cachedPlaceInvariants = formatInvariants(invariantVectors);
    }

    return cachedPlaceInvariants;
  }

  /**
   * Computes and returns the place invariant equations for the Petri net.
   *
   * <p>Place invariants are linear equations that describe the conservation of tokens across places
   * in the net, regardless of transition firings. This method uses the current incidence matrix and
   * initial marking to analyze and generate all valid place invariant equations. Results are cached
   * for efficiency.
   *
   * @return a list of {@link PlaceInvariantEquation} objects representing the place invariants
   */
  public List<PlaceInvariantEquation> getPlaceInvariantEquations() {
    if (cachedPlaceInvariantEquations == null) {
      Matrix intMatrix = convertToIntegerMatrix(incidenceMatrix.getMatrix());
      int[] initialMarking = this.initialMarking.getMarking();
      List<PlaceInvariantEquation> invariantEquations =
          invariantAnalyzer.getPlaceInvariantEquations(intMatrix, initialMarking);
      cachedPlaceInvariantEquations = invariantEquations;
    }

    return cachedPlaceInvariantEquations;
  }

  /**
   * Validates that the given marking satisfies all place invariant equations of the Petri net.
   *
   * @param marking an array representing the current marking of the Petri net (tokens in each
   *     place)
   * @return true if all place invariant equations are satisfied
   * @throws NotEqualToPlaceInvariantEquationException if any equation is violated by the marking
   */
  public boolean checkPlaceInvariants(int[] marking)
      throws NotEqualToPlaceInvariantEquationException {
    List<PlaceInvariantEquation> equations = getPlaceInvariantEquations();

    for (PlaceInvariantEquation equation : equations) {
      if (!equation.testPlaceInvariant(marking)) {
        throw new NotEqualToPlaceInvariantEquationException();
      }
    }

    return true; // All equations are satisfied
  }

  /** Converts a byte[][] matrix to an int[][] matrix. */
  private Matrix convertToIntegerMatrix(byte[][] byteMatrix) {
    if (byteMatrix == null || byteMatrix.length == 0) {
      return new Matrix(0, 0);
    }

    int rows = byteMatrix.length;
    int cols = byteMatrix[0].length;

    int[][] intMatrix = new int[rows][cols];
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        intMatrix[i][j] = byteMatrix[i][j];
      }
    }

    return new Matrix(intMatrix);
  }

  /**
   * Converts the raw invariant vectors into a list of index lists. Each non-zero element in a
   * vector indicates that the corresponding transition/place (by its index) is part of the
   * invariant.
   *
   * @param invariantVectors The raw vectors from the analyzer.
   * @return A list of lists of indices.
   */
  private List<ArrayList<Integer>> formatInvariants(List<int[]> invariantVectors) {
    List<ArrayList<Integer>> formattedInvariants = new ArrayList<>();
    if (invariantVectors == null) {
      return formattedInvariants;
    }

    for (int[] vector : invariantVectors) {
      ArrayList<Integer> currentInvariant = new ArrayList<>();
      for (int i = 0; i < vector.length; i++) {
        // A non-zero value means the transition/place at this index is part of the invariant.
        if (vector[i] != 0) {
          currentInvariant.add(i);
        }
      }
      if (!currentInvariant.isEmpty()) {
        formattedInvariants.add(currentInvariant);
      }
    }

    return formattedInvariants;
  }
}
