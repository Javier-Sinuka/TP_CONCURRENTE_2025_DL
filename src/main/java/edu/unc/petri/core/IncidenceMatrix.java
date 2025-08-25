package edu.unc.petri.core;

import java.util.ArrayList;

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
   * Retrieves a specific row from the incidence matrix.
   *
   * @param row The index of the row to retrieve.
   * @return The row as a byte array.
   */
  public byte[] getRow(int row) {
    byte[] rowData = new byte[getTransitions()];

    for (int i = 0; i < getPlaces(); i++) {
      rowData[i] = matrix[row][i];
    }

    return rowData;
  }

  /**
   * Retrieves a specific column from the incidence matrix.
   *
   * @param column The index of the column to retrieve.
   * @return The column as a byte array.
   */
  public byte[] getColumn(int column) {
    byte[] columnData = new byte[getPlaces()];

    for (int i = 0; i < getTransitions(); i++) {
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
    return matrix[row][column];
  }

  /**
   * Retrieves the indices of transitions that have an incoming edge to a specific place.
   *
   * @param place The index of the place.
   * @return An ArrayList of integers representing the indices of transitions that have an incoming
   *     edge to the specified place.
   */
  public ArrayList<Integer> getInTransitionsForPlace(int place) {
    ArrayList<Integer> inTransitions = new ArrayList<>();

    for (int i = 0; i < transitions; i++) {
      if (matrix[place][i] > 0) { // Positive value indicates an incoming edge from a transition
        inTransitions.add(i);
      }
    }

    return inTransitions;
  }

  /**
   * Retrieves the indices of transitions that have an outgoing edge from a specific place.
   *
   * @param place The index of the place.
   * @return An ArrayList of integers representing the indices of transitions that have an outgoing
   *     edge from the specified place.
   */
  public ArrayList<Integer> getOutTransitionsForPlace(int place) {
    ArrayList<Integer> outTransitions = new ArrayList<>();

    for (int i = 0; i < transitions; i++) {
      if (matrix[place][i] < 0) { // Negative value indicates an outgoing edge from a place
        outTransitions.add(i);
      }
    }

    return outTransitions;
  }

  /**
   * Retrieves the indices of places that have an incoming edge to a specific transition.
   *
   * @param transition The index of the transition.
   * @return An ArrayList of integers representing the indices of places that have an incoming edge
   *     to the specified transition.
   */
  public ArrayList<Integer> getInPlacesForTransition(int transition) {
    ArrayList<Integer> inPlaces = new ArrayList<>();

    for (int i = 0; i < places; i++) {
      if (matrix[i][transition] < 0) { // Negative value indicates an incoming edge from a place
        inPlaces.add(i);
      }
    }

    return inPlaces;
  }

  /**
   * Retrieves the indices of places that have an outgoing edge from a specific transition.
   *
   * @param transition The index of the transition.
   * @return An ArrayList of integers representing the indices of places that have an outgoing edge
   *     from the specified transition.
   */
  public ArrayList<Integer> getOutPlacesForTransition(int transition) {
    ArrayList<Integer> outPlaces = new ArrayList<>();

    for (int i = 0; i < places; i++) {
      if (matrix[i][transition]
          > 0) { // Positive value indicates an outgoing edge from a transition
        outPlaces.add(i);
      }
    }

    return outPlaces;
  }
}
