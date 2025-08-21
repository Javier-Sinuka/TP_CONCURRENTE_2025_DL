package edu.unc.petri.util;

import edu.unc.petri.core.CurrentMarking;
import edu.unc.petri.core.IncidenceMatrix;

/**
 * This class provides utility methods for calculating the state equation of a Petri net. It
 * includes methods to compute the next marking after firing a transition based on the incidence
 * matrix and the current marking.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-02-08
 */
public class StateEquationUtils {

  /**
   * Calculates the next marking (state equation) of a Petri net after firing a given transition.
   *
   * @param transition the index of the transition to fire
   * @param incidenceMatrix the incidence matrix representing the Petri net
   * @param currentMarking the current marking of the Petri net
   * @return an array representing the next marking after firing the transition
   */
  public static int[] calculateStateEquation(
      int transition, IncidenceMatrix incidenceMatrix, CurrentMarking currentMarking) {
    if (incidenceMatrix == null || currentMarking == null) {
      throw new IllegalArgumentException("The parameter is null");
    }
    if (transition < 0 || transition >= incidenceMatrix.getTransitions()) {
      throw new IllegalArgumentException("Transition index is out of bounds");
    }
    if (incidenceMatrix.getPlaces() == 0
        || incidenceMatrix.getTransitions() == 0
        || currentMarking.getMarking().length == 0) {
      throw new IllegalArgumentException("Parameters size cannot be 0");
    }
    byte[] transitionColumn = incidenceMatrix.getColumn(transition);
    int[] currentMarkingArray = currentMarking.getMarking();
    int[] nextMarking = new int[currentMarkingArray.length];

    for (int i = 0; i < transitionColumn.length; i++) {
      nextMarking[i] = currentMarkingArray[i] + transitionColumn[i];
    }

    return nextMarking;
  }
}
