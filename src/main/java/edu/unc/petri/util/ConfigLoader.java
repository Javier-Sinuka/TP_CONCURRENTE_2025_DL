package edu.unc.petri.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;

/**
 * The ConfigLoader class is responsible for loading Petri Net configurations from JSON files. It
 * uses the Jackson library to deserialize JSON content into a PetriNetConfig object.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-31-07
 */
public final class ConfigLoader {

  /** The ObjectMapper instance used for JSON deserialization. */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Private constructor to prevent instantiation of utility class. */
  private ConfigLoader() {}

  /**
   * Loads a configuration file from the specified path and maps its contents to a PetriNetConfig
   * object.
   *
   * @param path the path to the configuration file
   * @return a PetriNetConfig object representing the configuration
   * @throws IOException if an I/O error occurs during file reading
   */
  public static PetriNetConfig load(Path path) throws IOException {
    return MAPPER.readValue(path.toFile(), PetriNetConfig.class);
  }
}
