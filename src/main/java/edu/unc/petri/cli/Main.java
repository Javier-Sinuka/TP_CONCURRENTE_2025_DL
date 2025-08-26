package edu.unc.petri.cli;

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

      // 2) Build core model
      PetriNet petriNet = buildPetriNet(config);

      // 3) Logging + header
      String logPath =
          (config.logPath == null || config.logPath.trim().isEmpty())
              ? "default_log.txt"
              : config.logPath;
      Log log = new Log(logPath);
      log.logHeader("Petri Net Simulation Log", configPath.toString());

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
      Monitor monitor = new Monitor(petriNet, conditionQueues, policy, log);

      // 5) Workers
      List<Thread> workers = buildWorkers(config, monitor);

      // 6) Run
      startAll(workers);
      runSpinnerUntilDone(workers);
      joinAll(workers);

      // 7) Finish
      clearCurrentLine();
      System.out.println("Simulation complete.");

    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Failed to start: " + e.getMessage());
      System.exit(1);
    }
  }

  // ------------------------
  // Construction helpers
  // ------------------------

  private static Path resolveConfigPath(String[] args) {
    String file = (args != null && args.length > 0) ? args[0] : "config_default.json";
    return Paths.get(file);
  }

  private static PetriNet buildPetriNet(PetriNetConfig cfg) {
    IncidenceMatrix incidence = new IncidenceMatrix(cfg.incidence);
    CurrentMarking current = new CurrentMarking(cfg.initialMarking);
    EnableVector enableVector = new EnableVector(incidence.getTransitions());
    TimeRangeMatrix timeRanges = new TimeRangeMatrix(cfg.timeRanges, enableVector);
    return new PetriNet(incidence, current, timeRanges, enableVector);
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

  private static List<Thread> buildWorkers(PetriNetConfig cfg, Monitor monitor) {
    List<Thread> threads = new ArrayList<>();
    for (Segment segment : cfg.segments) {
      for (int i = 0; i < segment.threadQuantity; i++) {
        threads.add(new Worker(monitor, segment, i + 1));
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

  private static void runSpinnerUntilDone(List<Thread> threads) {
    // Print once so the carriage return has something to overwrite
    System.out.print(SPINNER_PREFIX + SPINNER_FRAMES[0]);
    System.out.flush();

    int frame = 0;
    long lastTick = System.nanoTime();

    while (anyAlive(threads)) {
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

  private static boolean anyAlive(List<Thread> threads) {
    for (Thread t : threads) {
      if (t.isAlive()) {
        return true;
      }
    }
    return false;
  }

  private static void clearCurrentLine() {
    System.out.print("\r");
    System.out.flush();
  }
}
