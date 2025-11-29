package edu.unc.petri.core;

/**
 * The IncidenceMatrix class represents the incidence matrix of a Petri net. It provides methods to
 * access the matrix, the number of places and transitions, and to retrieve specific rows, columns,
 * and elements.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public class IncidenceMatrix {
  /** The incidence matrix of the Petri net, represented as a 2D byte array. */
  private final byte[][] matrix;

  /** The number of places in the Petri net. */
  private final int places;

  /** The number of transitions in the Petri net. */
  private final int transitions;

  /**
   * Constructs an IncidenceMatrix from a given path. The matrix is initialized with the number of
   * places and transitions defined in the path.
   *
   * @param matrix A 2D byte array representing the incidence matrix of the Petri net.
   */
  public IncidenceMatrix(byte[][] matrix) {
    if (matrix == null || matrix.length == 0 || matrix[0] == null || matrix[0].length == 0) {
      throw new IllegalArgumentException(
          "Matrix must be non-null and have at least one row and one column");
    }

    // Check for rectangularity
    int numCols = matrix[0].length;
    for (int i = 1; i < matrix.length; i++) {
      if (matrix[i] == null || matrix[i].length != numCols) {
        throw new IllegalArgumentException(
            "All rows in the matrix must have the same number of columns");
      }
    }

    this.matrix = matrix;
    this.places = matrix.length; // Number of places is the number of rows
    this.transitions = matrix[0].length; // Number of transitions is the number of columns
  }

  /**
   * Retrieves the incidence matrix.
   *
   * @return The incidence matrix as a 2D byte array.
   */
  public byte[][] getMatrix() {
    return matrix;
  }

  /**
   * Retrieves the number of places in the Petri net.
   *
   * @return The number of places.
   */
  public int getPlaces() {
    return places;
  }

  /**
   * Retrieves the number of transitions in the Petri net.
   *
   * @return The number of transitions.
   */
  public int getTransitions() {
    return transitions;
  }

  /**
   * Retrieves a specific column from the incidence matrix.
   *
   * @param column The index of the column to retrieve.
   * @return The column as a byte array.
   */
  public byte[] getColumn(int column) {
    if (column < 0 || column >= transitions) {
      throw new IndexOutOfBoundsException("Column index out of bounds");
    }

    byte[] columnData = new byte[getPlaces()];

    for (int i = 0; i < getPlaces(); i++) {
      columnData[i] = matrix[i][column];
    }

    return columnData;
  }

  /**
   * Retrieves a specific element from the incidence matrix.
   *
   * @param row The index of the row.
   * @param column The index of the column.
   * @return The element at the specified row and column as a byte.
   */
  public byte getElement(int row, int column) {
    if (row < 0 || row >= places || column < 0 || column >= transitions) {
      throw new IndexOutOfBoundsException("Row or column index out of bounds");
    }

    return matrix[row][column];
  }
}
