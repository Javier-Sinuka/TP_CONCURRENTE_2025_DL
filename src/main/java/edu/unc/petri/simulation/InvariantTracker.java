package edu.unc.petri.simulation;

import edu.unc.petri.exceptions.SimulationLimitReachedException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

  private final List<ArrayList<Integer>> originalInvariants;
  private final Map<Integer, List<Pos>> indexByTransition;
  private Set<TrackerBundle> activeBundles = new HashSet<>();
  private final int invariantLimit;
  private int invariantCounter;
  private final int[] invariantCompletionCounts;
  private final boolean limitEnabled;
  private static long nextBundleId = 0;

  /**
   * Represents the position of a transition within an invariant template.
   *
   * <p>Each {@code Pos} instance identifies a specific template by its index and a position within
   * the sequence of transitions for that template.
   */
  private static final class Pos {
    final int tpl; // template index
    final int pos; // position in sequence

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
    final int tpl; // The index of the invariant template
    final int nextExpectedTransitionIdx; // The index of the *next* transition expected in sequence
    final int startPosInTpl; // The starting position in the template sequence (for circular check)

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
  private static final class TrackerBundle {
    final long id;
    final Set<InvariantTrackingInstance> hypotheses;

    TrackerBundle(Set<InvariantTrackingInstance> hypotheses) {
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
      return id == ((TrackerBundle) o).id;
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
    List<TrackerBundle> candidates = new ArrayList<>();
    for (TrackerBundle bundle : activeBundles) {
      for (InvariantTrackingInstance instance : bundle.hypotheses) {
        if (originalInvariants.get(instance.tpl).get(instance.nextExpectedTransitionIdx)
            == firedTransition) {
          candidates.add(bundle);
          break; // Found a match, this bundle is a candidate.
        }
      }
    }

    // --- Stage 2: Process based on whether there are candidates ---
    if (candidates.isEmpty()) {
      // **Case A: No existing bundle consumed the firedTransition. Spawn a new one.**
      spawnNewBundle(firedTransition);
    } else {
      // **Case B: One or more bundles could consume the firedTransition. Select a winner.**
      TrackerBundle winner =
          candidates.stream().min(Comparator.comparingLong(b -> b.id)).orElse(null);

      processWinnerBundle(winner, firedTransition);
    }

    if (isInvariantLimitReached()) {
      throw new SimulationLimitReachedException();
    }
  }

  private void processWinnerBundle(TrackerBundle winner, int firedTransition) {
    Set<TrackerBundle> nextGenerationBundles = new HashSet<>();
    for (TrackerBundle bundle : activeBundles) {
      if (bundle.id != winner.id) {
        // This bundle was not the winner, so it persists unchanged.
        nextGenerationBundles.add(bundle);
      }
    }

    // Now, process the winner to determine its next state
    Set<InvariantTrackingInstance> nextHypotheses = new HashSet<>();
    boolean bundleCompleted = false;

    // A single transition firing only advances *one* path within the winner bundle
    // We iterate to find the specific hypothesis that matches the firedTransition
    for (InvariantTrackingInstance instance : winner.hypotheses) {
      if (originalInvariants.get(instance.tpl).get(instance.nextExpectedTransitionIdx)
          == firedTransition) {
        // This is the specific hypothesis that is advanced by the firedTransition
        List<Integer> seq = originalInvariants.get(instance.tpl);
        int currentTransitionPosition =
            instance.nextExpectedTransitionIdx; // This was the one that just fired
        int nextPosInTpl = nextIdx(currentTransitionPosition, seq.size());

        if (seq.size() == 1 || nextPosInTpl == instance.startPosInTpl) {
          // The invariant complete
          invariantCounter++;
          invariantCompletionCounts[instance.tpl]++;
          bundleCompleted = true;
          // The bundle is now complete and will not be added to nextGenerationBundles
        } else {
          // The hypothesis advances.
          nextHypotheses.add(
              new InvariantTrackingInstance(instance.tpl, nextPosInTpl, instance.startPosInTpl));
        }
        // IMPORTANT: Once we found and processed the matching hypothesis,
        // we assume this single firedTransition only advances *that* one.
        // Other hypotheses in this bundle (if any) are implicitly pruned for this event.
        // However, if there are multiple hypotheses in the same bundle, and more than one could be
        // advanced by
        // the *same* firedTransition, this logic still implicitly favors the first one encountered.
      } else {
        // This hypothesis did not match the firedTransition, it is pruned.
      }
    }

    if (!bundleCompleted && !nextHypotheses.isEmpty()) {
      // The winning bundle advanced; add its new version to the next generation.
      nextGenerationBundles.add(new TrackerBundle(nextHypotheses));
    }

    this.activeBundles = nextGenerationBundles;
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
    List<Pos> carriers = indexByTransition.get(firedTransition);
    if (carriers != null) {
      Set<InvariantTrackingInstance> newHypotheses = new HashSet<>();
      for (Pos p : carriers) {
        List<Integer> seq = originalInvariants.get(p.tpl);
        if (seq.size() == 1) {
          // Single-transition invariant completes immediately, does not form a bundle.
          invariantCounter++;
          invariantCompletionCounts[p.tpl]++;
        } else {
          // Add a new hypothesis to the set for the new bundle.
          int startPos = p.pos;
          int nextPosInTpl = nextIdx(startPos, seq.size()); // The next expected transition
          newHypotheses.add(new InvariantTrackingInstance(p.tpl, nextPosInTpl, startPos));
        }
      }
      if (!newHypotheses.isEmpty()) {
        activeBundles.add(new TrackerBundle(newHypotheses));
      }
    }
  }

  /** Returns the next index in a circular manner for a list of given size. */
  private static int nextIdx(int i, int size) {
    return (i + 1) % size;
  }

  /** Builds an index mapping each transition to its positions in the invariant templates. */
  private static Map<Integer, List<Pos>> buildIndex(List<ArrayList<Integer>> invs) {
    Map<Integer, List<Pos>> idx = new HashMap<>();
    for (int tpl = 0; tpl < invs.size(); tpl++) {
      List<Integer> seq = invs.get(tpl);
      for (int pos = 0; pos < seq.size(); pos++) {
        int t = seq.get(pos);
        idx.computeIfAbsent(t, k -> new ArrayList<>()).add(new Pos(tpl, pos));
      }
    }
    return idx;
  }

  /** Creates a deep copy of the list of integer array lists. */
  private static List<ArrayList<Integer>> deepCopy(List<ArrayList<Integer>> original) {
    return original.stream().map(ArrayList::new).collect(Collectors.toList());
  }
}
