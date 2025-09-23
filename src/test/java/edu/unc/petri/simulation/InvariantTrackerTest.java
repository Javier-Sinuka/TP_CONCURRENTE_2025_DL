package edu.unc.petri.simulation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.unc.petri.exceptions.SimulationLimitReachedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvariantTrackerTest {

  private List<ArrayList<Integer>> invariants;

  @BeforeEach
  void setUp() {
    invariants = new ArrayList<>();
  }

  @Test
  void constructor_nullInvariants_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new InvariantTracker(null, null));
  }

  @Test
  void constructor_validInvariants_initializesCorrectly() {
    invariants.add(new ArrayList<>(Arrays.asList(0, 1, 2)));
    InvariantTracker tracker = new InvariantTracker(invariants, 10);

    assertEquals(0, tracker.getInvariantCounter());
    assertEquals(10, tracker.getInvariantLimit());
    assertEquals(1, tracker.getOriginalInvariants().size());
    assertEquals(3, tracker.getOriginalInvariants().get(0).size());
  }

  @Test
  void reset_clearsState() {
    invariants.add(new ArrayList<>(Arrays.asList(0, 1, 2)));
    InvariantTracker tracker = new InvariantTracker(invariants, 10);

    assertDoesNotThrow(() -> tracker.updateInvariantTracker(0));
    tracker.reset();

    assertEquals(0, tracker.getInvariantCounter());
    assertEquals(0, tracker.getInvariantCompletionCounts()[0]);

    // Fire the sequence again to make sure the tracker is in a clean state
    assertDoesNotThrow(() -> tracker.updateInvariantTracker(0));
    assertDoesNotThrow(() -> tracker.updateInvariantTracker(1));
    assertDoesNotThrow(() -> tracker.updateInvariantTracker(2));
    assertEquals(1, tracker.getInvariantCounter());
    assertEquals(1, tracker.getInvariantCompletionCounts()[0]);
  }

  @Test
  void updateInvariantTracker_singleTransitionInvariant_completesImmediately() {
    invariants.add(new ArrayList<>(Arrays.asList(0)));
    InvariantTracker tracker = new InvariantTracker(invariants, 10);

    assertDoesNotThrow(() -> tracker.updateInvariantTracker(0));

    assertEquals(1, tracker.getInvariantCounter());
    assertEquals(1, tracker.getInvariantCompletionCounts()[0]);
  }

  @Test
  void updateInvariantTracker_multiTransitionInvariant_tracksProgress() {
    invariants.add(new ArrayList<>(Arrays.asList(0, 1, 2)));
    InvariantTracker tracker = new InvariantTracker(invariants, 10);

    assertDoesNotThrow(() -> tracker.updateInvariantTracker(0));
    assertEquals(0, tracker.getInvariantCounter());

    assertDoesNotThrow(() -> tracker.updateInvariantTracker(1));
    assertEquals(0, tracker.getInvariantCounter());

    assertDoesNotThrow(() -> tracker.updateInvariantTracker(2));
    assertEquals(1, tracker.getInvariantCounter());
    assertEquals(1, tracker.getInvariantCompletionCounts()[0]);
  }

  @Test
  void updateInvariantTracker_invariantLimitReached_throwsException() {
    invariants.add(new ArrayList<>(Arrays.asList(0)));
    InvariantTracker tracker = new InvariantTracker(invariants, 1);

    assertThrows(SimulationLimitReachedException.class, () -> tracker.updateInvariantTracker(0));
    assertEquals(1, tracker.getInvariantCounter());
  }
}
