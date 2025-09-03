package edu.unc.petri.simulation;

import edu.unc.petri.util.PetriNetConfig;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Manages the lifecycle of a Petri net simulation. This class handles starting, monitoring, and
 * stopping worker threads, and can generate a report for a single run.
 */
public class SimulationManager {

  /** Shared tracker to monitor invariant completion. */
  private final InvariantTracker invariantTracker;

  /** List of worker threads executing the simulation. */
  private final List<Thread> workers;

  /** Path to the transition log file. */
  private static final String TRANSITION_LOG_PATH = "transition_log.txt";

  /**
   * Constructs a new SimulationManager for a single execution.
   *
   * @param invariantTracker The shared tracker used to determine when the simulation's goal is
   *     reached.
   * @param workers The list of worker threads created for this specific run.
   */
  public SimulationManager(InvariantTracker invariantTracker, List<Thread> workers) {
    this.invariantTracker = invariantTracker;
    this.workers = workers;
  }

  /**
   * Executes a single simulation run from start to finish.
   *
   * @param configPath The path to the simulation configuration file for metadata.
   * @param config The loaded PetriNetConfig object for metadata.
   * @return A SimulationResult object containing the results and metadata of the run.
   */
  public SimulationResult execute(
      Path configPath, PetriNetConfig config, CountDownLatch firstDoneSignal) {
    long startTime = System.currentTimeMillis();

    startAll(workers);

    try {
      firstDoneSignal.await(); // Wait for the first worker to signal completion
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // Handle the exception, e.g., log or rethrow
      System.err.println("Thread was interrupted while waiting for workers to complete.");
    }

    interruptAll(workers); // Interrupt remaining workers to signal shutdown

    long endTime = System.currentTimeMillis();

    long duration = endTime - startTime;

    // Ensure duration is non-negative in case of system clock quirks
    System.out.println("--- Simulation Run Complete (" + duration + " ms) ---");

    // Gather results
    Map<Integer, Integer> transitionCounts = readTransitionCountsFromLog();
    return new SimulationResult(duration, transitionCounts, invariantTracker, configPath, config);
  }

  /**
   * Reads the transition log file and counts the occurrences of each transition firing.
   *
   * @return A map where the key is the transition number and the value is its fire count.
   */
  private Map<Integer, Integer> readTransitionCountsFromLog() {
    Map<Integer, Integer> counts = new HashMap<>();
    Pattern pattern = Pattern.compile("\\] T(\\d+)"); // Regex to find "T" followed by digits

    try (BufferedReader reader = new BufferedReader(new FileReader(TRANSITION_LOG_PATH))) {
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
          int transitionNumber = Integer.parseInt(matcher.group(1));
          counts.put(transitionNumber, counts.getOrDefault(transitionNumber, 0) + 1);
        }
      }
    } catch (IOException e) {
      System.err.println("Error reading transition log for report: " + e.getMessage());
    }
    return counts;
  }

  /**
   * Generates and prints a detailed report for a single simulation run.
   *
   * @param result The result object from the completed simulation.
   */
  public void generateReport(SimulationResult result) {
    System.out.println("\n--- Simulation Report ---");
    printReportHeader(result);
    System.out.println("Total simulation time: " + result.getDuration() + " ms.");
    System.out.println(
        "Total invariants completed: "
            + result.getConfig().invariantLimit
            + "/"
            + result.getConfig().invariantLimit);

    // --- Transition Fire Counts ---
    System.out.println("\nTransition Fire Counts:");
    Map<Integer, Integer> transitionCounts = result.getTransitionCounts();
    if (transitionCounts.isEmpty()) {
      System.out.println("No transitions were fired or the log file is empty.");
    } else {
      transitionCounts.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(
              entry ->
                  System.out.println(
                      " - T" + entry.getKey() + ": " + entry.getValue() + " times."));
    }

    // --- Invariant Completion Counts ---
    System.out.println("\nInvariant Completion Counts:");
    int[] completionCounts = result.getInvariantCompletionCounts();
    List<ArrayList<Integer>> invariants = result.getOriginalInvariants();
    if (invariants.isEmpty()) {
      System.out.println("No invariants were defined to be tracked.");
    } else {
      for (int i = 0; i < completionCounts.length; i++) {
        String invariantString =
            invariants.get(i).stream().map(t -> "T" + t).collect(Collectors.joining(", "));
        System.out.println(
            " - Invariant "
                + (i + 1)
                + " {"
                + invariantString
                + "}: "
                + completionCounts[i]
                + " times.");
      }
    }
    System.out.println("\n--- End of Simulation Report ---");
  }

  /** Prints the header section of the simulation report. */
  private void printReportHeader(SimulationResult result) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    System.out.println("Date: " + result.getTimestamp().format(formatter));
    System.out.println("Configuration File: " + result.getConfigPath().getFileName());
    System.out.println("Policy Used: " + result.getPolicy());
  }

  /** Starts all threads in the provided list. */
  private void startAll(List<Thread> threads) {
    for (Thread t : threads) {
      t.start();
    }
  }

  /** Interrupts all threads in the provided list. */
  private void interruptAll(List<Thread> threads) {
    for (Thread t : threads) {
      t.interrupt();
    }
  }
}
