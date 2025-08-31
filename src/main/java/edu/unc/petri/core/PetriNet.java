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
      EnableVector enableVector,
      Log log) {
    this.incidenceMatrix = incidenceMatrix;
    this.currentMarking = currentMarking;
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

    // String markingBefore = currentMarking.toString();

    // Calculate the next marking based on the current marking and the incidence
    // matrix
    currentMarking.setMarking(
        StateEquationUtils.calculateStateEquation(
            transitionIndex, incidenceMatrix, currentMarking));

    log.logTransition(transitionIndex);

    // String markingAfter = currentMarking.toString();

    // log.logMessage("" + markingBefore + "->" + markingAfter + " by T" + transitionIndex);

    enableVector.updateEnableVector(incidenceMatrix, currentMarking);

    return true;
  }

  /**
   * Resets the Petri net to its initial state for a new simulation run.
   *
   * @param initialMarking The initial marking array from the configuration.
   */
  public void reset(int[] initialMarking) {
    // Restore the token marking to the initial configuration
    this.currentMarking = new CurrentMarking(initialMarking);
    // Recalculate which transitions are enabled from this initial state
    this.enableVector.updateEnableVector(this.incidenceMatrix, this.currentMarking);
  }

  /**
   * Returns an array indicating which transitions are currently enabled token wise.
   *
   * @return a boolean array where each index corresponds to a transition; true if the transition is
   *     enabled token wise, false otherwise
   */
  public boolean[] getTokenEnabledTransitions() {
    return enableVector.getTokenEnabledTransitions();
  }

  /**
   * Returns an array indicating which transitions are currently enabled, considering both token and
   * time constraints.
   *
   * @return a boolean array where each index corresponds to a transition; true if the transition is
   *     enabled, false otherwise
   */
  public boolean[] getTimeEnabledTransitions() {
    boolean[] enabledTransitions = enableVector.getTokenEnabledTransitions();

    for (int i = 0; i < enabledTransitions.length; i++) {
      if (enabledTransitions[i]) {
        try {
          if (!enableVector.isTransitionEnabled(i)) {
            enabledTransitions[i] = false; // Transition time not reached
          }
        } catch (TransitionTimeNotReachedException e) {
          enabledTransitions[i] = false; // Transition time not reached
        }
      }
    }

    return enabledTransitions;
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
