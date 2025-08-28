package edu.unc.petri.simulation;

import edu.unc.petri.exceptions.SimulationLimitReachedException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the simulation lifecycle by tracking the completion of transition invariants.
 *
 * <p>This class stops the simulation after a predefined number of T-invariants have been satisfied.
 * It is designed to be thread-safe.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-28-08
 */
public class SimulationManager {

  private final List<ArrayList<Integer>> originalInvariants;
  private List<ArrayList<Integer>> workingInvariants;
  private final int invariantLimit;
  private int invariantCounter;
  private final boolean limitEnabled;

  /**
   * Constructs a SimulationManager.
   *
   * @param transitionInvariants A list of T-invariants, where each is a list of transition indices.
   * @param limit The number of invariants to satisfy before stopping. A value less than 1 disables
   *     the limit.
   */
  public SimulationManager(List<ArrayList<Integer>> transitionInvariants, int limit) {
    if (transitionInvariants == null) {
      throw new IllegalArgumentException("Transition invariants list cannot be null.");
    }
    this.originalInvariants = deepCopy(transitionInvariants);
    this.invariantLimit = limit;
    this.invariantCounter = 0;
    this.limitEnabled = limit > 0 && !originalInvariants.isEmpty();

    // Initialize the working copy
    resetWorkingInvariants();
  }

  /**
   * Checks if the simulation's invariant limit has been reached.
   *
   * @return {@code true} if the limit is enabled and has been reached, otherwise {@code false}.
   */
  public synchronized boolean isInvariantLimitReached() {
    if (!limitEnabled) {
      return false; // If limit is not set, simulation never stops based on this.
    }

    return invariantCounter >= invariantLimit;
  }

  /**
   * Updates the state of invariants based on a fired transition.
   *
   * <p>This method should be called after each successful transition firing. It removes the
   * transition from the working set of invariants and checks if any invariant has been completed.
   *
   * @param transition The 0-based index of the transition that was fired.
   */
  public synchronized void updateInvariant(int transition) throws SimulationLimitReachedException {
    if (!limitEnabled || isInvariantLimitReached()) {
      throw new SimulationLimitReachedException();
    }

    // Iterate through each invariant in the working copy
    for (ArrayList<Integer> currentInvariant : workingInvariants) {
      // Remove the fired transition. Use Integer.valueOf() to remove the object,
      // not the element at a given index.
      currentInvariant.remove(Integer.valueOf(transition));

      // Check if the invariant is now empty (i.e., satisfied)
      if (currentInvariant.isEmpty()) {
        invariantCounter++;
        System.out.println(
            "\nInvariant satisfied! Count: " + invariantCounter + "/" + invariantLimit);

        // If the limit has been reached, we don't need to reset.
        if (isInvariantLimitReached()) {
          throw new SimulationLimitReachedException();
        }

        // Reload all invariants to their original state to start tracking the next one.
        resetWorkingInvariants();
        break; // An invariant was satisfied, so we restart the process.
      }
    }
  }

  /** Resets the working copy of invariants from the original, pristine copy. */
  private void resetWorkingInvariants() {
    this.workingInvariants = deepCopy(originalInvariants);
  }

  /** Performs a deep copy of the list of lists. */
  private List<ArrayList<Integer>> deepCopy(List<ArrayList<Integer>> original) {
    return original.stream().map(ArrayList::new).collect(Collectors.toList());
  }
}
