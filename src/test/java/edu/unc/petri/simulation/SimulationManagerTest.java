package edu.unc.petri.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import edu.unc.petri.util.Log;
import edu.unc.petri.util.PetriNetConfig;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimulationManagerTest {

  @Mock private Log transitionLog;
  @Mock private PetriNetConfig config;

  private InvariantTracker invariantTracker;
  private List<Thread> workers;
  private SimulationManager simulationManager;

  @TempDir File tempDir;

  @BeforeEach
  void setUp() {
    List<ArrayList<Integer>> invariants = new ArrayList<>();
    invariants.add(new ArrayList<>(Arrays.asList(0, 1)));
    invariantTracker = new InvariantTracker(invariants, 1);
    workers = Collections.singletonList(new Thread());
  }

  @Test
  void execute() throws IOException {
    // Given
    File logFile = new File(tempDir, "test.log");
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
      writer.write("T0 fired\n");
      writer.write("T1 fired\n");
      writer.write("T0 fired\n");
    }
    when(transitionLog.getFilePath()).thenReturn(logFile.getAbsolutePath());

    CountDownLatch latch = new CountDownLatch(1);
    simulationManager = new SimulationManager(invariantTracker, workers, transitionLog);

    // When
    latch.countDown(); // Simulate worker completion
    SimulationResult result = simulationManager.execute(logFile.toPath(), config, latch);

    // Then
    assertNotNull(result);
    assertEquals(2, result.getTransitionCounts().get(0));
    assertEquals(1, result.getTransitionCounts().get(1));
  }
}
