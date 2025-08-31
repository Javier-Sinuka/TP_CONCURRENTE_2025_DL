package edu.unc.petri.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the analysis of a Petri net, providing methods to calculate and display invariants.
 *
 * <p>This class serves as a high-level interface for analyzing Petri nets using a {@link
 * PetriNetAnalyzer}. It supports the computation and formatted reporting of:
 *
 * <ul>
 *   <li><b>T-Invariants</b> (transition invariants): Sets of transitions that, when fired in a
 *       certain combination, leave the marking of the net unchanged.
 *   <li><b>P-Invariants</b> (place invariants): Sets of places whose weighted sum of tokens remains
 *       constant throughout the net's execution.
 * </ul>
 *
 * <p>The analysis results are printed to the standard output in a human-readable format. If no
 * invariants are found, the report will indicate this for each category.
 *
 * <p>This class is intended for diagnostic and informational purposes and does not modify the
 * underlying Petri net.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-30-08
 */
public class AnalysisManager {
  private final PetriNetAnalyzer analyzer;
  private final ConflictAnalyzer conflictAnalyzer;

  /**
   * Constructs a new AnalysisManager.
   *
   * @param analyzer The PetriNetAnalyzer instance to be used for calculations.
   * @param conflictAnalyzer The ConflictAnalyzer instance for conflict analysis.
   */
  public AnalysisManager(PetriNetAnalyzer analyzer, ConflictAnalyzer conflictAnalyzer) {
    this.analyzer = analyzer;
    this.conflictAnalyzer = conflictAnalyzer;
  }

  /**
   * Prints a formatted analysis report of the Petri net to the standard output.
   *
   * <p>The report includes:
   *
   * <ul>
   *   <li>The calculated T-Invariants (transition invariants), listing each invariant and its
   *       transitions.
   *   <li>The calculated P-Invariants (place invariants), listing each invariant and its places.
   *   <li>The calculated structural conflicts, listing each place and the transitions that share
   *       it.
   * </ul>
   *
   * <p>If no invariants or conflicts are found, the report will indicate this for each category.
   *
   * <p>This method does not return a value and is intended for diagnostic or informational
   * purposes.
   */
  public void printAnalysisReport() {
    System.out.println("--- Petri Net Analysis Report ---");

    // Print T-Invariants
    System.out.println("\nCalculating T-Invariants...");
    List<ArrayList<Integer>> transitionInvariants = analyzer.getTransitionInvariants();
    if (transitionInvariants.isEmpty()) {
      System.out.println("-> No T-Invariants found.");
    } else {
      System.out.println("-> Found " + transitionInvariants.size() + " T-Invariant(s):");
      for (int i = 0; i < transitionInvariants.size(); i++) {
        String invariantString =
            transitionInvariants.get(i).stream()
                .map(t -> "T" + t)
                .collect(Collectors.joining(", "));
        System.out.println("   - Invariant " + (i + 1) + ": {" + invariantString + "}");
      }
    }

    // Print P-Invariants
    System.out.println("\nCalculating P-Invariants...");
    List<ArrayList<Integer>> placeInvariants = analyzer.getPlaceInvariants();
    if (placeInvariants.isEmpty()) {
      System.out.println("-> No P-Invariants found.");
    } else {
      System.out.println("-> Found " + placeInvariants.size() + " P-Invariant(s):");
      for (int i = 0; i < placeInvariants.size(); i++) {
        String invariantString =
            placeInvariants.get(i).stream().map(p -> "P" + p).collect(Collectors.joining(", "));
        System.out.println("   - Invariant " + (i + 1) + ": {" + invariantString + "}");
      }
    }

    // Print Structural Conflicts
    System.out.println("\nAnalyzing Structural Conflicts...");
    Map<Integer, List<Integer>> rawConflicts = conflictAnalyzer.getConflicts();
    if (rawConflicts.isEmpty()) {
      System.out.println("-> No structural conflicts found.");
    } else {
      // Data structure to hold collapsed conflicts.
      // Key: A sorted list of conflicting transition IDs (the conflict signature).
      // Value: A list of place IDs that share this exact conflict signature.
      Map<List<Integer>, List<Integer>> collapsedConflicts = new HashMap<>();

      // Group places by their exact set of conflicting transitions.
      for (Map.Entry<Integer, List<Integer>> entry : rawConflicts.entrySet()) {
        Integer place = entry.getKey();
        List<Integer> transitions = new ArrayList<>(entry.getValue());
        Collections.sort(transitions); // Use sorted list as a canonical key.
        collapsedConflicts.computeIfAbsent(transitions, k -> new ArrayList<>()).add(place);
      }

      System.out.println(
          "-> Found " + collapsedConflicts.size() + " unique structural conflict(s):");

      // Sort the output for deterministic reporting, based on the first place ID in each group.
      List<Map.Entry<List<Integer>, List<Integer>>> sortedConflicts =
          new ArrayList<>(collapsedConflicts.entrySet());
      sortedConflicts.sort(Comparator.comparingInt(entry -> entry.getValue().get(0)));

      for (Map.Entry<List<Integer>, List<Integer>> entry : sortedConflicts) {
        List<Integer> places = entry.getValue();
        List<Integer> transitions = entry.getKey();

        String placesString = formatPlaceList(places);
        String transitionsString =
            transitions.stream().map(t -> "T" + t).collect(Collectors.joining(", "));
        String verb = (places.size() > 1) ? "are a shared input" : "is a shared input";

        System.out.println(
            "   - " + placesString + " " + verb + " for: {" + transitionsString + "}");
      }
    }

    System.out.println("\n--- End of Analysis Report ---");
  }

  /**
   * Formats a list of place indices into a human-readable string (e.g., "P1", "P1 and P2", "P1, P2,
   * and P3").
   *
   * @param places The list of place indices.
   * @return A formatted string.
   */
  private String formatPlaceList(List<Integer> places) {
    // Sort places for consistent, readable output.
    Collections.sort(places);
    List<String> placeNames = places.stream().map(p -> "P" + p).collect(Collectors.toList());

    if (placeNames.size() < 2) {
      return placeNames.isEmpty() ? "" : placeNames.get(0);
    } else if (placeNames.size() == 2) {
      return placeNames.get(0) + " and " + placeNames.get(1);
    } else {
      // Join all but the last with ", " and then add ", and " before the last element.
      String allButLast = String.join(", ", placeNames.subList(0, placeNames.size() - 1));
      return allButLast + ", and " + placeNames.get(placeNames.size() - 1);
    }
  }
}
