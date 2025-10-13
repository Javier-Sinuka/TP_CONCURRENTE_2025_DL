package edu.unc.petri.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsAggregatorTest {

  @Mock private SimulationResult result1;

  private StatisticsAggregator aggregator;

  @BeforeEach
  void setUp() {
    aggregator = new StatisticsAggregator();
  }

  @Test
  void recordRun() {
    // Given
    when(result1.getDuration()).thenReturn(100L);
    Map<Integer, Integer> transitionCounts1 = new HashMap<>();
    transitionCounts1.put(0, 10);
    when(result1.getTransitionCounts()).thenReturn(transitionCounts1);
    when(result1.getInvariantCompletionCounts()).thenReturn(new int[] {1, 2});
    when(result1.getConfigPath()).thenReturn(Paths.get("config.yaml"));
    when(result1.getPolicy()).thenReturn("Random");
    when(result1.getOriginalInvariants()).thenReturn(new ArrayList<>());

    // When
    aggregator.recordRun(result1);

    // Then
    assertEquals(1, aggregator.getRunCount());
    assertEquals(100L, aggregator.getTotalSimulationTime());
    assertEquals(10L, aggregator.getTotalTransitionCounts().get(0).longValue());
    assertEquals(1L, aggregator.getTotalInvariantCompletionCounts().get(0).longValue());
    assertEquals(2L, aggregator.getTotalInvariantCompletionCounts().get(1).longValue());
  }
}
