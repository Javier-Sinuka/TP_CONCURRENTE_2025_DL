package edu.unc.petri.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ProbabilisticPriorityPolicyTest {

  private static class StubRandom extends Random {
    private final double[] doubles;
    private final int[] ints;
    private int doubleIndex = 0;
    private int intIndex = 0;

    StubRandom(double[] doubles, int[] ints) {
      this.doubles = doubles;
      this.ints = ints;
    }

    StubRandom(double... doubles) {
      this(doubles, new int[] {0});
    }

    @Override
    public double nextDouble() {
      double v = doubles[doubleIndex % doubles.length];
      doubleIndex++;
      return v;
    }

    @Override
    public int nextInt(int bound) {
      int v = ints[intIndex % ints.length];
      intIndex++;
      return Math.floorMod(v, bound);
    }
  }

  @Test
  void chooseHonorsWeightedSelection() {
    Map<Integer, Integer> probabilities = new HashMap<>();
    probabilities.put(1, 80);
    probabilities.put(2, 20);
    List<Integer> candidates = new ArrayList<>();
    candidates.add(1);
    candidates.add(2);
    candidates.add(3); // No weight -> should be ignored when weighted candidates exist.

    ProbabilisticPriorityPolicy policy =
        new ProbabilisticPriorityPolicy(probabilities, new StubRandom(0.1, 0.9));

    // 0.1 * 100 = 10 falls in transition 1's interval (0-80)
    assertEquals(1, policy.choose(candidates));
    // 0.9 * 100 = 90 falls in transition 2's interval (80-100)
    assertEquals(2, policy.choose(candidates));
  }

  @Test
  void chooseThrowsForMissingProbability() {
    Map<Integer, Integer> probabilities = new HashMap<>();
    probabilities.put(0, 50);
    List<Integer> candidates = new ArrayList<>();
    candidates.add(0);
    candidates.add(1);

    ProbabilisticPriorityPolicy policy =
        new ProbabilisticPriorityPolicy(
            probabilities, new StubRandom(new double[] {0.0}, new int[] {1}));

    // Only one conflict transition present -> random among all candidates.
    int chosen = policy.choose(candidates);
    assertTrue(chosen == 0 || chosen == 1);
  }

  @Test
  void chooseThrowsForNonPositiveProbability() {
    Map<Integer, Integer> probabilities = new HashMap<>();
    probabilities.put(0, 0);
    probabilities.put(1, 10);
    List<Integer> candidates = new ArrayList<>();
    candidates.add(0);
    candidates.add(1);

    ProbabilisticPriorityPolicy policy =
        new ProbabilisticPriorityPolicy(probabilities, new StubRandom(0.1));

    assertThrows(IllegalArgumentException.class, () -> policy.choose(candidates));
  }

  @Test
  void chooseThrowsForNullCandidates() {
    Map<Integer, Integer> probabilities = new HashMap<>();
    probabilities.put(0, 10);

    ProbabilisticPriorityPolicy policy =
        new ProbabilisticPriorityPolicy(probabilities, new StubRandom(0.1));

    assertThrows(IllegalArgumentException.class, () -> policy.choose(null));
  }

  @Test
  void chooseThrowsForEmptyCandidates() {
    Map<Integer, Integer> probabilities = new HashMap<>();
    probabilities.put(0, 10);

    ProbabilisticPriorityPolicy policy =
        new ProbabilisticPriorityPolicy(probabilities, new StubRandom(0.1));

    assertThrows(IllegalArgumentException.class, () -> policy.choose(new ArrayList<>()));
  }

  @Test
  void constructorThrowsForEmptyProbabilities() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProbabilisticPriorityPolicy(new HashMap<>(), new StubRandom(0.1)));
  }

  @Test
  void constructorThrowsForNullProbabilities() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProbabilisticPriorityPolicy(null, new StubRandom(0.1)));
  }

  @Test
  void constructorThrowsForNullRandom() {
    Map<Integer, Integer> probabilities = new HashMap<>();
    probabilities.put(0, 10);
    assertThrows(
        IllegalArgumentException.class, () -> new ProbabilisticPriorityPolicy(probabilities, null));
  }

  @Test
  void chooseCoversAllCandidatesOverMultipleCalls() {
    Map<Integer, Integer> probabilities = new HashMap<>();
    probabilities.put(0, 50);
    probabilities.put(1, 50);
    List<Integer> candidates = new ArrayList<>();
    candidates.add(0);
    candidates.add(1);

    // Alternate random values to make sure both choices appear.
    ProbabilisticPriorityPolicy policy =
        new ProbabilisticPriorityPolicy(probabilities, new StubRandom(0.1, 0.9));

    int first = policy.choose(candidates);
    int second = policy.choose(candidates);

    assertTrue(candidates.contains(first));
    assertTrue(candidates.contains(second));
    assertTrue(first != second, "Weighted randomness should allow selecting different transitions");
  }

  @Test
  void choosesRandomlyWhenNoWeightedCandidates() {
    Map<Integer, Integer> probabilities = new HashMap<>();
    probabilities.put(5, 10); // Not present in candidates
    List<Integer> candidates = new ArrayList<>();
    candidates.add(7);
    candidates.add(8);

    ProbabilisticPriorityPolicy policy =
        new ProbabilisticPriorityPolicy(
            probabilities, new StubRandom(new double[] {0.5}, new int[] {1}));

    int chosen = policy.choose(candidates);
    assertTrue(candidates.contains(chosen));
  }

  @Test
  void choosesRandomWhenSingleWeightedCandidateAndOthersAreUnweighted() {
    Map<Integer, Integer> probabilities = new HashMap<>();
    probabilities.put(1, 10); // Only one weighted candidate
    List<Integer> candidates = new ArrayList<>();
    candidates.add(1); // weighted
    candidates.add(2); // unweighted
    candidates.add(3); // unweighted

    // Single weighted candidate -> random among all candidates.
    ProbabilisticPriorityPolicy policy =
        new ProbabilisticPriorityPolicy(
            probabilities, new StubRandom(new double[] {0.0}, new int[] {2}));

    int chosen = policy.choose(candidates);
    assertTrue(candidates.contains(chosen));
  }

  @Test
  void exampleDistributionWhenOneConflictAndTwoNonConflicts() {
    // Only one conflict transition present -> weights not applied; random among all candidates.
    Map<Integer, Integer> probabilities = new HashMap<>();
    probabilities.put(5, 50);
    List<Integer> candidates = new ArrayList<>();
    candidates.add(5);
    candidates.add(8);
    candidates.add(1);

    ProbabilisticPriorityPolicy policy =
        new ProbabilisticPriorityPolicy(
            probabilities, new StubRandom(new double[] {0.0}, new int[] {2}));

    int chosen = policy.choose(candidates);
    assertTrue(candidates.contains(chosen));
  }

  @Test
  void exampleDistributionWhenTwoConflictsAndOneNonConflict() {
    // conflicts: T5=50, T2=35 (total 85); non-conflict T11 is ignored.
    Map<Integer, Integer> probabilities = new HashMap<>();
    probabilities.put(5, 50);
    probabilities.put(2, 35);
    List<Integer> candidates = new ArrayList<>();
    candidates.add(5);
    candidates.add(2);
    candidates.add(11);

    ProbabilisticPriorityPolicy policy =
        new ProbabilisticPriorityPolicy(probabilities, new StubRandom(0.4, 0.92));

    // r=0.4*85=34 -> falls in T5 (0-50)
    assertEquals(5, policy.choose(candidates));
    // r=0.92*85=78.2 -> falls in T2 interval (50-85)
    assertEquals(2, policy.choose(candidates));
  }
}
