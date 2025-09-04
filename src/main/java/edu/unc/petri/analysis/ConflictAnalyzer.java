package edu.unc.petri.analysis;

import edu.unc.petri.core.IncidenceMatrix;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzes structural conflicts in a Petri net based on its incidence matrix.
 *
 * <p>A structural conflict occurs when two or more transitions share the same input place. This
 * class identifies such conflicts by examining the incidence matrix.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-31-08
 */
public class ConflictAnalyzer {

  private final IncidenceMatrix incidenceMatrix;

  /**
   * Constructs a ConflictAnalyzer for a given incidence matrix.
   *
   * @param incidenceMatrix The incidence matrix of the Petri net to analyze.
   */
  public ConflictAnalyzer(IncidenceMatrix incidenceMatrix) {
    if (incidenceMatrix == null) {
      throw new IllegalArgumentException("Incidence matrix cannot be null.");
    }
    this.incidenceMatrix = incidenceMatrix;
  }

  /**
   * Finds and returns all structural conflicts in the Petri net.
   *
   * @return A map where each key is the index of a place (P) and the value is a list of transition
   *     (T) indices that are in conflict over that place. An empty map is returned if no conflicts
   *     are found.
   */
  public Map<Integer, List<Integer>> getConflicts() {
    Map<Integer, List<Integer>> conflicts = new HashMap<>();
    int numPlaces = incidenceMatrix.getPlaces();
    int numTransitions = incidenceMatrix.getTransitions();

    // Iterate through each place (row) to see which transitions use it as an input.
    for (int p = 0; p < numPlaces; p++) {
      List<Integer> conflictingTransitions = new ArrayList<>();
      for (int t = 0; t < numTransitions; t++) {
        // A negative value indicates that place `p` is an input to transition `t`.
        if (incidenceMatrix.getElement(p, t) < 0) {
          conflictingTransitions.add(t);
        }
      }

      // A conflict exists if more than one transition shares this input place.
      if (conflictingTransitions.size() > 1) {
        conflicts.put(p, conflictingTransitions);
      }
    }
    return conflicts;
  }
}
