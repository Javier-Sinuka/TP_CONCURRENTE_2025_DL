package edu.unc.petri.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {

  @TempDir File tempDir;

  @Test
  public void load_validConfigFile_returnsPetriNetConfig() throws IOException {
    // Given
    File configFile = new File(tempDir, "config.json");

    String json =
        "{\n"
            + "  \"logPath\": \"test.log\",\n"
            + "  \"initialMarking\": [1, 0, 0],\n"
            + "  \"incidence\": [[-1, 1, 0], [0, -1, 1]],\n"
            + "  \"timeRanges\": [[0, 0], [0, 0], [0, 0]],\n"
            + "  \"segments\": [],\n"
            + "  \"policy\": \"Random\",\n"
            + "  \"transitionWeights\": {},\n"
            + "  \"invariantLimit\": 100\n"
            + "}\n";

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
      writer.write(json);
    }

    // When
    PetriNetConfig config = ConfigLoader.load(configFile.toPath());

    // Then
    assertEquals("test.log", config.logPath);
    assertArrayEquals(new int[] {1, 0, 0}, config.initialMarking);
    assertArrayEquals(new byte[][] {{-1, 1, 0}, {0, -1, 1}}, config.incidence);
  }

  @Test
  void load_nonExistentFile_throwsIoException() {
    // Given
    Path nonExistentPath = Paths.get(tempDir.getAbsolutePath(), "nonexistent.json");

    // When & Then
    assertThrows(IOException.class, () -> ConfigLoader.load(nonExistentPath));
  }
}
