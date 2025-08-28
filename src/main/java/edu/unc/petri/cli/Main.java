package edu.unc.petri.cli;

import edu.unc.petri.analysis.InvariantAnalyzer;
import edu.unc.petri.analysis.PetriNetAnalyzer;
import edu.unc.petri.core.CurrentMarking;
import edu.unc.petri.core.EnableVector;
import edu.unc.petri.core.IncidenceMatrix;
import edu.unc.petri.core.PetriNet;
import edu.unc.petri.core.TimeRangeMatrix;
import edu.unc.petri.monitor.ConditionQueues;
import edu.unc.petri.monitor.Monitor;
import edu.unc.petri.policy.PolicyInterface;
import edu.unc.petri.policy.PriorityPolicy;
import edu.unc.petri.policy.RandomPolicy;
import edu.unc.petri.simulation.SimulationManager;
import edu.unc.petri.util.ConfigLoader;
import edu.unc.petri.util.Log;
import edu.unc.petri.util.PetriNetConfig;
import edu.unc.petri.util.Segment;
import edu.unc.petri.workers.Worker;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the Petri-net simulator.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-07-29
 */
public final class Main {

  // Spinner/loop tunables
  private static final String SPINNER_PREFIX = "RUNNING PETRI NET SIMULATION ";
  private static final String[] SPINNER_FRAMES = {".  ", ".. ", "..."};
  private static final Duration SPINNER_PERIOD = Duration.ofMillis(300);
  private static final Duration IDLE_SLEEP = Duration.ofMillis(50);

  private Main() {} // No instances

  /**
   * Main entry point for the Petri-net simulator.
   *
   * @param args Command-line arguments. The first argument can be a path to the configuration file.
   */
  public static void main(String[] args) {
    try {
      // 1) Load config
      Path configPath = resolveConfigPath(args);
      PetriNetConfig config = ConfigLoader.load(configPath);

      InvariantAnalyzer invariantAnalyzer = new InvariantAnalyzer();
      IncidenceMatrix incidenceMatrix = new IncidenceMatrix(config.incidence);
      PetriNetAnalyzer analyzer = new PetriNetAnalyzer(invariantAnalyzer, incidenceMatrix);

      List<ArrayList<Integer>> transitionInvariants = analyzer.getTransitionInvariants();

      SimulationManager simManager =
          new SimulationManager(transitionInvariants, config.invariantLimit);

      // 2) Logging + header
      String debugLogPath =
          (config.logPath == null || config.logPath.trim().isEmpty())
              ? "debug_log.txt"
              : config.logPath;
      Log debugLog = new Log(debugLogPath);
      debugLog.logHeader("Petri Net Simulation Log", configPath.toString());

      Log transitionLog = new Log();

      // 3) Build core model
      PetriNet petriNet = buildPetriNet(config, transitionLog);

      // 4) Policy
      PolicyInterface policy;
      try {
        policy = choosePolicy(config);
      } catch (IllegalArgumentException iae) {
        System.err.println("Failed to start: " + iae.getMessage());
        return;
      }

      // Monitor
      ConditionQueues conditionQueues = new ConditionQueues(petriNet.getNumberOfTransitions());
      Monitor monitor = new Monitor(simManager, petriNet, conditionQueues, policy, debugLog);

      // 5) Workers
      List<Thread> workers = buildWorkers(config, simManager, monitor);

      // 6) Run
      startAll(workers);

      while (!simManager.isInvariantLimitReached()) {
        // runSpinnerUntilDone(workers);
        try {
          Thread.sleep(100); // Sleep for a short duration to reduce CPU usage
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
      interruptAll(workers);

      // 7) Finish
      System.out.println("Simulation complete.");

    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Failed to start: " + e.getMessage());
      System.exit(1);
    }
  }

  // public static void main(String[] args) {
  //   System.out.println("--- Running Petri Net Invariant Analysis Test ---");
  //   // 1) Load config
  //   Path configPath;
  //   PetriNetConfig config;
  //   try {
  //     configPath = resolveConfigPath(args);
  //     config = ConfigLoader.load(configPath);
  //   } catch (IOException e) {
  //     // Handle the IOException, e.g., print error and exit
  //     System.err.println("Failed to load configuration: " + e.getMessage());
  //     return;
  //   }
  //
  //   // 1. Define a sample Incidence Matrix.
  //   byte[][] incidenceData = config.incidence;
  //
  //   IncidenceMatrix incidenceMatrix = new IncidenceMatrix(incidenceData);
  //   System.out.println("\nSuccessfully created Incidence Matrix.");
  //
  //   // 2. Create an instance of the analyzer.
  //   InvariantAnalyzer invariantAnalyzer = new InvariantAnalyzer();
  //   PetriNetAnalyzer analyzer = new PetriNetAnalyzer(invariantAnalyzer, incidenceMatrix);
  //   System.out.println("PetriNetAnalyzer initialized.");
  //
  //   // 3. Get and print the Transition Invariants (T-Invariants).
  //   System.out.println("\nCalculating T-Invariants...");
  //   List<ArrayList<Integer>> transitionInvariants = analyzer.getTransitionInvariants();
  //
  //   if (transitionInvariants.isEmpty()) {
  //     System.out.println("-> No T-Invariants found.");
  //   } else {
  //     System.out.println("-> Found " + transitionInvariants.size() + " T-Invariant(s):");
  //     for (int i = 0; i < transitionInvariants.size(); i++) {
  //       System.out.print("   - Invariant " + (i + 1) + ": {");
  //       List<Integer> invariant = transitionInvariants.get(i);
  //       List<String> transitionNames = new ArrayList<>();
  //       for (Integer transitionIndex : invariant) {
  //         transitionNames.add("T" + transitionIndex);
  //       }
  //       System.out.print(String.join(", ", transitionNames));
  //       System.out.println("}");
  //     }
  //   }
  //   // Expected output for this example: {T0, T1}
  //
  //   // 4. Get and print the Place Invariants (P-Invariants).
  //   System.out.println("\nCalculating P-Invariants...");
  //   List<ArrayList<Integer>> placeInvariants = analyzer.getPlaceInvariants();
  //
  //   if (placeInvariants.isEmpty()) {
  //     System.out.println("-> No P-Invariants found.");
  //   } else {
  //     System.out.println("-> Found " + placeInvariants.size() + " P-Invariant(s):");
  //     for (int i = 0; i < placeInvariants.size(); i++) {
  //       System.out.print("   - Invariant " + (i + 1) + ": {");
  //       List<Integer> invariant = placeInvariants.get(i);
  //       List<String> placeNames = new ArrayList<>();
  //       for (Integer placeIndex : invariant) {
  //         placeNames.add("P" + placeIndex);
  //       }
  //       System.out.print(String.join(", ", placeNames));
  //       System.out.println("}");
  //     }
  //   }
  //   // Expected output for this example: {P0, P1}
  //
  //   System.out.println("\n--- Test Complete ---");
  // }

  // ------------------------
  // Construction helpers
  // ------------------------

  private static Path resolveConfigPath(String[] args) {
    String file = (args != null && args.length > 0) ? args[0] : "config_default.json";
    return Paths.get(file);
  }

  private static PetriNet buildPetriNet(PetriNetConfig cfg, Log log) {
    IncidenceMatrix incidence = new IncidenceMatrix(cfg.incidence);
    CurrentMarking current = new CurrentMarking(cfg.initialMarking);
    TimeRangeMatrix timeRanges = new TimeRangeMatrix(cfg.timeRanges);
    EnableVector enableVector = new EnableVector(incidence.getTransitions(), timeRanges);
    return new PetriNet(incidence, current, enableVector, log);
  }

  private static PolicyInterface choosePolicy(PetriNetConfig cfg) {
    String name = (cfg.policy == null) ? "random" : cfg.policy.trim().toLowerCase();
    switch (name) {
      case "priority":
        return new PriorityPolicy(cfg.transitionWeights);
      case "random":
        return new RandomPolicy();
      default:
        System.out.println("Unknown policy '" + cfg.policy + "'. Defaulting to RandomPolicy.");
        return new RandomPolicy();
    }
  }

  private static List<Thread> buildWorkers(
      PetriNetConfig cfg, SimulationManager simManager, Monitor monitor) {
    List<Thread> threads = new ArrayList<>();
    for (Segment segment : cfg.segments) {
      for (int i = 0; i < segment.threadQuantity; i++) {
        threads.add(new Worker(simManager, monitor, segment, i + 1));
      }
    }
    return threads;
  }

  // ------------------------
  // Thread lifecycle helpers
  // ------------------------

  private static void startAll(List<Thread> threads) {
    for (Thread t : threads) {
      t.start();
    }
  }

  private static void joinAll(List<Thread> threads) {
    for (Thread t : threads) {
      boolean interrupted = false;
      try {
        t.join();
      } catch (InterruptedException ie) {
        interrupted = true;
        Thread.currentThread().interrupt();
      }
      if (interrupted) {
        break;
      }
    }
  }

  // ------------------------
  // CLI spinner
  // ------------------------

  private static void runSpinnerUntilDone(List<Thread> threads, long simulationMillis) {
    // Print once so the carriage return has something to overwrite
    System.out.print(SPINNER_PREFIX + SPINNER_FRAMES[0]);
    System.out.flush();

    int frame = 0;
    long lastTick = System.nanoTime();
    long startTime = System.nanoTime();
    long simulationNanos = simulationMillis * 1_000_000L;

    while ((System.nanoTime() - startTime < simulationNanos)) {
      long now = System.nanoTime();
      if (now - lastTick >= SPINNER_PERIOD.toNanos()) {
        frame = (frame + 1) % SPINNER_FRAMES.length;
        System.out.print("\r" + SPINNER_PREFIX + SPINNER_FRAMES[frame]);
        System.out.flush();
        lastTick = now;
      }
      try {
        Thread.sleep(IDLE_SLEEP.toMillis());
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  // private static boolean anyAlive(List<Thread> threads) {
  //   for (Thread t : threads) {
  //     if (t.isAlive()) {
  //       return true;
  //     }
  //   }
  //   return false;
  // }

  private static void interruptAll(List<? extends Thread> threads) {
    for (Thread t : threads) {
      t.interrupt();
    }
  }
}
