package edu.unc.petri.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs invariant analysis on a Petri net's incidence matrix.
 *
 * <p>This class computes:
 *
 * <ul>
 *   <li>T-invariants: null space of C (C * x = 0)
 *   <li>P-invariants: null space of C^T (C^T * y = 0)
 *   <li>Place-invariant equations: for each P-invariant column c and current marking M, the
 *       equation sum_i c[i] * M(p_i) = c^T M
 * </ul>
 *
 * <p>The core algorithm is adapted from PIPE2's InvariantAnalysis.
 *
 * @author Manos Papantoniou and Michael Camacho (Original PIPE2 Authors)
 * @author Der Landsknecht (Adaptation for this project)
 * @version 1.0
 * @since 2025-28-08
 */
public class InvariantAnalyzer {

  /**
   * Calculates T-invariants (transition invariants): basis of null space of C. Columns of the
   * returned list are invariant vectors.
   */
  public List<int[]> calculateTransitionInvariants(Matrix incidenceMatrix) {
    Matrix invariantMatrix = findVectors(incidenceMatrix);
    return matrixToInvariantList(invariantMatrix);
  }

  /**
   * Calculates P-invariants (place invariants): basis of null space of C^T. Columns of the returned
   * list are invariant vectors.
   */
  public List<int[]> calculatePlaceInvariants(Matrix incidenceMatrix) {
    Matrix invariantMatrix = findVectors(incidenceMatrix.transpose());
    return matrixToInvariantList(invariantMatrix);
  }

  /**
   * Builds place-invariant equations from P-invariants and the current marking.
   *
   * <p>For each P-invariant column vector <code>c</code> and marking <code>M</code>, creates:
   *
   * <pre>
   *   sum_i c[i] * M(p_i) = c^T * M
   * </pre>
   *
   * <p>Zero coefficients are omitted from the LHS. Optionally normalizes by gcd of |coeffs| and
   * RHS.
   *
   * @param incidenceMatrix Incidence matrix C (rows = places, cols = transitions)
   * @param initialMarking tokens per place; length must equal number of places (rows of C)
   * @return list of {@link PlaceInvariantEquation}
   * @throws IllegalArgumentException if dimensions mismatch
   */
  public List<PlaceInvariantEquation> getPlaceInvariantEquations(
      Matrix incidenceMatrix, int[] initialMarking) {

    int numPlaces = incidenceMatrix.getRowDimension();
    if (initialMarking == null || initialMarking.length != numPlaces) {
      throw new IllegalArgumentException(
          "currentMarking length ("
              + (initialMarking == null ? "null" : initialMarking.length)
              + ") must equal number of places ("
              + numPlaces
              + ")");
    }

    List<int[]> placeInvariants = calculatePlaceInvariants(incidenceMatrix);
    List<PlaceInvariantEquation> equations = new ArrayList<>(placeInvariants.size());

    for (int[] c : placeInvariants) {
      // Sparse LHS coefficients
      Map<Integer, Integer> coeffs = new HashMap<>();
      for (int p = 0; p < c.length; p++) {
        int a = c[p];
        if (a != 0) {
          coeffs.put(p, a);
        }
      }

      // RHS = c^T * M
      long rhs = 0L;
      for (int p = 0; p < c.length; p++) {
        rhs += (long) c[p] * (long) initialMarking[p];
      }

      // Normalize by gcd (optional but makes equations cleaner)
      int gcd = gcdAll(coeffs.values(), Math.abs(rhs));
      if (gcd > 1) {
        Map<Integer, Integer> reduced = new HashMap<>(coeffs.size());
        for (Map.Entry<Integer, Integer> e : coeffs.entrySet()) {
          reduced.put(e.getKey(), e.getValue() / gcd);
        }
        coeffs = reduced;
        rhs /= gcd;
      }

      if (!coeffs.isEmpty()) {
        if (rhs > Integer.MAX_VALUE || rhs < Integer.MIN_VALUE) {
          throw new ArithmeticException(
              "Place invariant RHS exceeds int range after normalization: " + rhs);
        }
        equations.add(new PlaceInvariantEquation(coeffs, (int) rhs));
      }
    }

    return equations;
  }

  /**
   * Finds a minimal generating set of vectors for the null space of matrix C (solves C*x = 0).
   * Returns a matrix whose columns are basis vectors.
   */
  private Matrix findVectors(Matrix incidenceMatrix) {
    int n = incidenceMatrix.getColumnDimension();
    Matrix vectorMatrix = Matrix.identity(n, n);

    // ---------- PHASE 1: Triangulation / elimination ----------
    while (!incidenceMatrix.isZeroMatrix()) {
      if (incidenceMatrix.checkCase11()) {
        // Remove columns that cannot contribute (rows with all non-negative or all non-positive)
        for (int i = 0; i < incidenceMatrix.getRowDimension(); i++) {
          int[] pos1 = incidenceMatrix.getPositiveIndices(i); // 1-based
          int[] neg1 = incidenceMatrix.getNegativeIndices(i); // 1-based
          if (pos1.length == 0 || neg1.length == 0) {
            int[] union1 = uniteSets(pos1, neg1); // 1-based
            for (int j = union1.length - 1; j >= 0; j--) {
              int colIndex0 = union1[j] - 1;
              incidenceMatrix = incidenceMatrix.eliminateCol(colIndex0);
              vectorMatrix = vectorMatrix.eliminateCol(colIndex0);
            }
          }
        }

      } else if (incidenceMatrix.cardinalityCondition() >= 0) {
        // Row has exactly one positive or one negative
        while (incidenceMatrix.cardinalityCondition() >= 0) {
          int row = incidenceMatrix.cardinalityCondition();
          int k0 = incidenceMatrix.cardinalityOne(); // 0-based column index (pivot)
          int[] j1 = incidenceMatrix.colsToUpdate(); // 1-based columns to update
          int[] coeffs = new int[j1.length];
          for (int i = 0; i < j1.length; i++) {
            coeffs[i] = Math.abs(incidenceMatrix.get(row, j1[i] - 1));
          }
          int chk = Math.abs(incidenceMatrix.get(row, k0));

          incidenceMatrix.linearlyCombine(k0, chk, j1, coeffs);
          vectorMatrix.linearlyCombine(k0, chk, j1, coeffs);

          incidenceMatrix = incidenceMatrix.eliminateCol(k0);
          vectorMatrix = vectorMatrix.eliminateCol(k0);
        }

      } else {
        // General reduction step
        int h;
        while ((h = incidenceMatrix.firstNonZeroRowIndex()) > -1) {
          int k0 = incidenceMatrix.firstNonZeroElementIndex(h); // 0-based pivot column
          int chk = incidenceMatrix.get(h, k0);

          int[] chj0 = incidenceMatrix.findRemainingNonZeroIndices(h); // 0-based
          if (isEmptySet(chj0)) {
            incidenceMatrix = incidenceMatrix.eliminateCol(k0);
            vectorMatrix = vectorMatrix.eliminateCol(k0);
            continue;
          }

          int[] otherCoeffs = incidenceMatrix.findRemainingNonZeroCoefficients(h); // values
          int[] alpha = alphaCoef(chk, otherCoeffs);
          int[] beta = betaCoef(chk, otherCoeffs.length);

          incidenceMatrix.linearlyCombine(k0, alpha, chj0, beta);
          vectorMatrix.linearlyCombine(k0, alpha, chj0, beta);

          incidenceMatrix = incidenceMatrix.eliminateCol(k0);
          vectorMatrix = vectorMatrix.eliminateCol(k0);
        }
      }
    }

    // ---------- PHASE 2: Make all elements non-negative ----------
    int h;
    while ((h = vectorMatrix.rowWithNegativeElement()) > -1) {
      int[] pos1 = vectorMatrix.getPositiveIndices(h); // 1-based
      int[] neg1 = vectorMatrix.getNegativeIndices(h); // 1-based

      if (pos1.length > 0 && neg1.length > 0) {
        for (int p1 : pos1) {
          for (int n1 : neg1) {
            int p0 = p1 - 1;
            int n0 = n1 - 1;

            int a = -vectorMatrix.get(h, n0);
            int b = vectorMatrix.get(h, p0);

            Matrix vp = vectorMatrix.getMatrix(0, vectorMatrix.getRowDimension() - 1, p0, p0);
            Matrix vn = vectorMatrix.getMatrix(0, vectorMatrix.getRowDimension() - 1, n0, n0);

            Matrix newCol = new Matrix(vectorMatrix.getRowDimension(), 1);
            for (int r = 0; r < vectorMatrix.getRowDimension(); r++) {
              newCol.set(r, 0, a * vp.get(r, 0) + b * vn.get(r, 0));
            }

            int gcd = newCol.getGreatestCommonDivisor();
            if (gcd > 1) {
              newCol.divideEquals(gcd);
            }

            vectorMatrix = vectorMatrix.appendVector(newCol);
          }
        }
      }

      // Remove columns with negative in row h (1-based indices returned)
      int[] negCols1 = vectorMatrix.getNegativeIndices(h);
      for (int i = negCols1.length - 1; i >= 0; i--) {
        vectorMatrix = vectorMatrix.eliminateCol(negCols1[i] - 1);
      }
    }

    // ---------- PHASE 3: Remove non-minimal support vectors ----------
    int k;
    while ((k = vectorMatrix.findNonMinimal()) > -1) {
      vectorMatrix = vectorMatrix.eliminateCol(k);
    }

    return vectorMatrix;
  }

  /** Convert a matrix whose columns are vectors into a list of int[] vectors. */
  private List<int[]> matrixToInvariantList(Matrix matrix) {
    List<int[]> invariants = new ArrayList<>();
    int rows = matrix.getRowDimension();
    int cols = matrix.getColumnDimension();
    for (int j = 0; j < cols; j++) {
      int[] v = new int[rows];
      for (int i = 0; i < rows; i++) {
        v[i] = matrix.get(i, j);
      }
      invariants.add(v);
    }
    return invariants;
  }

  /** true if set is empty. */
  private boolean isEmptySet(int[] set) {
    return set == null || set.length == 0;
  }

  /** Concatenate two arrays (used with 1-based indices). */
  private int[] uniteSets(int[] a, int[] b) {
    int[] r = new int[(a == null ? 0 : a.length) + (b == null ? 0 : b.length)];
    int k = 0;
    if (a != null) {
      for (int x : a) {
        r[k++] = x;
      }
    }
    if (b != null) {
      for (int x : b) {
        r[k++] = x;
      }
    }
    return r;
  }

  /**
   * alpha[i] = ±|otherCoeffs[i]|, sign chosen to cancel negatives with pivot coeff 'pivotCoeff'.
   * (Matches the PIPE reduction rule in the general case.)
   */
  private int[] alphaCoef(int pivotCoeff, int[] otherCoeffs) {
    int[] alpha = new int[otherCoeffs.length];
    for (int i = 0; i < otherCoeffs.length; i++) {
      int cj = otherCoeffs[i];
      alpha[i] = (pivotCoeff * cj < 0) ? Math.abs(cj) : -Math.abs(cj);
    }
    return alpha;
  }

  /** beta[i] = |pivotCoeff| for all i. */
  private int[] betaCoef(int pivotCoeff, int n) {
    int[] beta = new int[n];
    int a = Math.abs(pivotCoeff);
    for (int i = 0; i < n; i++) {
      beta[i] = a;
    }
    return beta;
  }

  /** gcd of all |values| and rhsAbs; returns at least 1. */
  private int gcdAll(Iterable<Integer> values, long rhsAbs) {
    int g = 0;
    for (int v : values) {
      g = gcd(g, Math.abs(v));
      if (g == 1) {
        break;
      }
    }
    if (rhsAbs != 0) {
      g = gcd(g, (int) (rhsAbs % Integer.MAX_VALUE));
    }
    return Math.max(g, 1);
  }

  private int gcd(int a, int b) {
    a = Math.abs(a);
    b = Math.abs(b);
    if (a == 0) {
      return b;
    }
    if (b == 0) {
      return a;
    }
    while (b != 0) {
      int t = a % b;
      a = b;
      b = t;
    }
    return a;
  }
}
