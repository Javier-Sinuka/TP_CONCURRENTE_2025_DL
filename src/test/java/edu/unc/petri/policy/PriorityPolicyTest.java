package edu.unc.petri.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
  void chooseShouldSelectTheFirstOneInCaseOfTie() {
    ArrayList<Integer> enabledTransitions = new ArrayList<>();
    enabledTransitions.add(0);
    enabledTransitions.add(1);
    enabledTransitions.add(3);
    enabledTransitions.add(4); // Priorities: 10, 20, 20, 15

    int chosen = priorityPolicy.choose(enabledTransitions);

    assertEquals(
        1,
        chosen,
        "Should choose transition 1 because it's the first one with the highest priority (20).");
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
    enabledTransitions.add(2);

    int chosen = priorityPolicy.choose(enabledTransitions);

    assertTrue(
        chosen >= 0 && chosen <= 2, "Should choose one of the available transitions randomly.");
  }

  @Test
  void chooseShouldThrowExceptionForNullInput() {
    assertThrows(IllegalArgumentException.class, () -> priorityPolicy.choose(null));
  }

  @Test
  void chooseShouldThrowExceptionForEmptyInput() {
    assertThrows(IllegalArgumentException.class, () -> priorityPolicy.choose(new ArrayList<>()));
  }
}
