package edu.unc.petri.core;

import edu.unc.petri.analysis.PetriNetAnalyzer;
import edu.unc.petri.exceptions.NotEqualToPlaceInvariantEquationException;
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

  /** The PetriNetAnalysis associated with this Petri net. */
  private PetriNetAnalyzer petriNetAnalyzer;

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
      PetriNetAnalyzer petriNetAnalyzer,
      Log log) {
    this.incidenceMatrix = incidenceMatrix;
    this.currentMarking = currentMarking;
    this.enableVector = enableVector;
    this.petriNetAnalyzer = petriNetAnalyzer;
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
  public boolean fire(int transitionIndex)
      throws TransitionTimeNotReachedException, NotEqualToPlaceInvariantEquationException {
    if (transitionIndex < 0 || transitionIndex >= incidenceMatrix.getTransitions()) {
      throw new IllegalArgumentException("Invalid transition index: " + transitionIndex);
    }

    if (!enableVector.isTransitionEnabled(transitionIndex)) {
      return false; // Transition is not enabled token wise or time wise or another thread is
      // waiting for the transition
    }

    // Calculate the potential next marking
    int[] nextMarking =
        StateEquationUtils.calculateStateEquation(transitionIndex, incidenceMatrix, currentMarking);

    // Update the current marking and enable vector
    currentMarking.setMarking(nextMarking);
    log.logTransition(transitionIndex);

    // Check place invariants after firing the transition
    petriNetAnalyzer.checkPlaceInvariants(currentMarking.getMarking());

    if (enableVector.isThereThreadWaitingForTransition(transitionIndex)) {
      if (enableVector.getWaitingThreadId(transitionIndex) == Thread.currentThread().getId()) {
        // This transition is enabled both in terms of tokens and timing, and the current thread was
        // previously waiting for it to become time enabled
        enableVector.clearWaitingThreadId(transitionIndex);
      } else {
        throw new IllegalStateException(
            "Another thread is waiting for this transition: " + transitionIndex);
      }
    }

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
    this.enableVector.resetWaitingThreads();
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
   * Return the number of transitions in the Petri net.
   *
   * @return the number of transitions
   */
  public int getNumberOfTransitions() {
    return incidenceMatrix.getTransitions();
  }
}
