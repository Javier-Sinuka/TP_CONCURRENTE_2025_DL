package edu.unc.petri.core;

import edu.unc.petri.exceptions.TransitionTimeNotReachedException;
import edu.unc.petri.util.Log;
import edu.unc.petri.util.StateEquationUtils;

/**
 * The PetriNet class represents a Petri net. It contains the incidence matrix, current marking, and
 * enable vector. It provides methods to fire transitions and check enabled transitions.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public class PetriNet {
  /**
   * The incidence matrix of the Petri net, representing the relationship between places and
   * transitions.
   */
  private IncidenceMatrix incidenceMatrix;

  /** The current marking of the Petri net, representing the number of tokens in each place. */
  private CurrentMarking currentMarking;

  /** The time range matrix of the Petri net, representing the time ranges for each transition. */
  private TimeRangeMatrix timeRangeMatrix;

  /** The enable vector of the Petri net, indicating which transitions are currently enabled. */
  private EnableVector enableVector;

  /** The log for recording transition firings in the simulation. */
  private Log log;

  /**
   * Constructs a Petri net with the given incidence matrix, current marking, and enable vector.
   *
   * @param incidenceMatrix the incidence matrix of the Petri net
   * @param currentMarking the current marking of the Petri net
   * @param enableVector the enable vector of the Petri net
   */
  public PetriNet(
      IncidenceMatrix incidenceMatrix,
      CurrentMarking currentMarking,
      TimeRangeMatrix timeRangeMatrix,
      EnableVector enableVector,
      Log log) {
    this.incidenceMatrix = incidenceMatrix;
    this.currentMarking = currentMarking;
    this.timeRangeMatrix = timeRangeMatrix;
    this.enableVector = enableVector;
    this.log = log;

    this.enableVector.updateEnableVector(incidenceMatrix, currentMarking);
  }

  /**
   * Fires a transition in the Petri net. It checks if the transition is enabled and updates the
   * current marking and enable vector
   *
   * @param transitionIndex the index of the transition to fire
   * @return true if the transition was successfully fired, false otherwise
   */
  public boolean fire(int transitionIndex) throws TransitionTimeNotReachedException {
    if (transitionIndex < 0 || transitionIndex >= incidenceMatrix.getTransitions()) {
      throw new IllegalArgumentException("Invalid transition index: " + transitionIndex);
    }

    if (!enableVector.isTransitionEnabled(transitionIndex)) {
      return false; // Transition is not enabled token wise
    }

    if (!timeRangeMatrix.isInsideTimeRange(transitionIndex)) {
      if (!timeRangeMatrix.isBeforeTimeRange(transitionIndex)) {
        long sleepTime = timeRangeMatrix.getSleepTimeToFire(transitionIndex);

        throw new TransitionTimeNotReachedException(sleepTime);
      }

      return false; // Transition has past its time range
    }

    // String markingBefore = currentMarking.toString();

    // Calculate the next marking based on the current marking and the incidence
    // matrix
    currentMarking.setMarking(
        StateEquationUtils.calculateStateEquation(
            transitionIndex, incidenceMatrix, currentMarking));

    // String markingAfter = currentMarking.toString();

    // log.logMessage("" + markingBefore + "->" + markingAfter + " by T" + transitionIndex);

    enableVector.updateEnableVector(incidenceMatrix, currentMarking);

    return true;
  }

  /**
   * Returns the transitions currently enabled in the Petri net.
   *
   * @return an array of booleans where each index corresponds to a transition and true indicates
   *     that the transition is enabled.
   */
  public boolean[] getEnableTransitions() {
    return enableVector.getEnableVector();
  }

  /**
   * Return the number of places in the Petri net.
   *
   * @return the number of places
   */
  public int getNumberOfPlaces() {
    return incidenceMatrix.getPlaces();
  }

  /**
   * Return the number of transitions in the Petri net.
   *
   * @return the number of transitions
   */
  public int getNumberOfTransitions() {
    return incidenceMatrix.getTransitions();
  }
}
