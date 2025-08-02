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
    byte[] transitionColumn = incidenceMatrix.getColumn(transition);
    int[] currentMarkingArray = currentMarking.getMarking();
    int[] nextMarking = new int[currentMarkingArray.length];

    for (int i = 0; i < transitionColumn.length; i++) {
      nextMarking[i] = currentMarkingArray[i] + transitionColumn[i];
    }

    return nextMarking;
  }
}
