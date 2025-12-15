package edu.unc.petri.simulation;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Collects and summarizes results from multiple simulation runs.
 *
 * <p>This class is used to aggregate data and provide an overview of simulation outcomes. It
 * enables analysis of trends and patterns across several runs, and can generate a summary report.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-30-08
 */
public class StatisticsAggregator {

  /** Total number of runs recorded. */
  private int runCount = 0;

  /** Cumulative simulation time across all runs in milliseconds. */
  private long totalSimulationTime = 0;

  /** Cumulative transition fire counts across all runs. */
  private final Map<Integer, Long> totalTransitionCounts = new HashMap<>();

  /** Cumulative invariant completion counts across all runs. */
  private final Map<Integer, Long> totalInvariantCompletionCounts = new HashMap<>();

  // Metadata from the first run

  /** Configuration file path used for the simulations. */
  private Path configPath;

  /** Policy used in the simulations. */
  private String policy;

  /** Original invariants tracked during the simulations. */
  private List<ArrayList<Integer>> originalInvariants;

  /**
   * Records the results of a single simulation run, adding its data to the totals.
   *
   * @param result The SimulationResult object from the completed run.
   */
  public void recordRun(SimulationResult result) {
    if (runCount == 0) {
      // On the first run, store the metadata
      this.configPath = result.getConfigPath();
      this.policy = result.getPolicy();
      this.originalInvariants = result.getOriginalInvariants();
    }

    runCount++;
    totalSimulationTime += result.getDuration();

    result
        .getTransitionCounts()
        .forEach(
            (transition, count) ->
                totalTransitionCounts.merge(transition, count.longValue(), Long::sum));

    int[] completions = result.getInvariantCompletionCounts();
    for (int i = 0; i < completions.length; i++) {
      totalInvariantCompletionCounts.merge(i, (long) completions[i], Long::sum);
    }
  }

  /**
   * Generates and prints a comprehensive statistical report summarizing the results of all
   * simulation runs.
   *
   * <p>The report includes:
   *
   * <ul>
   *   <li>The total number of simulation runs
   *   <li>The average simulation time per run
   *   <li>The average fire count for each transition
   *   <li>The average completion count for each invariant (if any are defined)
   * </ul>
   *
   * <p>If no simulation runs have been recorded, a message is printed indicating that statistics
   * cannot be generated.
   */
  public void printStatisticsReport() {
    if (runCount == 0) {
      System.out.println("No simulation runs were recorded to generate statistics.");
      return;
    }

    System.out.println("\n-------------------- Statistical Simulation Report --------------------");
    System.out.println();
    printReportHeader();
    System.out.println("Number of runs: " + runCount);

    double avgTime = (double) totalSimulationTime / runCount;
    System.out.printf("Average simulation time: %.2f ms.%n", avgTime);

    // --- Average Transition Fire Counts ---
    System.out.println("\nAverage Transition Fire Counts:");
    totalTransitionCounts.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              double avgCount = (double) entry.getValue() / runCount;
              System.out.printf(" - T%d: %.2f times.%n", entry.getKey(), avgCount);
            });

    // --- Average Invariant Completion Counts ---
    System.out.println("\nAverage Invariant Completion Counts:");
    if (originalInvariants.isEmpty()) {
      System.out.println("No invariants were defined to be tracked.");
    } else {
      totalInvariantCompletionCounts.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(
              entry -> {
                double avgCount = (double) entry.getValue() / runCount;
                String invariantString =
                    originalInvariants.get(entry.getKey()).stream()
                        .map(t -> "T" + t)
                        .collect(Collectors.joining(", "));
                System.out.printf(
                    " - Invariant %d {%s}: %.2f times.%n",
                    entry.getKey() + 1, invariantString, avgCount);
              });
    }

    System.out.println("\n---------------------- End of Statistical Report ----------------------");
    System.out.println();
  }

  /** Prints the header information for the report. */
  private void printReportHeader() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    System.out.println("Date: " + LocalDateTime.now().format(formatter));
    System.out.println("Configuration File Name: " + configPath.getFileName());
    System.out.println("Policy Used: " + policy);
  }

  public int getRunCount() {
    return runCount;
  }

  public long getTotalSimulationTime() {
    return totalSimulationTime;
  }

  public Map<Integer, Long> getTotalTransitionCounts() {
    return totalTransitionCounts;
  }

  public Map<Integer, Long> getTotalInvariantCompletionCounts() {
    return totalInvariantCompletionCounts;
  }
}
