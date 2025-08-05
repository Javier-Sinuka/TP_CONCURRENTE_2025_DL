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
    if (!file.exists()) {
      try {
        boolean created = file.createNewFile();
        if (!created) {
          System.out.println("Failed to create the file: " + filePath);
        }
      } catch (IOException e) {
        System.out.println("Error while creating the file: " + e.getMessage());
      }
    }
  }

  /**
   * Method that stores a string of the format "[T *transitionNumber*]"
   *
   * @param transitionNumber number of trasition
   */
  public void logTransition(int transitionNumber) {
    try {
      FileWriter fileWriter = new FileWriter(filePath, true);
      BufferedWriter w = new BufferedWriter(fileWriter);
      w.write("[T" + transitionNumber + "]");
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file whit logTransition(): " + e.getMessage());
    }
  }

  /**
   * Method that stores a string of the format "[Th-N: *threadName* T*transitionNumber*]"
   *
   * @param transitionNumber
   * @param threadName
   */
  public void logTransition(int transitionNumber, String threadName) {
    try {
      FileWriter fileWriter = new FileWriter(filePath, true);
      BufferedWriter w = new BufferedWriter(fileWriter);
      w.write("[Th-N: " + threadName + " T" + transitionNumber + "]");
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file: " + e.getMessage());
    }
  }

  /**
   * Method that stores a string of the format "[LOG] *message*"
   *
   * @param message
   */
  public void logMessage(String message) {
    try {
      FileWriter fileWriter = new FileWriter(filePath, true);
      BufferedWriter w = new BufferedWriter(fileWriter);
      w.write("[LOG] " + message);
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file: " + e.getMessage());
    }
  }

  /**
   * Method that stores a string of the format "=== *title.UpperCase* ==="
   *
   * @param title
   */
  public void logHeader(String title) {
    try {
      FileWriter fileWriter = new FileWriter(filePath, true);
      BufferedWriter w = new BufferedWriter(fileWriter);
      w.write("=== " + title.toUpperCase() + " ===");
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file: " + e.getMessage());
    }
  }

  /**
   * Clear content file, make that before to start write the new content.
   */
  public void clearLog() {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
      } catch (IOException e) {
          System.out.println("Error while clearing the file: " + e.getMessage());
      }
  }
}
