package edu.unc.petri.cli;

import edu.unc.petri.analysis.AnalysisManager;
import edu.unc.petri.analysis.ConflictAnalyzer;
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
import edu.unc.petri.policy.ProbabilisticPriorityPolicy;
import edu.unc.petri.policy.RandomPolicy;
import edu.unc.petri.simulation.InvariantTracker;
import edu.unc.petri.simulation.SimulationManager;
import edu.unc.petri.simulation.SimulationResult;
import edu.unc.petri.simulation.StatisticsAggregator;
import edu.unc.petri.util.ConfigLoader;
import edu.unc.petri.util.Log;
import edu.unc.petri.util.PetriNetConfig;
import edu.unc.petri.util.Segment;
import edu.unc.petri.workers.Worker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * Entry point for the Petri-net simulator. It can run a simulation, an analysis, or both based on
 * command-line flags. Supports multiple runs, statistical analysis, and conditional debug logging.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-07-29
 */
public final class Main {

  private Main() {} // No instances

  private static final String BANNER =
      ""
          + "                                                                     \n"
          + "               ▀▀█      ▀                    ▄             ▀         \n"
          + "         ▄▄▄     █    ▄▄▄    ▄▄▄▄    ▄▄▄   ▄▄█▄▄   ▄ ▄▄  ▄▄▄         \n"
          + "        █▀  ▀    █      █    █▀ ▀█  █▀  █    █     █▀  ▀   █         \n"
          + "        █        █      █    █   █  █▀▀▀▀    █     █       █         \n"
          + "        ▀█▄▄▀    ▀▄▄  ▄▄█▄▄  ██▄█▀  ▀█▄▄▀    ▀▄▄   █     ▄▄█▄▄       \n"
          + "                             █                                v0.1.2 \n"
          + "                             ▀                                       \n";

  // ---- CLI parsing helpers ----
  private static final Set<String> KNOWN_FLAGS =
      new HashSet<>(
          Arrays.asList(
              "--analysis",
              "--simulation",
              "--statistics",
              "--debug",
              "--help",
              "--runs",
              "--regex-checker"));

  private static final class Cli {

    final boolean analysis;
    final boolean simulation;
    final boolean statistics;
    final boolean debug;
    final boolean help;
    final boolean regexChecker;
    final int runs;
    final String configPath;

    Cli(boolean a, boolean s, boolean st, boolean d, boolean h, boolean rx, int r, String c) {
      this.analysis = a;
      this.simulation = s;
      this.statistics = st;
      this.debug = d;
      this.help = h;
      this.regexChecker = rx;
      this.runs = r;
      this.configPath = c;
    }
  }

  private static Cli parseArgs(String[] args) {
    boolean analysis = false;
    boolean simulation = false;
    boolean statistics = false;
    boolean debug = false;
    boolean help = false;
    boolean regexChecker = false;
    String configArg = null;
    int runs = 1;

    List<String> list = Arrays.asList(args);
    for (int i = 0; i < list.size(); i++) {
      String a = list.get(i);
      if (a.startsWith("-")) {
        if (!KNOWN_FLAGS.contains(a)) {
          System.err.println("Unknown option: " + a);
          System.exit(2);
        }
        switch (a) {
          case "--runs":
            if (i + 1 >= list.size() || list.get(i + 1).startsWith("-")) {
              System.err.println("Error: --runs must be followed by a positive integer.");
              System.exit(2);
            }
            try {
              runs = Integer.parseInt(list.get(++i));
              if (runs <= 0) {
                System.err.println("Error: --runs must be >= 1.");
                System.exit(2);
              }
            } catch (NumberFormatException e) {
              System.err.println("Error: --runs must be an integer.");
              System.exit(2);
            }
            break;
          case "--analysis":
            analysis = true;
            break;
          case "--simulation":
            simulation = true;
            break;
          case "--statistics":
            statistics = true;
            break;
          case "--debug":
            debug = true;
            break;
          case "--regex-checker":
            regexChecker = true;
            break;
          case "--help":
            help = true;
            break;
          default:
            System.err.println("Unknown option: " + a);
            System.exit(2);
            break;
        }
      } else {
        if (configArg != null) {
          System.err.println("Error: multiple config paths given. Only one is allowed.");
          System.exit(2);
        }
        configArg = a;
      }
    }

    // Default: run both if neither explicitly requested
    if (!analysis && !simulation) {
      simulation = true; // Run simulation by default
      regexChecker = true; // Enable regex checker by default with simulation
    }

    if (statistics && runs == 1) {
      System.out.println("Note: --statistics has no effect when --runs == 1.");
    }

    return new Cli(analysis, simulation, statistics, debug, help, regexChecker, runs, configArg);
  }

  /**
   * Main method to execute the Petri-net simulator based on command-line arguments.
   *
   * @param args Command-line arguments to control the simulator's behavior.
   */
  public static void main(String[] args) {
    Cli cli = parseArgs(args);

    if (cli.help) {
      printHelpMessage();
      return;
    }

    try {
      // --- Initial Setup (Done Once) ---
      Path configPath = resolveConfigPath(cli.configPath);
      if (!Files.exists(configPath)) {
        System.err.println("Config file not found: " + configPath);
        System.exit(1);
      }
      PetriNetConfig config = ConfigLoader.load(configPath);
      validateConfig(config);

      // Banner for traceability
      System.out.println(BANNER);
      System.out.println("========================= Simulation Overview =========================");
      System.out.println();
      System.out.println("Config:   " + configPath.getFileName());
      System.out.println("Runs:     " + cli.runs);
      System.out.println(
          "Analysis: "
              + cli.analysis
              + " | Simulation: "
              + cli.simulation
              + " | Debug: "
              + cli.debug
              + " | Statistics: "
              + cli.statistics);
      System.out.println();
      System.out.println("=======================================================================");

      PetriNetAnalyzer analyzer = setupAnalyzer(config);

      if (cli.analysis) {
        ConflictAnalyzer conflictAnalyzer = new ConflictAnalyzer(analyzer.getIncidenceMatrix());
        AnalysisManager analysisManager = new AnalysisManager(analyzer, conflictAnalyzer);
        analysisManager.printAnalysisReport();
      }

      if (cli.simulation) {
        // --- Setup Reusable Simulation Components ---
        String debugLogPath = cli.debug ? resolveDebugPath(config) : null;
        if (cli.debug) {
          System.out.println("Debug logging is enabled. Path: " + debugLogPath);
        }
        Log debugLog = new Log(debugLogPath); // Creates a "do-nothing" log if path is null

        Log transitionLog = new Log(); // Cleared each run
        PetriNet petriNet = buildPetriNet(config, analyzer, transitionLog);
        InvariantTracker invariantTracker = setupInvariantTracker(config, analyzer);
        Monitor monitor = null;

        StatisticsAggregator stats = new StatisticsAggregator();
        // Create a stateless manager instance for report generation
        SimulationManager reporter = new SimulationManager(null, null, null);

        // --- Main Execution Loop ---
        int failedRuns = 0;
        for (int i = 0; i < cli.runs; i++) {
          if (cli.runs > 1) {
            System.out.println(
                "\n======================== Starting Run "
                    + (i + 1)
                    + " of "
                    + cli.runs
                    + " =========================");
          }

          try {
            // 1. Reset state of shared components for a clean run
            petriNet.reset(config.initialMarking);
            invariantTracker.reset();
            PolicyInterface policy = choosePolicy(config); // Create a new policy for each run
            monitor = setupMonitor(invariantTracker, petriNet, policy, debugLog);
            transitionLog.clearLog();
            debugLog.logHeader("Petri Net Simulation Log: Run " + (i + 1), configPath.toString());

            // 2. Create new lightweight worker threads for this run
            final int totalWorkers = config.segments.stream().mapToInt(s -> s.threadQuantity).sum();
            final CyclicBarrier startBarrier =
                (totalWorkers > 0) ? new CyclicBarrier(totalWorkers) : null;
            final CountDownLatch firstDoneSignal = new CountDownLatch(1);
            List<Thread> workers =
                buildWorkers(config, invariantTracker, monitor, startBarrier, firstDoneSignal);

            // 3. Execute the simulation for one run
            SimulationManager runner =
                new SimulationManager(invariantTracker, workers, transitionLog);
            SimulationResult result = runner.execute(configPath, config, firstDoneSignal);

            // 4. Process the result
            if (cli.runs > 1 && cli.statistics) {
              stats.recordRun(result);
            } else {
              reporter.generateReport(result);
            }
            // 5. Optional: run regex-based invariant checker
            if (cli.regexChecker) {
              runInvariantChecker(transitionLog.getFilePath());
            }
          } catch (Throwable runEx) {
            // isolate per-run failures when running many times
            failedRuns++;
            System.err.println("Run " + (i + 1) + " failed: " + runEx);
            runEx.printStackTrace(System.err);
            if (cli.runs == 1) {
              // single run: fail fast as before
              throw runEx;
            }
          }
        }

        // --- Final Statistics Report (if applicable) ---
        if (cli.runs > 1 && cli.statistics) {
          stats.printStatisticsReport();
        }
        if (failedRuns > 0) {
          System.err.println("\nCompleted with " + failedRuns + " failed run(s).");
        }

        System.out.println(
            "=======================================================================");

        // If Log has a close(), you can call it here; otherwise it’s a no-op object by design.
      }
    } catch (IOException e) {
      System.err.println("Failed to load or run simulation: " + e.getMessage());
      System.exit(1);
    } catch (Throwable t) {
      // Last-resort guard (useful if a worker throws outside SimulationManager control)
      System.err.println("Fatal error: " + t);
      t.printStackTrace(System.err);
      System.exit(1);
    }
  }

  /** Prints the command-line help message to the console. */
  private static void printHelpMessage() {
    System.out.println("Usage: java -jar petri-sim.jar [options] [config_file_path]");
    System.out.println();
    System.out.println(
        "Executes a Petri net analysis and/or simulation based on a JSON configuration file.");
    System.out.println(
        "If no options are specified, both analysis and simulation are run for a single"
            + " iteration.");
    System.out.println();
    System.out.println("Options:");
    System.out.println(
        "  --analysis               Run only the Petri net analysis (P and T invariants).");
    System.out.println("  --simulation             Run only the simulation.");
    System.out.println(
        "  --runs <number>          Execute the simulation a specified number of times.");
    System.out.println("                           Defaults to 1 if not provided.");
    System.out.println(
        "  --statistics             When --runs > 1, this flag suppresses individual run reports");
    System.out.println(
        "                           and displays a final statistical report with averages.");
    System.out.println(
        "  --debug                  Enable detailed debug logging. The log file path is specified");
    System.out.println("                           in the JSON configuration.");
    System.out.println(
        "  --regex-checker          After each simulation run, execute"
            + " invariant_checker/invariant_checker.py");
    System.out.println("                           on transition_log.txt and print its results.");
    System.out.println("  --help                   Display this help message and exit.");
    System.out.println();
    System.out.println("Arguments:");
    System.out.println(
        "  config_file_path         Optional. The path to the JSON configuration file.");
    System.out.println(
        "                           Defaults to 'config_default.json' if not provided.");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  java -jar petri-sim.jar");
    System.out.println("  java -jar petri-sim.jar --analysis my_config.json");
    System.out.println(
        "  java -jar petri-sim.jar --simulation --runs 10 --statistics --debug my_config.json");
  }

  // --- Setup and Construction Helpers ---

  /** Initializes the PetriNetAnalyzer with the given configuration. */
  private static PetriNetAnalyzer setupAnalyzer(PetriNetConfig config) {
    IncidenceMatrix incidenceMatrix = new IncidenceMatrix(config.incidence);
    CurrentMarking initialMarking = new CurrentMarking(config.initialMarking);
    InvariantAnalyzer invariantAnalyzer = new InvariantAnalyzer();
    return new PetriNetAnalyzer(invariantAnalyzer, incidenceMatrix, initialMarking);
  }

  /** Initializes the InvariantTracker with invariants from the analyzer and config limits. */
  private static InvariantTracker setupInvariantTracker(
      PetriNetConfig config, PetriNetAnalyzer analyzer) {
    List<ArrayList<Integer>> transitionInvariants = analyzer.getTransitionInvariants();
    return new InvariantTracker(transitionInvariants, config.invariantLimit);
  }

  /** Sets up the Monitor with the necessary components. */
  private static Monitor setupMonitor(
      InvariantTracker invariantTracker, PetriNet petriNet, PolicyInterface policy, Log debugLog) {
    ConditionQueues conditionQueues = new ConditionQueues(petriNet.getNumberOfTransitions());
    return new Monitor(invariantTracker, petriNet, conditionQueues, policy, debugLog);
  }

  /** Resolves the configuration file path from the command-line argument or defaults. */
  private static Path resolveConfigPath(String arg) {
    String file =
        (arg != null) ? arg : "simulation_configs/config_5_segments_1_thread_segment_A_random.json";
    return Paths.get(file).toAbsolutePath().normalize();
  }

  /** Chooses and constructs the policy based on the configuration. */
  private static PolicyInterface choosePolicy(PetriNetConfig cfg) {
    String raw = (cfg.policy == null) ? "random" : cfg.policy;
    String name = raw.trim().toLowerCase(java.util.Locale.ROOT);
    switch (name) {
      case "priority-probabilistic":
        return new ProbabilisticPriorityPolicy(cfg.transitionProbabilities);
      case "priority":
        return new PriorityPolicy(cfg.transitionWeights);
      case "random":
        return new RandomPolicy();
      default:
        System.out.println("Unknown policy '" + raw + "'. Defaulting to RandomPolicy.");
        return new RandomPolicy();
    }
  }

  private static void runInvariantChecker(String transitionLogPath) {
    String scriptPath = Paths.get("invariant_checker", "invariant_checker.py").toString();
    String[] interpreters = new String[] {"python3", "python", "py"};

    for (String py : interpreters) {
      try {
        ProcessBuilder pb = new ProcessBuilder(Arrays.asList(py, scriptPath, transitionLogPath));
        pb.inheritIO(); // Show script output directly in the console
        System.out.println("\n--- Running Invariant Checker ---");
        Process process = pb.start();
        int exitCode = process.waitFor(); // Wait for the script to finish

        if (exitCode == 0) {
          System.out.println("--- Invariant Checker Passed ---");
          return; // Success
        } else {
          // Script ran but failed
          throw new RuntimeException(
              "Invariant checker script failed with a non-zero exit code: " + exitCode);
        }
      } catch (IOException e) {
        // This interpreter failed to start, try the next one.
      } catch (InterruptedException e) {
        // The waiting was interrupted.
        Thread.currentThread().interrupt(); // Preserve interrupt status
        throw new RuntimeException("Invariant checker was interrupted.", e);
      }
    }
    // If the loop finishes, no interpreter was found
    throw new RuntimeException(
        "[invariant_checker] Could not execute "
            + scriptPath
            + ". Is Python installed and in PATH?");
  }

  /** Constructs the PetriNet instance from the configuration and transition log. */
  private static PetriNet buildPetriNet(
      PetriNetConfig cfg, PetriNetAnalyzer petriNetAnalyzer, Log log) {
    IncidenceMatrix incidence = new IncidenceMatrix(cfg.incidence);
    CurrentMarking current = new CurrentMarking(cfg.initialMarking);
    TimeRangeMatrix timeRanges = new TimeRangeMatrix(cfg.timeRanges);
    EnableVector enableVector = new EnableVector(incidence.getTransitions(), timeRanges);
    return new PetriNet(incidence, current, enableVector, petriNetAnalyzer, log);
  }

  /** Constructs worker threads based on the configuration segments. */
  private static List<Thread> buildWorkers(
      PetriNetConfig cfg,
      InvariantTracker invariantTracker,
      Monitor monitor,
      CyclicBarrier startBarrier,
      CountDownLatch firstDoneSignal) {
    List<Thread> threads = new ArrayList<>();
    for (Segment segment : cfg.segments) {
      int qty = Math.max(0, segment.threadQuantity);
      if (qty == 0) {
        System.out.println("Note: segment '" + segment.name + "' has threadQuantity 0; skipping.");
        continue;
      }
      for (int i = 0; i < qty; i++) {
        Thread w =
            new Worker(invariantTracker, monitor, segment, i + 1, startBarrier, firstDoneSignal);
        if (w.getName() == null || w.getName().trim().isEmpty()) {
          w.setName(segment.name + "-Worker-" + (i + 1));
        }
        w.setUncaughtExceptionHandler(
            (t, ex) -> {
              System.err.println("[uncaught] " + t.getName());
              ex.printStackTrace(System.err);
            });
        threads.add(w);
      }
    }
    return threads;
  }

  /** Resolves the debug log path based on config or defaults. */
  private static String resolveDebugPath(PetriNetConfig config) {
    if (config.logPath == null || config.logPath.trim().isEmpty()) {
      return "debug_log.txt";
    }
    return config.logPath.trim();
  }

  /** Basic sanity checks to fail fast on obviously broken configs. */
  private static void validateConfig(PetriNetConfig cfg) {
    if (cfg == null) {
      throw new IllegalArgumentException("Config is null.");
    }
    if (cfg.incidence == null || cfg.incidence.length == 0) {
      throw new IllegalArgumentException("Config.incidence is missing or empty.");
    }
    final int numPlaces = cfg.incidence.length;
    final int numTransitions = cfg.incidence[0].length;
    if (numTransitions == 0) {
      throw new IllegalArgumentException("Config.incidence has 0 transitions.");
    }

    if (cfg.initialMarking == null) {
      throw new IllegalArgumentException("Config.initialMarking is missing.");
    }
    if (cfg.initialMarking.length != numPlaces) {
      throw new IllegalArgumentException(
          String.format(
              "Config mismatch: initialMarking length (%d) does not match number of places (%d).",
              cfg.initialMarking.length, numPlaces));
    }

    if (cfg.timeRanges == null) {
      throw new IllegalArgumentException("Config.timeRanges is missing.");
    }
    if (cfg.timeRanges.length != numTransitions) {
      throw new IllegalArgumentException(
          String.format(
              "Config mismatch: timeRanges length (%d) does not match number of transitions (%d).",
              cfg.timeRanges.length, numTransitions));
    }

    if (cfg.segments == null || cfg.segments.isEmpty()) {
      throw new IllegalArgumentException("Config.segments is missing or empty.");
    }
    for (Segment s : cfg.segments) {
      if (s == null) {
        throw new IllegalArgumentException("Config.segments contains a null segment entry.");
      }
      if (s.name == null || s.name.trim().isEmpty()) {
        throw new IllegalArgumentException("A segment has an empty name.");
      }
      if (s.threadQuantity < 0) {
        throw new IllegalArgumentException("Segment '" + s.name + "' has negative threadQuantity.");
      }
      if (s.transitions != null) {
        if (s.transitions.length == 0) {
          throw new IllegalArgumentException(
              String.format("Segment '%s' contains an empty 'transitions' array.", s.name));
        }
        for (int t : s.transitions) {
          if (t < 0 || t >= numTransitions) {
            throw new IllegalArgumentException(
                String.format(
                    "Segment '%s' assigns an invalid transition index (%d). Valid indices are"
                        + " 0-%d.",
                    s.name, t, numTransitions - 1));
          }
        }
      }
    }

    if ("priority".equalsIgnoreCase(cfg.policy)) {
      if (cfg.transitionWeights == null) {
        throw new IllegalArgumentException(
            "Config.transitionWeights is required for the 'priority' policy.");
      }
      for (Integer transition : cfg.transitionWeights.keySet()) {
        if (transition < 0 || transition >= numTransitions) {
          throw new IllegalArgumentException(
              String.format(
                  "Config.transitionWeights contains an invalid transition index (%d). Valid"
                      + " indices are 0-%d.",
                  transition, numTransitions - 1));
        }
      }
    }

    if ("priority-probabilistic".equalsIgnoreCase(cfg.policy)) {
      if (cfg.transitionProbabilities == null || cfg.transitionProbabilities.isEmpty()) {
        throw new IllegalArgumentException(
            "Config.transitionProbabilities is required for the 'priority-probabilistic' policy.");
      }
      for (Map.Entry<Integer, Integer> entry : cfg.transitionProbabilities.entrySet()) {
        Integer transition = entry.getKey();
        Integer probability = entry.getValue();
        if (transition < 0 || transition >= numTransitions) {
          throw new IllegalArgumentException(
              String.format(
                  "Config.transitionProbabilities contains an invalid transition index (%d). Valid"
                      + " indices are 0-%d.",
                  transition, numTransitions - 1));
        }
        if (probability == null || probability <= 0) {
          throw new IllegalArgumentException(
              String.format(
                  "Config.transitionProbabilities for transition %d must be a positive integer.",
                  transition));
        }
      }

      // Validate that each structural conflict group sums to 100%.
      IncidenceMatrix incidenceMatrix = new IncidenceMatrix(cfg.incidence);
      ConflictAnalyzer conflictAnalyzer = new ConflictAnalyzer(incidenceMatrix);
      Map<Integer, List<Integer>> conflicts = conflictAnalyzer.getConflicts();

      for (Map.Entry<Integer, List<Integer>> conflict : conflicts.entrySet()) {
        int place = conflict.getKey();
        List<Integer> transitionsInConflict = conflict.getValue();

        int sum = 0;
        for (int t : transitionsInConflict) {
          Integer probability = cfg.transitionProbabilities.get(t);
          if (probability == null) {
            throw new IllegalArgumentException(
                String.format(
                    "Missing transitionProbabilities entry for transition %d in conflict group at"
                        + " place %d.",
                    t, place));
          }
          if (probability <= 0) {
            throw new IllegalArgumentException(
                String.format(
                    "transitionProbabilities for transition %d in conflict group at place %d must"
                        + " be positive.",
                    t, place));
          }
          sum += probability;
        }

        if (sum != 100) {
          throw new IllegalArgumentException(
              String.format(
                  "Probabilities for conflict group at place %d must sum to 100 but sum to %d.",
                  place, sum));
        }
      }
    }

    if (cfg.invariantLimit < 0) {
      throw new IllegalArgumentException("Config.invariantLimit must be >= 0.");
    }
  }
}
