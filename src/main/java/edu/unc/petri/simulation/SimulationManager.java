package edu.unc.petri.simulation;

import edu.unc.petri.util.Log;
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

  /** The transition log for this specific run. */
  private final Log transitionLog;

  /**
   * Constructs a new SimulationManager for a single execution.
   *
   * @param invariantTracker The shared tracker used to determine when the simulation's goal is
   *     reached.
   * @param workers The list of worker threads created for this specific run.
   * @param transitionLog The unique transition log for this run.
   */
  public SimulationManager(
      InvariantTracker invariantTracker, List<Thread> workers, Log transitionLog) {
    this.invariantTracker = invariantTracker;
    this.workers = workers;
    this.transitionLog = transitionLog;
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
    startAll(workers);

    long startTime = System.currentTimeMillis();

    try {
      firstDoneSignal.await(); // Wait for the first worker to signal completion
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long duration = System.currentTimeMillis() - startTime;

    interruptAll(workers);
    joinAll(workers);

    System.out.println();
    System.out.println(
        "- - - - - - - - -  Simulation Run Complete (" + duration + " ms)  - - - - - - - - -");

    Map<Integer, Integer> transitionCounts = readTransitionCountsFromLog();

    return new SimulationResult(duration, transitionCounts, invariantTracker, configPath, config);
  }

  /**
   * Reads the transition log file and counts the occurrences of each transition firing.
   *
   * @return A map where the key is the transition number and the value is its fire count.
   */
  private Map<Integer, Integer> readTransitionCountsFromLog() {
    if (this.transitionLog == null || this.transitionLog.getFilePath() == null) {
      return new HashMap<>(); // Return empty map if logging is disabled
    }

    Map<Integer, Integer> counts = new HashMap<>();

    Pattern pattern = Pattern.compile("T(\\d+)"); // Regex to find "T" followed by digits

    try (BufferedReader reader =
        new BufferedReader(new FileReader(this.transitionLog.getFilePath()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) { // Find all transitions in the line
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
    System.out.println("\n-------------------------- Simulation Report --------------------------");
    System.out.println();
    printReportHeader(result);
    System.out.println("Total simulation time: " + result.getDuration() + " ms.");
    System.out.println(
        "Total invariants completed: "
            + result.getTotalInvariantCompletionsCount()
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
    System.out.println("\n---------------------- End of Simulation Report -----------------------");
    System.out.println();
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

  /** Joins all threads in the provided list. */
  private void joinAll(List<Thread> threads) {
    for (Thread t : threads) {
      try {
        t.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.err.println("Thread was interrupted while joining worker threads.");
        return;
      }
    }
  }
}
