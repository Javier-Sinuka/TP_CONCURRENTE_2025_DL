package edu.unc.petri.simulation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import edu.unc.petri.util.PetriNetConfig;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimulationResultTest {

  private static final long DURATION = 1000L;
  private static final String POLICY = "TestPolicy";
  private static final Path CONFIG_PATH = Paths.get("test/config.json");

  @Mock private InvariantTracker invariantTracker;
  private PetriNetConfig config;

  private Map<Integer, Integer> transitionCounts;
  private int[] invariantCompletionCounts;
  private List<ArrayList<Integer>> originalInvariants;
  private SimulationResult simulationResult;

  @BeforeEach
  void setUp() {
    transitionCounts = new HashMap<>();
    transitionCounts.put(0, 10);
    transitionCounts.put(1, 20);

    invariantCompletionCounts = new int[] {5, 15};
    originalInvariants = new ArrayList<>();
    ArrayList<Integer> invariant1 = new ArrayList<>();
    invariant1.add(0);
    invariant1.add(1);
    originalInvariants.add(invariant1);

    when(invariantTracker.getInvariantCounter()).thenReturn(20);
    when(invariantTracker.getInvariantCompletionCounts()).thenReturn(invariantCompletionCounts);
    when(invariantTracker.getOriginalInvariants()).thenReturn(originalInvariants);

    config =
        new PetriNetConfig(
            "log.txt",
            new int[] {1, 0},
            new byte[][] {{1, -1}},
            new long[][] {{0, 1}},
            new ArrayList<>(),
            POLICY,
            new HashMap<>(),
            100);

    simulationResult =
        new SimulationResult(DURATION, transitionCounts, invariantTracker, CONFIG_PATH, config);
  }

  @Test
  void getDuration() {
    assertEquals(DURATION, simulationResult.getDuration());
  }

  @Test
  void getTransitionCounts() {
    assertEquals(transitionCounts, simulationResult.getTransitionCounts());
  }

  @Test
  void getTotalInvariantCompletionsCount() {
    assertEquals(20, simulationResult.getTotalInvariantCompletionsCount());
  }

  @Test
  void getInvariantCompletionCounts() {
    assertArrayEquals(invariantCompletionCounts, simulationResult.getInvariantCompletionCounts());
  }

  @Test
  void getOriginalInvariants() {
    assertEquals(originalInvariants, simulationResult.getOriginalInvariants());
  }

  @Test
  void getConfigPath() {
    assertEquals(CONFIG_PATH, simulationResult.getConfigPath());
  }

  @Test
  void getPolicy() {
    assertEquals(POLICY, simulationResult.getPolicy());
  }

  @Test
  void getTimestamp() {
    assertNotNull(simulationResult.getTimestamp());
    // Check that the timestamp is recent
    assertEquals(0, simulationResult.getTimestamp().compareTo(LocalDateTime.now()), 1);
  }

  @Test
  void getConfig() {
    assertEquals(config, simulationResult.getConfig());
  }
}
