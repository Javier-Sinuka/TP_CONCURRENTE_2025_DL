package edu.unc.petri.core;

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

  /**
   * Constructs a Petri net with the given incidence matrix, current marking, and enable vector.
   *
   * @param incidenceMatrix the incidence matrix of the Petri net
   * @param currentMarking the current marking of the Petri net
   * @param enableVector the enable vector of the Petri net
   */
  public PetriNet(
      IncidenceMatrix incidenceMatrix, CurrentMarking currentMarking, EnableVector enableVector) {
    this.incidenceMatrix = incidenceMatrix;
    this.currentMarking = currentMarking;
    this.enableVector = enableVector;
  }

  /**
   * Fires a transition in the Petri net. It checks if the transition is enabled and updates the
   * current marking and enable vector
   *
   * @param transitionIndex the index of the transition to fire
   * @return true if the transition was successfully fired, false otherwise
   */
  public boolean fire(int transitionIndex) {
    if (transitionIndex < 0 || transitionIndex >= incidenceMatrix.getTransitions()) {
      throw new IllegalArgumentException("Invalid transition index: " + transitionIndex);
    }

    boolean[] enabledTransitions = enableVector.getEnableVector();

    if (!enabledTransitions[transitionIndex]) {
      return false; // Transition is not enabled
    }

    // Calculate the next marking based on the current marking and the incidence matrix
    currentMarking.setMarking(calculateStateEquation(transitionIndex));

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

  private int[] calculateStateEquation(int transition) {
    byte[] transitionColumn = incidenceMatrix.getColumn(transition);
    int[] currentMarkingArray = currentMarking.getMarking();
    int[] nextMarking = new int[currentMarkingArray.length];

    for (int i = 0; i < transitionColumn.length; i++) {
      nextMarking[i] = currentMarkingArray[i] + transitionColumn[i];
    }

    return nextMarking;
  }
}
