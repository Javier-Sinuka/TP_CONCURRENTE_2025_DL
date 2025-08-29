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
 * <p>This class ensures correct handling of parallel and circular invariants by:
 *
 * <ul>
 *   <li>Maintaining a unique ID for each tracking instance to support parallelism and
 *       deduplication.
 *   <li>Spawning new tracking instances globally when transitions occur.
 *   <li>Advancing or completing tracking instances based on transition matches, with exclusivity
 *       for completers.
 *   <li>Deleting completed instances and enforcing an optional invariant completion limit.
 * </ul>
 *
 * <p>Algorithm highlights:
 *
 * <ul>
 *   <li>Circular order traversal of invariants.
 *   <li>Global spawn of new tracking instances for transitions not consumed by active trackers.
 *   <li>Deduplication and correct parallel tracking via unique instance IDs.
 *   <li>Invariant completion limit enforcement and reset capability.
 * </ul>
 */
public class InvariantTracker {

  /** Stores the original invariants as a list of integer lists. */
  private final List<ArrayList<Integer>> originalInvariants;

  /** Maps transition numbers to their corresponding list of positions. */
  private final Map<Integer, List<Pos>> indexByTransition;

  /** Set of currently active invariant tracking instances. */
  private final Set<InvariantTrackingInstance> active = new HashSet<>();

  /** The maximum number of invariants allowed. */
  private final int invariantLimit;

  /** Counter for the number of invariants processed. */
  private int invariantCounter;

  /** Indicates whether the invariant limit is enabled. */
  private final boolean limitEnabled;

  /** A static counter to ensure every new Instance gets a unique ID. */
  private static long nextInstanceId = 0;

  /**
   * Represents the position of a transition within an invariant template sequence.
   *
   * <p>Each {@code Pos} instance identifies:
   *
   * <ul>
   *   <li>The index of the invariant template ({@code tpl})
   *   <li>The position within the invariant sequence ({@code pos})
   * </ul>
   */
  private static final class Pos {
    /** Index of the invariant template. */
    final int tpl;

    /** Position within the invariant sequence. */
    final int pos;

    /**
     * Constructs a {@code Pos} with the specified template index and position.
     *
     * @param tpl the index of the invariant template
     * @param pos the position within the invariant sequence
     */
    Pos(int tpl, int pos) {
      this.tpl = tpl;
      this.pos = pos;
    }
  }

  /**
   * Represents a tracking instance for a specific invariant.
   *
   * <p>Each instance is assigned a unique ID, ensuring that even if multiple instances track the
   * same invariant at the same position, they can be distinguished from one another. The instance
   * stores the template index (tpl), current position (cur), and start position (start). Instances
   * are typically created from spawn events.
   */
  private static final class InvariantTrackingInstance {
    /** Unique identifier for this specific tracking instance. */
    final long id;

    /** Index of the invariant template being tracked. */
    final int tpl;

    /** Current position within the invariant sequence. */
    final int cur;

    /** Cursor position where this tracking instance started. */
    final int start;

    /** Constructor for brand new instances. */
    InvariantTrackingInstance(int tpl, int cur, int start) {
      this.id = nextInstanceId++; // Assign a new, unique ID
      this.tpl = tpl;
      this.cur = cur;
      this.start = start;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      InvariantTrackingInstance instance = (InvariantTrackingInstance) o;
      return id == instance.id; // Equality is based solely on the unique ID
    }

    @Override
    public int hashCode() {
      return Objects.hash(id); // Hash code is based solely on the unique ID
    }

    @Override
    public String toString() {
      return "I{id=" + id + ",tpl=" + tpl + ",cur=" + cur + ",start=" + start + "}";
    }
  }

  /**
   * Constructs an InvariantTracker with the specified list of transition invariants and a limit.
   *
   * @param transitionInvariants the list of transition invariants, each represented as an ArrayList
   *     of Integers
   * @param limit the maximum number of invariants to track; if 0 or less, no limit is enforced
   * @throws IllegalArgumentException if transitionInvariants is null
   */
  public InvariantTracker(List<ArrayList<Integer>> transitionInvariants, int limit) {
    if (transitionInvariants == null) {
      throw new IllegalArgumentException("Transition invariants list cannot be null.");
    }
    this.originalInvariants = deepCopy(transitionInvariants);
    this.invariantLimit = limit;
    this.invariantCounter = 0;
    this.limitEnabled = limit > 0 && !originalInvariants.isEmpty();
    this.indexByTransition = buildIndex(this.originalInvariants);
    InvariantTracker.nextInstanceId = 0;
  }

  /**
   * Checks if the invariant limit has been reached.
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

  /**
   * Updates the invariant tracker based on the given transition.
   *
   * <p>This method performs the following steps:
   *
   * <ul>
   *   <li>Processes the current set of active invariant tracking instances.
   *   <li>Identifies tracking instances waiting for the fired transition.
   *   <li>Handles completion and advancement of active tracking instances.
   *   <li>Enforces exclusivity for invariant completers, which means only one can complete per
   *       method call.
   *   <li>Manages spawning of new tracking instances if no active instance waits for the fired
   *       transition.
   *   <li>Increments the invariant counter as appropriate.
   *   <li>Throws a {@link SimulationLimitReachedException} if the invariant limit is reached before
   *       or after processing.
   * </ul>
   *
   * @param transition the transition to process
   * @throws SimulationLimitReachedException if the simulation invariant limit is reached
   */
  public synchronized void updateInvariantTracker(int transition)
      throws SimulationLimitReachedException {
    if (!limitEnabled || isInvariantLimitReached()) {
      throw new SimulationLimitReachedException();
    }

    // 1. Identify which instances wait for this transition
    List<InvariantTrackingInstance> willComplete = new ArrayList<>();
    List<InvariantTrackingInstance> willAdvance = new ArrayList<>();
    for (InvariantTrackingInstance inst : active) {
      List<Integer> seq = originalInvariants.get(inst.tpl);
      int expected = seq.get(inst.cur);
      if (expected == transition) {
        int next = nextIdx(inst.cur, seq.size());
        boolean completes = (seq.size() == 1) || (next == inst.start);
        if (completes) {
          willComplete.add(inst);
        } else {
          willAdvance.add(inst);
        }
      }
    }

    boolean consumedThisTick = false;
    Set<InvariantTrackingInstance> nextActive = new HashSet<>(Math.max(16, active.size()));

    if (!willComplete.isEmpty()) {
      // 1.1 Exclusivity: choose ONE single invariant completer
      willComplete.sort(
          Comparator.comparingInt((InvariantTrackingInstance x) -> x.tpl)
              .thenComparingInt(x -> x.start));
      InvariantTrackingInstance winner = willComplete.get(0);

      // Rebuild state: only the winner consumes (completes and is removed)
      for (InvariantTrackingInstance inst : active) {
        if (inst == winner) {
          invariantCounter++;
          consumedThisTick = true;
        } else {
          // everyone else does NOT consume this tick; they remain the same
          nextActive.add(inst);
        }
      }

    } else if (!willAdvance.isEmpty()) {
      // 1.2 There are no completers: all that can advance (wait for T), do so but not complete
      for (InvariantTrackingInstance inst : active) {
        List<Integer> seq = originalInvariants.get(inst.tpl);
        int expected = seq.get(inst.cur);
        if (expected == transition) {
          int next = nextIdx(inst.cur, seq.size());
          nextActive.add(new InvariantTrackingInstance(inst.tpl, next, inst.start));
          consumedThisTick = true;
        } else {
          nextActive.add(inst);
        }
      }

    } else {
      // No one was waiting for T: copy as is
      nextActive.addAll(active);
    }

    // 2 Gloal spawn: only if no one consumed
    if (!consumedThisTick) {
      List<Pos> carriers = indexByTransition.get(transition);
      if (carriers != null) {
        for (Pos p : carriers) {
          List<Integer> seq = originalInvariants.get(p.tpl);
          if (seq.size() == 1) {
            invariantCounter++; // inmediate complete
          } else {
            int start = p.pos;
            int curAfterConsume = nextIdx(start, seq.size());
            nextActive.add(new InvariantTrackingInstance(p.tpl, curAfterConsume, start));
          }
        }
        consumedThisTick = !carriers.isEmpty();
      }
    }

    // 3. Update active set
    active.clear();
    active.addAll(nextActive);

    if (isInvariantLimitReached()) {
      throw new SimulationLimitReachedException();
    }
  }

  /**
   * Returns the next index in a circular array. If the current index is the last, wraps around to
   * 0.
   *
   * @param i the current index
   * @param size the size of the array
   * @return the next index, wrapping to 0 if at the end
   */
  private static int nextIdx(int i, int size) {
    int n = i + 1;
    return (n == size) ? 0 : n;
  }

  /**
   * Builds an index mapping each integer value to a list of its positions in the input sequences.
   *
   * @param invs List of sequences, where each sequence is a list of integers.
   * @return A map from integer values to lists of Pos objects indicating their positions.
   */
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

  /**
   * Creates a deep copy of a list ArrayList of Integers. Each inner ArrayList is copied to ensure
   * modifications do not affect the original.
   *
   * @param original the list to copy
   * @return a deep copy of the original list
   */
  private static List<ArrayList<Integer>> deepCopy(List<ArrayList<Integer>> original) {
    return original.stream().map(ArrayList::new).collect(Collectors.toList());
  }
}
