package edu.unc.petri.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The Log class is logs the events in the Petri net simulation. It can be disabled by passing a
 * null path to its constructor, in which case it performs no file I/O.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-04-08
 */
public class Log {

  /** The path of the file where the content will be saved. Will be null if logging is disabled. */
  private final String filePath;

  /**
   * Constructs a {@code Log} instance that writes sampled content to a file. If the provided {@code
   * filePath} is {@code null} or empty, logging is disabled and no files will be created or
   * written.
   *
   * <p>If the file already exists, a numeric suffix is appended to the filename to avoid
   * overwriting existing files. If no parent directory is specified in {@code filePath}, the file
   * will be created in a {@code logs} directory in the current working directory, which will be
   * created if it does not exist.
   *
   * @param filePath the path of the file where the content will be saved; if {@code null} or empty,
   *     logging is disabled
   * @throws RuntimeException if the file cannot be created due to an I/O error
   */
  public Log(String filePath) {
    if (filePath == null || filePath.trim().isEmpty()) {
      this.filePath = null; // Disable logging
      return;
    }

    File file = new File(filePath);
    int suffix = 1;
    String baseName = file.getName();
    String parent = file.getParent(); // may be null if no parent
    if (parent == null) {
      File logsDir = new File("logs");
      if (!logsDir.exists()) {
        logsDir.mkdirs();
      }
      file = new File(logsDir, baseName);
      parent = logsDir.getPath();
    }
    String name = baseName;
    String extension = "";
    int dotIndex = baseName.lastIndexOf('.');
    if (dotIndex > 0) {
      name = baseName.substring(0, dotIndex);
      extension = baseName.substring(dotIndex);
    }
    while (file.exists()) {
      String newName = name + "(" + suffix + ")" + extension;
      file = parent != null ? new File(parent, newName) : new File(newName);
      suffix++;
    }
    this.filePath = file.getPath();
    try {
      boolean created = file.createNewFile();
      if (!created) {
        throw new IOException("Failed to create the file: " + this.filePath);
      }
    } catch (IOException e) {
      throw new RuntimeException(
          "Error while creating the file: " + this.filePath + ", reason: " + e.getMessage(), e);
    }
  }

  /**
   * Initializes a new {@code Log} instance and manages the lifecycle of the log file {@code
   * transition_log.txt}.
   *
   * <p>If {@code transition_log.txt} already exists in the working directory, it will be deleted
   * and recreated to ensure a clean log file. The absolute path to the log file is stored in the
   * {@code filePath} instance variable.
   *
   * <p><b>Note:</b> Any I/O errors encountered during file deletion or creation will result in a
   * {@link RuntimeException} being thrown.
   *
   * @throws RuntimeException if an {@link IOException} occurs while deleting or creating the log
   *     file.
   */
  public Log() {
    try {
      File file = new File("transition_log.txt");
      if (file.exists()) {
        if (!file.delete()) {
          throw new IOException("Failed to delete existing file: transition_log.txt");
        }
      }
      file.createNewFile();
      this.filePath = file.getPath();
    } catch (IOException e) {
      throw new RuntimeException(
          "Error while creating or deleting the file: transition_log.txt, reason: "
              + e.getMessage(),
          e);
    }
  }

  /**
   * Logs a transition event to the specified file in the format "[HH:mm:ss.SSS] TN".
   *
   * <p>The method appends a line containing the current local time and the given transition number
   * to the file at {@code filePath}. If {@code filePath} is {@code null}, the method returns
   * immediately. In case of an I/O error, an error message is printed to standard output.
   *
   * @param transitionNumber the number of the transition to log
   */
  public void logTransition(int transitionNumber) {
    if (filePath == null) {
      return;
    }
    try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath, true))) {
      String timeStamp = java.time.LocalTime.now().toString();
      w.write("[" + timeStamp + "] T" + transitionNumber);
      w.newLine();
    } catch (IOException e) {
      System.out.println("Error while writing to the file: " + e.getMessage());
    }
  }

  /**
   * Logs a debug message to the configured log file with a timestamp.
   *
   * <p>The message is prefixed with [DEBUG] and the current local time. If the log file path is not
   * set, the method returns without logging. Any I/O errors encountered during writing are reported
   * to standard output.
   *
   * @param message the debug message to be logged; must not be {@code null}
   */
  public void logDebug(String message) {
    if (filePath == null) {
      return;
    }
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
   * @param description A description to be displayed below the title.
   */
  public void logHeader(String title, String description) {
    if (filePath == null) {
      return;
    }
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

  /**
   * Clears the content of the log file specified by {@code filePath}.
   *
   * <p>If the log file exists, its contents will be truncated, effectively erasing all previous log
   * entries. If the file does not exist, a new empty file will be created at the specified path.
   *
   * <p>This method does nothing if {@code filePath} is {@code null}. Any {@link IOException}
   * encountered during the operation will be reported to {@code System.err}.
   */
  public void clearLog() {
    if (filePath == null) {
      return;
    }
    // The 'false' argument in FileWriter truncates the file if it exists.
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
      // The try-with-resources block handles closing the writer, which flushes
      // the empty content, effectively clearing the file.
    } catch (IOException e) {
      System.err.println("Error while clearing the file: " + e.getMessage());
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
