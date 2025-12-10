package edu.unc.petri.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriorityPolicyTest {

  private Map<Integer, Integer> weights;
  private PriorityPolicy priorityPolicy;

  @BeforeEach
  void setUp() {
    weights = new HashMap<>();
    // Default weights for transitions
    weights.put(0, 10);
    weights.put(1, 20);
    weights.put(2, 5);
    weights.put(3, 20); // Same highest priority as transition 1
    weights.put(4, 15);
    priorityPolicy = new PriorityPolicy(weights);
  }

  @Test
  void constructorShouldThrowExceptionForNullWeights() {
    assertThrows(IllegalArgumentException.class, () -> new PriorityPolicy(null));
  }

  @Test
  void constructorShouldThrowExceptionForEmptyWeights() {
    assertThrows(IllegalArgumentException.class, () -> new PriorityPolicy(new HashMap<>()));
  }

  @Test
  void chooseShouldSelectTransitionWithHighestPriority() {
    ArrayList<Integer> enabledTransitions = new ArrayList<>();
    enabledTransitions.add(0);
    enabledTransitions.add(2);
    enabledTransitions.add(4); // Priorities: 10, 5, 15

    int chosen = priorityPolicy.choose(enabledTransitions);

    assertEquals(4, chosen, "Should choose transition 4 as it has the highest priority (15).");
  }

  @Test
  void chooseShouldSelectRandomlyInCaseOfTie() {
    ArrayList<Integer> enabledTransitions = new ArrayList<>();
    enabledTransitions.add(0);
    enabledTransitions.add(1);
    enabledTransitions.add(3);
    enabledTransitions.add(4); // Priorities: 10, 20, 20, 15

    // Collect results over multiple runs to check randomness
    Set<Integer> chosenIndices = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      int chosen = priorityPolicy.choose(enabledTransitions);
      if (chosen == 1 || chosen == 3) {
        chosenIndices.add(chosen);
      }
    }

    // Both indices with highest priority (1 and 3) should be chosen at least once
    assertTrue(chosenIndices.contains(1), "Transition 1 should be chosen at least once.");
    assertTrue(chosenIndices.contains(3), "Transition 3 should be chosen at least once.");
    assertEquals(
        2,
        chosenIndices.size(),
        "Should randomly select between transitions 1 and 3 in case of a tie.");
  }

  @Test
  void chooseShouldUseRandomWhenAllPrioritiesAreEqual() {
    weights.put(0, 10);
    weights.put(1, 10);
    weights.put(2, 10);
    priorityPolicy = new PriorityPolicy(weights);
    ArrayList<Integer> enabledTransitions = new ArrayList<>();
    enabledTransitions.add(0);
    enabledTransitions.add(1);
    enabledTransitions.add(2); // All have the same priority (10)

    Set<Integer> chosenSet = new HashSet<>();

    for (int i = 0; i < 100; i++) {
      int chosen = priorityPolicy.choose(enabledTransitions);
      assertTrue(
          enabledTransitions.contains(chosen),
          "Should choose one of the available transitions randomly.");
      chosenSet.add(chosen);
    }

    assertEquals(3, chosenSet.size(), "All transitions should be chosen at least once.");
  }

  @Test
  void chooseShouldThrowExceptionForNullInput() {
    assertThrows(IllegalArgumentException.class, () -> priorityPolicy.choose(null));
  }

  @Test
  void chooseShouldThrowExceptionForEmptyInput() {
    assertThrows(IllegalArgumentException.class, () -> priorityPolicy.choose(new ArrayList<>()));
  }

  @Test
  void chooseShouldThrowExceptionForUnknownTransition() {
    ArrayList<Integer> enabledTransitions = new ArrayList<>();
    enabledTransitions.add(0);
    enabledTransitions.add(99); // 99 is not in the weights map
    Exception exception =
        assertThrows(
            IllegalArgumentException.class, () -> priorityPolicy.choose(enabledTransitions));
    assertTrue(exception.getMessage().contains("Transition 99 does not exist in the weight map"));
  }
}
