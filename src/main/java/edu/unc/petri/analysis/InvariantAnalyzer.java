package edu.unc.petri.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs invariant analysis on a Petri net's incidence matrix.
 *
 * <p>This class contains the algorithmic logic to compute T-invariants and P-invariants by finding
 * the null space of the incidence matrix (or its transpose). The algorithm is adapted from the
 * InvariantAnalysis module of the PIPE2 Petri Net Tool.
 *
 * @author Manos Papantoniou & Michael Camacho (Original PIPE2 Authors)
 * @author Der Landsknecht (Adaptation for this project)
 * @version 1.0
 * @since 2025-28-08
 */
public class InvariantAnalyzer {
  /**
   * Calculates the transition invariants (T-invariants) for a given incidence matrix.
   *
   * @param incidenceMatrix The incidence matrix of the Petri net.
   * @return A list of integer arrays, where each array represents a T-invariant vector.
   */
  public List<int[]> calculateTransitionInvariants(Matrix incidenceMatrix) {
    Matrix invariantMatrix = findVectors(incidenceMatrix);
    return matrixToInvariantList(invariantMatrix);
  }

  /**
   * Calculates the place invariants (P-invariants) for a given incidence matrix.
   *
   * @param incidenceMatrix The incidence matrix of the Petri net.
   * @return A list of integer arrays, where each array represents a P-invariant vector.
   */
  public List<int[]> calculatePlaceInvariants(Matrix incidenceMatrix) {
    Matrix invariantMatrix = findVectors(incidenceMatrix.transpose());
    return matrixToInvariantList(invariantMatrix);
  }

  /** Converts a matrix of invariants (where columns are vectors) to a list of arrays. */
  private List<int[]> matrixToInvariantList(Matrix matrix) {
    List<int[]> invariants = new ArrayList<>();
    int rows = matrix.getRowDimension();
    int cols = matrix.getColumnDimension();

    for (int j = 0; j < cols; j++) {
      int[] invariant = new int[rows];
      for (int i = 0; i < rows; i++) {
        invariant[i] = matrix.get(i, j);
      }
      invariants.add(invariant);
    }
    return invariants;
  }

  /**
   * Core algorithm to find the minimal generating set of vectors for the null space of matrix C
   * (solves C*x = 0).
   *
   * @param incidenceMatrix The matrix to analyze.
   * @return A matrix where each column is a basis vector of the null space.
   */
  private Matrix findVectors(Matrix incidenceMatrix) {
    int n = incidenceMatrix.getColumnDimension();
    Matrix vectorMatrix = Matrix.identity(n, n);

    // PHASE 1: Triangulation
    while (!incidenceMatrix.isZeroMatrix()) {
      if (incidenceMatrix.checkCase11()) {
        for (int i = 0; i < incidenceMatrix.getRowDimension(); i++) {
          int[] positiveColumnIndices1Based = incidenceMatrix.getPositiveIndices(i);
          int[] negativeColumnIndices1Based = incidenceMatrix.getNegativeIndices(i);

          if (positiveColumnIndices1Based.length == 0 || negativeColumnIndices1Based.length == 0) {
            int[] unionColumnIndices1Based =
                uniteSets(positiveColumnIndices1Based, negativeColumnIndices1Based);
            for (int j = unionColumnIndices1Based.length - 1; j >= 0; j--) {
              int colIndex = unionColumnIndices1Based[j] - 1;
              incidenceMatrix = incidenceMatrix.eliminateCol(colIndex);
              vectorMatrix = vectorMatrix.eliminateCol(colIndex);
            }
          }
        }
      } else if (incidenceMatrix.cardinalityCondition() >= 0) {
        while (incidenceMatrix.cardinalityCondition() >= 0) {
          int cardinalityRowIndex = incidenceMatrix.cardinalityCondition();
          int k = incidenceMatrix.cardinalityOne();
          int[] j = incidenceMatrix.colsToUpdate();
          int[] columnUpdateCoefficients = new int[j.length];
          for (int i = 0; i < j.length; i++) {
            columnUpdateCoefficients[i] =
                Math.abs(incidenceMatrix.get(cardinalityRowIndex, j[i] - 1));
          }
          incidenceMatrix.linearlyCombine(
              k,
              Math.abs(incidenceMatrix.get(cardinalityRowIndex, k)),
              j,
              columnUpdateCoefficients);
          vectorMatrix.linearlyCombine(
              k,
              Math.abs(incidenceMatrix.get(cardinalityRowIndex, k)),
              j,
              columnUpdateCoefficients);

          incidenceMatrix = incidenceMatrix.eliminateCol(k);
          vectorMatrix = vectorMatrix.eliminateCol(k);
        }
      } else {
        int h;
        while ((h = incidenceMatrix.firstNonZeroRowIndex()) > -1) {
          int k = incidenceMatrix.firstNonZeroElementIndex(h);
          int chk = incidenceMatrix.get(h, k);
          int[] chj = incidenceMatrix.findRemainingNonZeroIndices(h);

          if (isEmptySet(chj)) {
            incidenceMatrix = incidenceMatrix.eliminateCol(k);
            vectorMatrix = vectorMatrix.eliminateCol(k);
            continue;
          }

          int[] updatedColumnCoefficients = incidenceMatrix.findRemainingNonZeroCoefficients(h);
          int[] alpha = alphaCoef(chk, updatedColumnCoefficients);
          int[] beta = betaCoef(chk, updatedColumnCoefficients.length);

          incidenceMatrix.linearlyCombine(k, alpha, chj, beta);
          vectorMatrix.linearlyCombine(k, alpha, chj, beta);

          incidenceMatrix = incidenceMatrix.eliminateCol(k);
          vectorMatrix = vectorMatrix.eliminateCol(k);
        }
      }
    }

    // PHASE 2: Make all elements non-negative
    int h;
    while ((h = vectorMatrix.rowWithNegativeElement()) > -1) {
      int[] positiveIndex1Based = vectorMatrix.getPositiveIndices(h);
      int[] negativeIndex1Based = vectorMatrix.getNegativeIndices(h);

      if (positiveIndex1Based.length > 0 && negativeIndex1Based.length > 0) {
        for (int positiveVeIndex : positiveIndex1Based) {
          for (int negativeVeIndex : negativeIndex1Based) {
            int positiveIdx0 = positiveVeIndex - 1;
            int negativeIdx0 = negativeVeIndex - 1;

            int a = -vectorMatrix.get(h, negativeIdx0);
            int b = vectorMatrix.get(h, positiveIdx0);

            Matrix positiveColumnVector =
                vectorMatrix.getMatrix(
                    0, vectorMatrix.getRowDimension() - 1, positiveIdx0, positiveIdx0);
            Matrix negativeColumnVector =
                vectorMatrix.getMatrix(
                    0, vectorMatrix.getRowDimension() - 1, negativeIdx0, negativeIdx0);

            Matrix newCol = new Matrix(vectorMatrix.getRowDimension(), 1);
            for (int row = 0; row < vectorMatrix.getRowDimension(); row++) {
              newCol.set(
                  row,
                  0,
                  a * positiveColumnVector.get(row, 0) + b * negativeColumnVector.get(row, 0));
            }

            int gcd = newCol.getGreatestCommonDivisor();
            if (gcd > 1) {
              newCol.divideEquals(gcd);
            }
            vectorMatrix = vectorMatrix.appendVector(newCol);
          }
        }
      }

      // Eliminate columns with negative elements in this row
      int[] colsToEliminate = vectorMatrix.getNegativeIndices(h);
      for (int i = colsToEliminate.length - 1; i >= 0; i--) {
        vectorMatrix = vectorMatrix.eliminateCol(colsToEliminate[i] - 1);
      }
    }

    // Remove non-minimal support vectors
    int k;
    while ((k = vectorMatrix.findNonMinimal()) > -1) {
      vectorMatrix = vectorMatrix.eliminateCol(k);
    }
    return vectorMatrix;
  }

  // --- Helper Methods from InvariantAnalysis.txt ---

  private boolean isEmptySet(int[] set) {
    return set.length == 0;
  }

  private int[] uniteSets(int[] firstSet, int[] secondSet) {
    int[] union = new int[firstSet.length + secondSet.length];
    System.arraycopy(firstSet, 0, union, 0, firstSet.length);
    System.arraycopy(secondSet, 0, union, firstSet.length, secondSet.length);
    return union;
  }

  private int[] alphaCoef(int k, int[] j) {
    int[] alpha = new int[j.length];
    for (int i = 0; i < j.length; i++) {
      alpha[i] = (k * j[i] < 0) ? Math.abs(j[i]) : -Math.abs(j[i]);
    }
    return alpha;
  }

  private int[] betaCoef(int chk, int n) {
    int[] beta = new int[n];
    int abschk = Math.abs(chk);
    for (int i = 0; i < n; i++) {
      beta[i] = abschk;
    }
    return beta;
  }
}
