package edu.unc.petri.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RandomPolicyTest {

  private RandomPolicy randomPolicy;

  @BeforeEach
  void setUp() {
    // Create a new instance before each test
    randomPolicy = new RandomPolicy();
  }

  @Test
  void chooseValidTransition() {
    int[] enabledTransitions = {2, 5, 8, 10};

    int chosenTransition = randomPolicy.choose(enabledTransitions);

    assertTrue(
        Arrays.stream(enabledTransitions).anyMatch(t -> t == chosenTransition),
        "The chosen transition must be one of the enabled transitions.");
  }

  @Test
  void chooseWithSingleTransitionShouldReturnThatTransition() {
    int[] enabledTransitions = {7};

    int chosenTransition = randomPolicy.choose(enabledTransitions);

    assertEquals(7, chosenTransition, "When only one transition is available, it must be chosen.");
  }

  @Test
  void chooseShouldThrowExceptionForNullInput() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          randomPolicy.choose(null);
        },
        "Choosing from a null array should throw an IllegalArgumentException.");
  }

  @Test
  void chooseShouldThrowExceptionForEmptyInput() {
    int[] emptyTransitions = {};

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          randomPolicy.choose(emptyTransitions);
        },
        "Choosing from an empty array should throw an IllegalArgumentException.");
  }
}
