package edu.unc.petri.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The Log class is logs the events in the Petri net simulation. It is used to keep track of the
 * simulation's progress and any significant events that occur.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-04-08
 */
public class Log {

  /** The path of the file where the content will be saved. */
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
   * Method that prints a string of the format "[time] TN".
   *
   * @param transitionNumber number of transition
   */
  public void logTransition(int transitionNumber) {
    try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath, true))) {
      String timeStamp = java.time.LocalTime.now().toString();
      w.write("[" + timeStamp + "] T" + transitionNumber);
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file: " + e.getMessage());
    }
  }

  /**
   * Method that stores a string of the format "[time] TN Thread: *threadName*".
   *
   * @param transitionNumber The number of the transition being logged.
   * @param threadName The name of the thread associated with the transition.
   */
  public void logTransition(int transitionNumber, String threadName) {
    try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath, true))) {
      String time = java.time.LocalDateTime.now().toString(); // ISO-8601 format
      w.write("[" + time + "] T" + transitionNumber + " Thread: " + threadName + "]");
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file: " + e.getMessage());
    }
  }

  /**
   * Method that stores a string of the format "[DEBUG][time] *message*".
   *
   * @param message the message to be logged
   */
  public void logDebug(String message) {
    try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath, true))) {
      String timestamp = java.time.LocalTime.now().toString();
      w.write("[DEBUG][" + timestamp + "] " + message);
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file: " + e.getMessage());
    }
  }

  /**
   * Logs a formatted header containing the provided title, description, and the current timestamp.
   * The header is displayed in a visually structured format with aligned content inside a box.
   *
   * @param title The title to be displayed prominently in uppercase.
   * @param description A description to be displayed below the title with additional file context.
   */
  public void logHeader(String title, String description) {
    try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath, true))) {
      // Prepare content
      String headerTitle = title.toUpperCase();
      String headerDesc = "File: " + description;
      String headerTime =
          "Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

      // Compute inner width based on the longest content line
      int innerWidth =
          Math.max(Math.max(headerTitle.length(), headerDesc.length()), headerTime.length());

      int minWidth = 40;
      int maxWidth = 120;
      innerWidth = Math.max(innerWidth, minWidth);
      innerWidth = Math.min(innerWidth, maxWidth);

      // Build lines
      String top = "╔" + repeat("═", innerWidth + 2) + "╗";
      String blank = "║ " + repeat(" ", innerWidth) + " ║";
      String lineTitle = "║ " + center(headerTitle, innerWidth) + " ║";
      String lineDesc = "║ " + padRight(headerDesc, innerWidth) + " ║";
      String lineTime = "║ " + padRight(headerTime, innerWidth) + " ║";
      String bot = "╚" + repeat("═", innerWidth + 2) + "╝";

      // Write
      w.write(top);
      w.newLine();
      w.write(lineTitle);
      w.newLine();
      w.write(blank);
      w.newLine();
      w.write(lineDesc);
      w.newLine();
      w.write(lineTime);
      w.newLine();
      w.write(bot);
      w.newLine();
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

  /* ----------------- Helpers  ----------------- */

  /** Repeats the given string `s`, `n` times. */
  private String repeat(String s, int n) {
    if (n <= 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder(s.length() * n);
    for (int i = 0; i < n; i++) {
      sb.append(s);
    }
    return sb.toString();
  }

  /** Pads the string `s` on the right with spaces to reach the specified `width`. */
  private String padRight(String s, int width) {
    if (s.length() >= width) {
      return s;
    }
    StringBuilder sb = new StringBuilder(width);
    sb.append(s);
    for (int i = s.length(); i < width; i++) {
      sb.append(' ');
    }
    return sb.toString();
  }

  /** Centers the string `s` within a field of the specified `width`, padding with spaces. */
  private String center(String s, int width) {
    if (s.length() >= width) {
      return s;
    }
    int left = (width - s.length()) / 2;
    int right = width - s.length() - left;
    StringBuilder sb = new StringBuilder(width);
    for (int i = 0; i < left; i++) {
      sb.append(' ');
    }
    sb.append(s);
    for (int i = 0; i < right; i++) {
      sb.append(' ');
    }
    return sb.toString();
  }
}
