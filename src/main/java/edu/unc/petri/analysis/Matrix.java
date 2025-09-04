package edu.unc.petri.analysis;

/**
 * Provides matrix manipulation functions required for Petri net invariant analysis.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-28-08
 */
public class Matrix {

  private final int[][] matrix;
  private final int rowNumber;
  private final int columnSize;

  /**
   * Constructs a matrix with the specified number of rows and columns, initialized to zeros.
   *
   * @param rowNumber the number of rows in the matrix
   * @param columnNumber the number of columns in the matrix
   */
  public Matrix(int rowNumber, int columnNumber) {
    this.rowNumber = rowNumber;
    this.columnSize = columnNumber;
    matrix = new int[rowNumber][columnNumber];
  }

  /**
   * Construct a matrix from a 2-D array.
   *
   * @param matrix Two-dimensional array of integers.
   * @throws IllegalArgumentException All rows must have the same length.
   */
  public Matrix(int[][] matrix) {
    if (matrix == null || matrix.length == 0) {
      this.rowNumber = 0;
      this.columnSize = 0;
      this.matrix = new int[0][0];
      return;
    }
    this.rowNumber = matrix.length;
    this.columnSize = matrix[0].length;
    for (int i = 0; i < rowNumber; i++) {
      if (matrix[i].length != columnSize) {
        throw new IllegalArgumentException("All rows must have the same length.");
      }
    }
    this.matrix = matrix;
  }

  /**
   * Creates an identity matrix of specified dimensions.
   *
   * <p>The identity matrix has ones on its main diagonal and zeros elsewhere. If the matrix is not
   * square, ones are placed on the diagonal where row and column indices are equal.
   *
   * @param rowNumber the number of rows in the matrix
   * @param columnNumber the number of columns in the matrix
   * @return a Matrix object with ones on the diagonal and zeros elsewhere
   */
  public static Matrix identity(int rowNumber, int columnNumber) {
    Matrix matrix = new Matrix(rowNumber, columnNumber);
    int[][] arrayMatrix = matrix.getArray();
    for (int i = 0; i < rowNumber; i++) {
      for (int j = 0; j < columnNumber; j++) {
        arrayMatrix[i][j] = (i == j ? 1 : 0);
      }
    }
    return matrix;
  }

  public int getRowDimension() {
    return rowNumber;
  }

  public int getColumnDimension() {
    return columnSize;
  }

  public int[][] getArray() {
    return matrix;
  }

  /**
   * Returns the value at the specified row and column in the matrix.
   *
   * @param rowIndex the row index of the element to retrieve
   * @param columnIndex the column index of the element to retrieve
   * @return the value at the given row and column
   */
  public int get(int rowIndex, int columnIndex) {
    return matrix[rowIndex][columnIndex];
  }

  /**
   * Sets the value at the specified row and column in the matrix.
   *
   * @param rowIndex the index of the row
   * @param columnIndex the index of the column
   * @param value the value to set at the specified position
   */
  public void set(int rowIndex, int columnIndex, int value) {
    matrix[rowIndex][columnIndex] = value;
  }

  /**
   * Returns a deep copy of this matrix.
   *
   * <p>The returned Matrix object contains a new internal array with the same values as this
   * matrix, ensuring that changes to the copy do not affect the original.
   *
   * @return a new Matrix object that is a deep copy of this matrix
   */
  public Matrix copy() {
    Matrix copiedMatrix = new Matrix(rowNumber, columnSize);
    int[][] arrayMatrix = copiedMatrix.getArray();
    for (int i = 0; i < rowNumber; i++) {
      System.arraycopy(matrix[i], 0, arrayMatrix[i], 0, columnSize);
    }
    return copiedMatrix;
  }

  /**
   * Returns a new Matrix that is the transpose of this matrix. The transpose operation flips the
   * matrix over its diagonal, switching the row and column indices of each element.
   *
   * @return a transposed Matrix
   */
  public Matrix transpose() {
    Matrix transposedMatrix = new Matrix(columnSize, rowNumber);
    int[][] arrayMatrix = transposedMatrix.getArray();
    for (int i = 0; i < rowNumber; i++) {
      for (int j = 0; j < columnSize; j++) {
        arrayMatrix[j][i] = matrix[i][j];
      }
    }
    return transposedMatrix;
  }

  /**
   * Returns true if every element in the matrix is zero.
   *
   * @return true if all elements are zero, false otherwise
   */
  public boolean isZeroMatrix() {
    for (int i = 0; i < rowNumber; i++) {
      for (int j = 0; j < columnSize; j++) {
        if (get(i, j) != 0) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Returns true if all elements in the specified row are zero.
   *
   * @param r the index of the row to check
   * @return true if every element in row {@code r} is zero, false otherwise
   */
  private boolean isZeroRow(int r) {
    for (int j = 0; j < columnSize; j++) {
      if (matrix[r][j] != 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the matrix satisfies condition 1.1 of the invariant calculation algorithm.
   *
   * @return True if a row exists where either all elements are non-negative or all are
   *     non-positive.
   */
  public boolean checkCase11() {
    for (int i = 0; i < rowNumber; i++) {
      boolean hasPositive = false;
      boolean hasNegative = false;
      for (int j = 0; j < columnSize; j++) {
        if (matrix[i][j] > 0) {
          hasPositive = true;
        }
        if (matrix[i][j] < 0) {
          hasNegative = true;
        }
      }
      if ((!hasPositive || !hasNegative) && !isZeroRow(i)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if a matrix has a row where the count of positive or negative elements is exactly 1.
   *
   * @return The index of the first row that satisfies the condition, or -1 if none.
   */
  public int cardinalityCondition() {
    for (int i = 0; i < rowNumber; i++) {
      int positiveCount = 0;
      int negativeCount = 0;
      for (int j = 0; j < columnSize; j++) {
        if (matrix[i][j] > 0) {
          positiveCount++;
        }
        if (matrix[i][j] < 0) {
          negativeCount++;
        }
      }
      if (positiveCount == 1 || negativeCount == 1) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Finds the column index of an element in a row that has a cardinality of 1 (either 1 positive or
   * 1 negative element).
   *
   * @return The column index, or -1 if no such row is found.
   */
  public int cardinalityOne() {
    int cardRow = cardinalityCondition();
    if (cardRow == -1) {
      return -1;
    }

    int positiveCount = 0;
    int negativeCount = 0;
    int positiveIndex = -1;
    int negativeIndex = -1;

    for (int j = 0; j < columnSize; j++) {
      if (matrix[cardRow][j] > 0) {
        positiveCount++;
        positiveIndex = j;
      }
      if (matrix[cardRow][j] < 0) {
        negativeCount++;
        negativeIndex = j;
      }
    }

    if (positiveCount == 1) {
      return positiveIndex;
    }
    if (negativeCount == 1) {
      return negativeIndex;
    }

    return -1;
  }

  /**
   * Gets the column indices to be updated via linear combination based on a row with cardinality 1.
   *
   * @return An array of column indices.
   */
  public int[] colsToUpdate() {
    int cardRow = cardinalityCondition();
    if (cardRow == -1) {
      return new int[0];
    }

    if (getPositiveIndices(cardRow).length == 1) {
      return getNegativeIndices(cardRow);
    } else {
      return getPositiveIndices(cardRow);
    }
  }

  /**
   * Returns the column indices of all positive elements for a given row.
   *
   * @param rowNumber The row to inspect.
   * @return An integer array of 1-based indices.
   */
  public int[] getPositiveIndices(int rowNumber) {
    int[] positives = new int[columnSize];
    int count = 0;
    for (int j = 0; j < columnSize; j++) {
      if (matrix[rowNumber][j] > 0) {
        positives[count++] = j + 1;
      }
    }
    int[] result = new int[count];
    System.arraycopy(positives, 0, result, 0, count);
    return result;
  }

  /**
   * Returns the column indices of all negative elements for a given row.
   *
   * @param rowNumber The row to inspect.
   * @return An integer array of 1-based indices.
   */
  public int[] getNegativeIndices(int rowNumber) {
    int[] negatives = new int[columnSize];
    int count = 0;
    for (int j = 0; j < columnSize; j++) {
      if (matrix[rowNumber][j] < 0) {
        negatives[count++] = j + 1;
      }
    }
    int[] result = new int[count];
    System.arraycopy(negatives, 0, result, 0, count);
    return result;
  }

  /**
   * Eliminates a column from the matrix.
   *
   * @param columnToDelete The 0-based index of the column to delete.
   * @return A new matrix with the specified column removed.
   */
  public Matrix eliminateCol(int columnToDelete) {
    if (columnToDelete < 0 || columnToDelete >= columnSize) {
      return this;
    }
    Matrix reduced = new Matrix(rowNumber, columnSize - 1);
    for (int i = 0; i < rowNumber; i++) {
      int newCol = 0;
      for (int j = 0; j < columnSize; j++) {
        if (j != columnToDelete) {
          reduced.set(i, newCol++, get(i, j));
        }
      }
    }
    return reduced;
  }

  /**
   * Adds a linear combination of column k to other columns in the matrix. This operation modifies
   * the target columns by adding a scaled version of column k. Useful for matrix transformations
   * such as Gaussian elimination.
   *
   * @param k the index of the source column to combine
   * @param chk the scaling factor for the source column
   * @param j the indices of the target columns to be updated (1-based)
   * @param columnCoefficients the coefficients for each target column (same length as j)
   */
  public void linearlyCombine(int k, int chk, int[] j, int[] columnCoefficients) {
    for (int i = 0; i < j.length; i++) {
      if (j[i] != 0) {
        int colToUpdate = j[i] - 1;
        int chj = columnCoefficients[i];
        for (int row = 0; row < rowNumber; row++) {
          int val = chj * get(row, k) + chk * get(row, colToUpdate);
          set(row, colToUpdate, val);
        }
      }
    }
  }

  /**
   * Performs a linear combination of columns in the matrix. For each index i in the array j, if
   * j[i] != 0, updates column j[i] by setting each element: set(row, j[i], alpha[i] * get(row, k) +
   * beta[i] * get(row, j[i])) for all rows.
   *
   * @param k the index of the column to combine from
   * @param alpha array of multipliers for column k
   * @param j array of column indices to update
   * @param beta array of multipliers for columns j[i]
   */
  public void linearlyCombine(int k, int[] alpha, int[] j, int[] beta) {
    for (int i = 0; i < j.length; i++) {
      if (j[i] != 0) {
        int colToUpdate = j[i];
        for (int row = 0; row < rowNumber; row++) {
          int val = alpha[i] * get(row, k) + beta[i] * get(row, colToUpdate);
          set(row, colToUpdate, val);
        }
      }
    }
  }

  /**
   * Finds the index of the first row that is not all zeros.
   *
   * @return The 0-based row index, or -1 if the matrix is all zeros.
   */
  public int firstNonZeroRowIndex() {
    for (int i = 0; i < rowNumber; i++) {
      if (!isZeroRow(i)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Finds the column index of the first non-zero element in a given row.
   *
   * @return The 0-based column index, or -1 if the row is all zeros.
   */
  public int firstNonZeroElementIndex(int h) {
    for (int j = 0; j < columnSize; j++) {
      if (get(h, j) != 0) {
        return j;
      }
    }
    return -1;
  }

  /**
   * Finds the indices of non-zero elements in row {@code h}, skipping the first non-zero element.
   *
   * @param rowIndex the row index to search for non-zero elements
   * @return an array of column indices where non-zero elements are found, excluding the first
   *     non-zero
   */
  public int[] findRemainingNonZeroIndices(int rowIndex) {
    int[] k = new int[columnSize];
    int count = 0;
    boolean firstSkipped = false;
    for (int j = 0; j < columnSize; j++) {
      if (get(rowIndex, j) != 0) {
        if (firstSkipped) {
          k[count++] = j;
        } else {
          firstSkipped = true;
        }
      }
    }
    int[] result = new int[count];
    System.arraycopy(k, 0, result, 0, count);
    return result;
  }

  /**
   * Finds and returns the remaining non-zero coefficients in the specified row, skipping the first
   * non-zero coefficient encountered.
   *
   * @param rowIndex the row index to search for non-zero coefficients
   * @return an array containing all non-zero coefficients in row {@code h}, except the first one
   */
  public int[] findRemainingNonZeroCoefficients(int rowIndex) {
    int[] k = new int[columnSize];
    int count = 0;
    boolean firstSkipped = false;
    for (int j = 0; j < columnSize; j++) {
      if (get(rowIndex, j) != 0) {
        if (firstSkipped) {
          k[count++] = get(rowIndex, j);
        } else {
          firstSkipped = true;
        }
      }
    }
    int[] result = new int[count];
    System.arraycopy(k, 0, result, 0, count);
    return result;
  }

  /**
   * Returns the index of the first row that contains a negative element. Iterates through each row
   * and column, checking for negative values.
   *
   * @return the index of the first row with a negative element, or -1 if none found
   */
  public int rowWithNegativeElement() {
    for (int i = 0; i < rowNumber; i++) {
      for (int j = 0; j < columnSize; j++) {
        if (get(i, j) < 0) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Returns a submatrix from this matrix, specified by the given row and column indices.
   *
   * @param i0 the starting row index (inclusive)
   * @param i1 the ending row index (inclusive)
   * @param j0 the starting column index (inclusive)
   * @param j1 the ending column index (inclusive)
   * @return a new Matrix containing the specified submatrix
   * @throws ArrayIndexOutOfBoundsException if the indices are out of bounds
   */
  public Matrix getMatrix(int i0, int i1, int j0, int j1) {
    Matrix x = new Matrix(i1 - i0 + 1, j1 - j0 + 1);
    try {
      for (int i = i0; i <= i1; i++) {
        System.arraycopy(matrix[i], j0, x.matrix[i - i0], 0, j1 - j0 + 1);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException("Submatrix indices");
    }
    return x;
  }

  /**
   * Appends a column vector to the right side of this matrix.
   *
   * @param x the column vector to append; must have the same number of rows as this matrix
   * @return a new Matrix with the vector appended as the last column
   */
  public Matrix appendVector(Matrix x) {
    Matrix r = new Matrix(rowNumber, columnSize + 1);
    for (int i = 0; i < rowNumber; i++) {
      System.arraycopy(this.matrix[i], 0, r.matrix[i], 0, columnSize);
      r.set(i, columnSize, x.get(i, 0));
    }
    return r;
  }

  private int gcd2(int a, int b) {
    return b == 0 ? a : gcd2(b, a % b);
  }

  /**
   * Calculates the greatest common divisor (GCD) of the absolute values in the first column of the
   * matrix for all rows.
   *
   * @return the GCD of the first column values, or 0 if there are no rows
   */
  public int getGreatestCommonDivisor() {
    if (rowNumber == 0) {
      return 0;
    }
    int result = Math.abs(matrix[0][0]);
    for (int i = 1; i < rowNumber; i++) {
      result = gcd2(result, Math.abs(matrix[i][0]));
    }
    return result;
  }

  /**
   * Divides each element of the matrix by the specified value. If the divisor is zero, the
   * operation is skipped.
   *
   * @param s the value to divide each matrix element by
   */
  public void divideEquals(int s) {
    if (s == 0) {
      return;
    }
    for (int i = 0; i < rowNumber; i++) {
      for (int j = 0; j < columnSize; j++) {
        matrix[i][j] /= s;
      }
    }
  }

  /**
   * Returns a matrix indicating the row indices (1-based) of non-zero elements. For each element in
   * the original matrix, if it is non-zero, the corresponding element in the returned matrix will
   * be set to its row index + 1; otherwise, it will be 0.
   *
   * @return a Matrix with non-zero elements replaced by their row indices (1-based), zeros
   *     elsewhere
   */
  public Matrix nonZeroIndices() {
    Matrix x = new Matrix(rowNumber, columnSize);
    for (int i = 0; i < rowNumber; i++) {
      for (int j = 0; j < columnSize; j++) {
        x.set(i, j, get(i, j) == 0 ? 0 : i + 1);
      }
    }
    return x;
  }

  /**
   * Finds the index of a non-minimal column in the matrix. A column is considered non-minimal if
   * there exists another column that is a proper subset of it (i.e., all non-zero entries in one
   * column are also non-zero in the other, and at least one entry is zero in the subset column
   * where the other has a non-zero value).
   *
   * @return the index of the non-minimal column if found, otherwise -1
   */
  public int findNonMinimal() {
    for (int j = 0; j < columnSize; j++) {
      for (int k = 0; k < columnSize; k++) {
        if (j == k) {
          continue;
        }
        boolean isSubset = true;
        boolean isProperSubset = false;
        for (int i = 0; i < rowNumber; i++) {
          if (matrix[i][k] != 0 && matrix[i][j] == 0) {
            isSubset = false;
            break;
          }
          if (matrix[i][k] == 0 && matrix[i][j] != 0) {
            isProperSubset = true;
          }
        }
        if (isSubset && isProperSubset) {
          return j;
        }
      }
    }
    return -1;
  }
}
