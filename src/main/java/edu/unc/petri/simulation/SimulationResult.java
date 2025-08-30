package edu.unc.petri.simulation;

import edu.unc.petri.util.PetriNetConfig;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a Petri net simulation, encapsulating key metrics and configuration
 * details.
 *
 * <p>This class stores information about the simulation duration, transition firing counts,
 * invariant completion statistics, the original invariants, the configuration file path, the policy
 * used, the timestamp of the result, and the Petri net configuration. It is typically constructed
 * after a simulation run and provides access to all relevant data for analysis or reporting.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-30-08
 */
public class SimulationResult {

  /** Duration of the simulation in milliseconds. */
  private final long duration;

  /** Map of transition IDs to their respective firing counts. */
  private final Map<Integer, Integer> transitionCounts;

  /** Array of counts indicating how many times each invariant was completed. */
  private final int[] invariantCompletionCounts;

  /** List of original invariants used in the simulation. */
  private final List<ArrayList<Integer>> originalInvariants;

  /** Path to the configuration file used for the simulation. */
  private final Path configPath;

  /** The policy used during the simulation. */
  private final String policy;

  /** Timestamp indicating when the simulation result was created. */
  private final LocalDateTime timestamp;

  /** The configuration settings for the Petri net simulation. */
  private final PetriNetConfig config;

  /**
   * Constructs a SimulationResult object with the specified simulation details.
   *
   * @param duration the duration of the simulation in milliseconds
   * @param transitionCounts a map of transition IDs to their respective counts
   * @param invariantTracker the tracker containing invariant completion data
   * @param configPath the path to the configuration file used for the simulation
   * @param config the PetriNet configuration used for the simulation
   */
  public SimulationResult(
      long duration,
      Map<Integer, Integer> transitionCounts,
      InvariantTracker invariantTracker,
      Path configPath,
      PetriNetConfig config) {
    this.duration = duration;
    this.transitionCounts = transitionCounts;
    this.invariantCompletionCounts = invariantTracker.getInvariantCompletionCounts();
    this.originalInvariants = invariantTracker.getOriginalInvariants();
    this.configPath = configPath;
    this.policy = config.policy;
    this.config = config;
    this.timestamp = LocalDateTime.now();
  }

  public long getDuration() {
    return duration;
  }

  public Map<Integer, Integer> getTransitionCounts() {
    return transitionCounts;
  }

  public int[] getInvariantCompletionCounts() {
    return invariantCompletionCounts;
  }

  public List<ArrayList<Integer>> getOriginalInvariants() {
    return originalInvariants;
  }

  public Path getConfigPath() {
    return configPath;
  }

  public String getPolicy() {
    return policy;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public PetriNetConfig getConfig() {
    return config;
  }
}
