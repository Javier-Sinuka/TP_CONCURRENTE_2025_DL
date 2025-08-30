package edu.unc.petri.analysis;

import java.util.ArrayList;
import java.util.List;
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

  /**
   * Constructs a new AnalysisManager.
   *
   * @param analyzer The PetriNetAnalyzer instance to be used for calculations.
   */
  public AnalysisManager(PetriNetAnalyzer analyzer) {
    this.analyzer = analyzer;
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
   * </ul>
   *
   * <p>If no invariants are found, the report will indicate this for each category.
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

    System.out.println("\n--- End of Analysis Report ---");
  }
}
