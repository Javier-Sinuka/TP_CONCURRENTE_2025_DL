package edu.unc.petri.simulation;

import edu.unc.petri.exceptions.SimulationLimitReachedException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tracks and manages the lifecycle of transition invariants (T-invariants) during simulation.
 *
 * <p>This implementation uses a "Contention and Winner Selection" algorithm.
 *
 * <ul>
 *   <li><b>Contention:</b> When a transition fires, it's treated as a single event. All active
 *       processes ("bundles") waiting for this event are considered candidates.
 *   <li><b>Winner Selection:</b> Only one candidate, the "winner" (chosen by the earliest creation
 *       time), gets to consume the event and advance its state.
 *   <li><b>State Preservation:</b> All other bundles, including those that lost the race for the
 *       transition, persist unchanged, waiting for a future event.
 *   <li><b>Spawning:</b> A new bundle is created only if no existing bundle could consume the
 *       transition event, signifying the start of a new process.
 * </ul>
 */
public class InvariantTracker {

  /** Stores the original invariants as a list of integer lists. */
  private final List<ArrayList<Integer>> originalInvariants;

  /** Maps transition indices to lists of positions. */
  private final Map<Integer, List<Pos>> indexByTransition;

  /** Set of currently active tracking instance bundles. */
  private Set<TrackingInstanceBundle> activeBundles = new HashSet<>();

  /** The maximum number of invariants allowed. */
  private final int invariantLimit;

  /** Counter for the number of invariants processed. */
  private int invariantCounter;

  /** Array tracking completion counts for each invariant. */
  private final int[] invariantCompletionCounts;

  /** Indicates whether the invariant limit is enabled. */
  private final boolean limitEnabled;

  /** Static variable for generating unique bundle IDs. */
  private static long nextBundleId = 0;

  /**
   * Represents the position of a transition within an invariant template.
   *
   * <p>Each {@code Pos} instance identifies a specific template by its index and a position within
   * the sequence of transitions for that template.
   */
  private static final class Pos {
    /** The index of the template. */
    final int tpl;

    /** The position in the sequence. */
    final int pos;

    Pos(int tpl, int pos) {
      this.tpl = tpl;
      this.pos = pos;
    }
  }

  /**
   * Represents a single hypothesis for which invariant is being executed.
   *
   * <p>Each instance tracks the progress of checking a specific invariant template against a
   * sequence of transitions. It records:
   */
  private static final class InvariantTrackingInstance {
    /** The index of the invariant template. */
    final int tpl;

    /** The index of the next transition expected in sequence. */
    int nextExpectedTransitionIdx;

    /** The starting position in the template sequence (for circular check). */
    final int startPosInTpl;

    InvariantTrackingInstance(int tpl, int nextExpectedTransitionIdx, int startPosInTpl) {
      this.tpl = tpl;
      this.nextExpectedTransitionIdx = nextExpectedTransitionIdx;
      this.startPosInTpl = startPosInTpl;
    }
  }

  /**
   * Represents a bundle of mutually exclusive hypotheses for a single ongoing invariant execution.
   *
   * <p>Each {@code TrackerBundle} is identified by a unique {@code id} and contains a set of {@link
   * InvariantTrackingInstance} objects representing the hypotheses being tracked. Bundles are
   * considered equal if their {@code id} values are the same.
   */
  private static final class TrackingInstanceBundle {
    /** Unique identifier for the tracking instance. */
    final long id;

    /** Set of hypotheses being tracked by this instance. */
    final Set<InvariantTrackingInstance> hypotheses;

    TrackingInstanceBundle(Set<InvariantTrackingInstance> hypotheses) {
      this.id = nextBundleId++;
      this.hypotheses = hypotheses;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      return id == ((TrackingInstanceBundle) o).id;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }

  /**
   * Constructs an InvariantTracker with the specified list of transition invariants and an optional
   * limit.
   *
   * <p>The transition invariants are deep-copied to preserve immutability. The limit parameter
   * controls the maximum number of invariants to track; if null or non-positive, no limit is
   * enforced. The tracker initializes internal counters and indices for efficient lookup and
   * completion tracking.
   *
   * @param transitionInvariants the list of transition invariants to track; must not be null
   * @param limit the maximum number of invariants to track; if null or non-positive, no limit is
   *     enforced
   * @throws IllegalArgumentException if {@code transitionInvariants} is null
   */
  public InvariantTracker(List<ArrayList<Integer>> transitionInvariants, Integer limit) {
    if (transitionInvariants == null) {
      throw new IllegalArgumentException("Transition invariants list cannot be null.");
    }
    this.originalInvariants = deepCopy(transitionInvariants);
    this.invariantLimit = (limit != null && limit > 0) ? limit : Integer.MAX_VALUE;
    this.invariantCounter = 0;
    this.limitEnabled = (limit != null && limit > 0) && !originalInvariants.isEmpty();
    this.indexByTransition = buildIndex(this.originalInvariants);
    this.invariantCompletionCounts = new int[this.originalInvariants.size()];
  }

  /**
   * Checks if the invariant limit has been reached.
   *
   * <p>If the limit is not enabled, this method returns {@code false}. Otherwise, it returns {@code
   * true} if the invariant counter is greater than or equal to the invariant limit.
   *
   * @return {@code true} if the invariant limit is reached; {@code false} otherwise.
   */
  public synchronized boolean isInvariantLimitReached() {
    if (!limitEnabled) {
      return false;
    }
    return invariantCounter >= invariantLimit;
  }

  public synchronized int getInvariantCounter() {
    return invariantCounter;
  }

  public int getInvariantLimit() {
    return invariantLimit;
  }

  public synchronized int[] getInvariantCompletionCounts() {
    return invariantCompletionCounts;
  }

  public List<ArrayList<Integer>> getOriginalInvariants() {
    return originalInvariants;
  }

  /**
   * Advances the tracker by one simulation event, updating in–flight invariant executions, spawning
   * new executions when needed, and recording completed cycles. This is the core step that must be
   * invoked after every successfully fired transition.
   *
   * <p><b>Algorithm overview</b>
   *
   * <ol>
   *   <li><b>Early limit check:</b> If an invariant limit is enabled and already reached, a {@link
   *       SimulationLimitReachedException} is thrown immediately.
   *   <li><b>Candidate discovery:</b> Among the active bundles (ongoing invariant executions), find
   *       those that can consume the {@code firedTransition}, i.e., any hypothesis whose next
   *       expected transition equals {@code firedTransition}.
   *   <li><b>No candidates → spawn:</b> If no bundle can consume the event, a new bundle is created
   *       from all invariant template positions that contain {@code firedTransition}.
   *       Single-transition templates complete immediately and increment the counters; longer
   *       templates produce a hypothesis (start position = the matched index; next expected = the
   *       following index mod length) added to the new bundle.
   *   <li><b>With candidates → winner:</b> If one or more bundles can consume the event, the
   *       earliest-created bundle (smallest id) is chosen as the <i>winner</i>. All other bundles
   *       persist unchanged.
   *   <li><b>Winner advancement:</b> Within the winner, the hypothesis that expects {@code
   *       firedTransition} is advanced (its next-expected index moves forward circularly). If
   *       advancing wraps around to the hypothesis's start position (or the template length is 1),
   *       that execution completes and the global and per-template completion counters are
   *       incremented; otherwise, the advanced hypothesis is kept in a new version of the bundle.
   *       Non-matching hypotheses are pruned.
   *   <li><b>Post limit check:</b> If, after processing the event, the invariant limit is reached,
   *       a {@link SimulationLimitReachedException} is thrown.
   * </ol>
   *
   * <p><b>Thread-safety:</b> This method is {@code synchronized}; updates to internal state (active
   * bundles and completion counters) are serialized and visible across threads.
   *
   * @param firedTransition the transition id that just fired in the simulation
   * @throws SimulationLimitReachedException if the configured invariant completion limit was
   *     already reached before processing this event, or becomes reached as a result of processing
   *     it
   */
  public synchronized void updateInvariantTracker(int firedTransition)
      throws SimulationLimitReachedException {
    if (isInvariantLimitReached()) {
      throw new SimulationLimitReachedException();
    }

    // --- Stage 1: Identify all candidate bundles that can consume this firedTransition ---
    List<TrackingInstanceBundle> candidates = new ArrayList<>();
    for (TrackingInstanceBundle bundle : activeBundles) {
      for (InvariantTrackingInstance instance : bundle.hypotheses) {
        if (originalInvariants.get(instance.tpl).get(instance.nextExpectedTransitionIdx)
            == firedTransition) {
          candidates.add(bundle);
          break; // Found a match, this bundle is a candidate. We just need at least one match.
        }
      }
    }

    // --- Stage 2: Process based on whether there are candidates ---
    if (candidates.isEmpty()) {
      // **Case A: No active bundle expects to consume the firedTransition. Spawn a new one.**
      spawnNewBundle(firedTransition);
    } else {
      // **Case B: One or more active bundles expect to consume the firedTransition. Select a
      // winner.**
      TrackingInstanceBundle winnerBundle =
          candidates.stream()
              .min(Comparator.comparingLong(b -> b.id))
              .orElse(null); // Select the earliest-created bundle

      processWinnerBundle(winnerBundle, firedTransition);
    }

    if (isInvariantLimitReached()) {
      throw new SimulationLimitReachedException();
    }
  }

  /**
   * Processes the winner bundle after a transition fires.
   *
   * <p>For the given fired transition, this method advances all hypotheses within the winner bundle
   * that expect the transition, while pruning (removing) those that do not. Advancing means
   * updating the tracking instance's expected transition index to the next position in its
   * invariant template.
   *
   * <p>If any hypothesis completes its invariant (i.e., all expected transitions have fired in
   * order), the bundle is marked as completed, relevant counters are incremented, and the bundle is
   * removed from the set of active bundles. Only one invariant can complete per fired transition;
   * support for multiple completions (e.g., cobegin/coend) is a potential future enhancement.
   *
   * @param winnerBundle the bundle containing hypotheses to process
   * @param firedTransition the transition that has just fired
   */
  private void processWinnerBundle(TrackingInstanceBundle winnerBundle, int firedTransition) {
    boolean bundleCompleted = false;

    // A single transition firing advances all the hypotheses that expect the transition within the
    // winner bundle and the rest of the hypotheses are pruned.
    for (Iterator<InvariantTrackingInstance> it = winnerBundle.hypotheses.iterator();
        it.hasNext(); ) {
      InvariantTrackingInstance trackingInstance = it.next();

      if (originalInvariants
              .get(trackingInstance.tpl)
              .get(trackingInstance.nextExpectedTransitionIdx)
          == firedTransition) {

        // This hypothesis expects the firedTransition, it advances.
        List<Integer> invariantTemplate = originalInvariants.get(trackingInstance.tpl);
        int currentTransitionPosition = trackingInstance.nextExpectedTransitionIdx; // just fired
        int nextPosInTpl = nextIdx(currentTransitionPosition, invariantTemplate.size());

        if (invariantTemplate.size() == 1 || nextPosInTpl == trackingInstance.startPosInTpl) {
          // NOTE: Only one invariant can complete per fired transition.
          // TODO: add support for cobegin and coend to allow multiple invariants to complete at the
          // same time.
          invariantCounter++;
          invariantCompletionCounts[trackingInstance.tpl]++;
          bundleCompleted = true;
          break; // bundle complete; no need to process further hypotheses
        } else {
          // Advance the tracking instance in place (safe: no structural modification)
          trackingInstance.nextExpectedTransitionIdx = nextPosInTpl;
        }

      } else {
        // This hypothesis did not expect the firedTransition, prune it
        it.remove();
      }
    }

    if (bundleCompleted) {
      // The winner bundle completed; remove it from active bundles.
      this.activeBundles.remove(winnerBundle);
    }
  }

  /**
   * Resets the internal state of the tracker to prepare for a new simulation run.
   *
   * <p>This method performs the following actions:
   *
   * <ul>
   *   <li>Resets the invariant counter to zero.
   *   <li>Clears the array of invariant completion counts.
   *   <li>Removes all active bundles.
   *   <li>Resets the bundle ID to zero to ensure deterministic behavior in tests and runs.
   * </ul>
   */
  public synchronized void reset() {
    this.invariantCounter = 0;
    // Clear the array of completion counts
    for (int i = 0; i < this.invariantCompletionCounts.length; i++) {
      this.invariantCompletionCounts[i] = 0;
    }
    this.activeBundles.clear();
    // Reset the bundle ID to ensure deterministic behavior in tests/runs
    nextBundleId = 0;
  }

  /** Spawns a new bundle for the given fired transition if no existing bundle could consume it. */
  private void spawnNewBundle(int firedTransition) {
    List<Pos> transitionPositions = indexByTransition.get(firedTransition);
    if (transitionPositions != null) {
      Set<InvariantTrackingInstance> newHypotheses = new HashSet<>();
      for (Pos p : transitionPositions) {
        List<Integer> invariantTemplate = originalInvariants.get(p.tpl);
        if (invariantTemplate.size() == 1) { // Autoloop invariant
          // Single-transition invariant completes immediately, does not form a bundle.
          invariantCounter++; // Global count
          invariantCompletionCounts[p.tpl]++; // Per-invariant count
        } else {
          // Add a new hypothesis to the set for the new bundle.
          int startPos = p.pos;
          int nextPosInTpl =
              nextIdx(startPos, invariantTemplate.size()); // The next expected transition
          newHypotheses.add(new InvariantTrackingInstance(p.tpl, nextPosInTpl, startPos));
        }
      }
      if (!newHypotheses.isEmpty()) {
        activeBundles.add(new TrackingInstanceBundle(newHypotheses));
      }
    }
  }

  /** Returns the next index in a circular manner for a list of given size. */
  private static int nextIdx(int i, int size) {
    return (i + 1) % size;
  }

  /**
   * Constructs an index that maps each transition to a list of its occurrences within the invariant
   * templates.
   *
   * <p>For each transition found in the provided list of invariant templates, this method records
   * all positions (template index and position within the template) where the transition appears.
   * The resulting map allows efficient lookup of all locations for a given transition across all
   * templates.
   *
   * @param transitionInvariants a list of invariant templates, each represented as a list of
   *     transition integers
   * @return a map from transition integer to a list of Pos objects indicating its positions in the
   *     templates
   */
  private static Map<Integer, List<Pos>> buildIndex(List<ArrayList<Integer>> transitionInvariants) {
    Map<Integer, List<Pos>> idx = new HashMap<>();
    for (int tpl = 0; tpl < transitionInvariants.size(); tpl++) {
      List<Integer> seq = transitionInvariants.get(tpl);
      for (int pos = 0; pos < seq.size(); pos++) {
        int transition = seq.get(pos);
        idx.computeIfAbsent(transition, k -> new ArrayList<>()).add(new Pos(tpl, pos));
      }
    }
    return idx;
  }

  /** Creates a deep copy of the list of integer array lists. */
  private static List<ArrayList<Integer>> deepCopy(List<ArrayList<Integer>> original) {
    return original.stream().map(ArrayList::new).collect(Collectors.toList());
  }
}
