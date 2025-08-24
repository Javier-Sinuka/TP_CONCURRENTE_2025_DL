package edu.unc.petri.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * The Log class is logs the events in the Petri net simulation. It is used to keep track of the
 * simulation's progress and any significant events that occur.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-04-08
 */
public class Log {
  private final String filePath;

  /**
   * Constructs a Log that generates a file with the sampled content.
   *
   * @param filePath is a path of file where the content will be saved.
   */
  public Log(String filePath) {
    this.filePath = filePath;
    File file = new File(filePath);
    if (file.exists()) {
      if (!file.delete()) {
        System.out.println("Failed to delete the existing file: " + filePath);
      }
    }
    try {
      boolean created = file.createNewFile();
      if (!created) {
        System.out.println("Failed to create the file: " + filePath);
      }
    } catch (IOException e) {
      System.out.println("Error while creating the file: " + e.getMessage());
    }
  }

  /**
   * Method that stores a string of the format "[T *transitionNumber*]".
   *
   * @param transitionNumber number of trasition
   */
  public void logTransition(int transitionNumber) {
    try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath, true))) {
      w.write("[T" + transitionNumber + "]");
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file whit logTransition(): " + e.getMessage());
    }
  }

  /**
   * Method that stores a string of the format "[Th-N: *threadName* T*transitionNumber*]".
   *
   * @param transitionNumber The number of the transition being logged.
   * @param threadName The name of the thread associated with the transition.
   */
  public void logTransition(int transitionNumber, String threadName) {
    try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath, true))) {
      w.write("[Th-N: " + threadName + " T" + transitionNumber + "]");
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file: " + e.getMessage());
    }
  }

  /**
   * Method that stores a string of the format "[LOG] *message*".
   *
   * @param message the message to be logged
   */
  public void logMessage(String message) {
    try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath, true))) {
      w.write("[LOG] " + message);
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file: " + e.getMessage());
    }
  }

  /**
   * Method that stores a string of the format "=== *title.UpperCase* ===".
   *
   * @param title The title to be logged as a header.
   */
  public void logHeader(String title) {
    try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath, false))) {
      w.write("=== " + title.toUpperCase() + " ===");
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file: " + e.getMessage());
    }
  }

  /** Clear content file, make that before to start write the new content. */
  public void clearLog() {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
    } catch (IOException e) {
      System.out.println("Error while clearing the file: " + e.getMessage());
    }
  }
}
