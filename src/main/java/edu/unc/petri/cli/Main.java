package edu.unc.petri.cli;

import edu.unc.petri.core.CurrentMarking;
import edu.unc.petri.core.EnableVector;
import edu.unc.petri.core.IncidenceMatrix;
import edu.unc.petri.core.PetriNet;
import edu.unc.petri.core.TimeRangeMatrix;
import edu.unc.petri.monitor.ConditionQueues;
import edu.unc.petri.monitor.Monitor;
import edu.unc.petri.policy.RandomPolicy;
import edu.unc.petri.util.ConfigLoader;
import edu.unc.petri.util.Log;
import edu.unc.petri.util.PetriNetConfig;
import edu.unc.petri.util.Segment;
import edu.unc.petri.workers.Worker;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class for the Petri-net simulator. This class serves as the entry point for the application.
 * It currently prints a message indicating that the skeleton is running.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public final class Main {

  /**
   * Main method that serves as the entry point for the Petri-net simulator. It prints a message
   * indicating that the skeleton is running.
   *
   * @param args command line arguments (not used)
   */
  public static void main(String[] args) {
    try {
      // Load the Petri net configuration from the specified JSON file
      PetriNetConfig config = ConfigLoader.load(Paths.get("config.json"));

      // Initialize the core components of the Petri net
      IncidenceMatrix incidenceMatrix = new IncidenceMatrix(config.incidence);
      CurrentMarking currentMarking = new CurrentMarking(config.initialMarking);
      TimeRangeMatrix timeRangeMatrix = new TimeRangeMatrix(config.timeRanges);
      EnableVector enableVector = new EnableVector(incidenceMatrix.getTransitions());
      ConditionQueues conditionQueues = new ConditionQueues(incidenceMatrix.getTransitions());

      // Instantiate the Petri net with all configurations
      PetriNet petriNet =
          new PetriNet(incidenceMatrix, currentMarking, timeRangeMatrix, enableVector);

      // Initialize the logger to record simulation events
      Log log = new Log("log.txt");

      // Create a monitor to manage the Petri net using a random policy
      Monitor monitor = new Monitor(petriNet, new RandomPolicy(), log);

      // Prepare threads based on the configuration's segments
      List<Thread> threads = new ArrayList<>();
      for (Segment segment : config.segments) {
        for (int i = 0; i < segment.threadQuantity; i++) {
          // Add a worker thread for each segment
          threads.add(new Worker(monitor, segment));
        }
      }

      // Start all the worker threads
      for (Thread thread : threads) {
        thread.start();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
