package edu.unc.petri.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogTest {

  @TempDir File tempDir;

  @Test
  void constructor_withNullPath_disablesLogging() {
    Log log = new Log(null);
    assertNull(log.getFilePath());
  }

  @Test
  void constructor_withEmptyPath_disablesLogging() {
    Log log = new Log("");
    assertNull(log.getFilePath());
  }

  @Test
  void constructor_withValidPath_createsFile() {
    File logFile = new File(tempDir, "test.log");
    Log log = new Log(logFile.getAbsolutePath());
    assertTrue(new File(log.getFilePath()).exists());
  }

  @Test
  void constructor_withExistingFile_createsSuffixedFile() throws IOException {
    File logFile = new File(tempDir, "test.log");
    logFile.createNewFile();
    Log log = new Log(logFile.getAbsolutePath());
    assertTrue(new File(log.getFilePath()).exists());
    assertFalse(log.getFilePath().equals(logFile.getAbsolutePath()));
  }

  @Test
  void constructor_noParent_createsInLogsDir() {
    Log log = new Log("test.log");
    File logFile = new File(log.getFilePath());
    assertTrue(logFile.exists());
    assertEquals("logs", logFile.getParent());
    logFile.delete();
    new File("logs").delete();
  }

  @Test
  void defaultConstructor_createsTransitionLog() {
    Log log = new Log();
    File logFile = new File(log.getFilePath());
    assertTrue(logFile.exists());
    assertEquals("transition_log.txt", logFile.getName());
    logFile.delete();
  }

  @Test
  void logTransition() throws IOException {
    File logFile = new File(tempDir, "test.log");
    Log log = new Log(logFile.getAbsolutePath());
    log.logTransition(5);
    try (BufferedReader reader = new BufferedReader(new FileReader(log.getFilePath()))) {
      assertEquals("T5", reader.readLine());
    }
  }

  @Test
  void logDebug() throws IOException {
    File logFile = new File(tempDir, "test.log");
    Log log = new Log(logFile.getAbsolutePath());
    log.logDebug("test message");
    try (BufferedReader reader = new BufferedReader(new FileReader(log.getFilePath()))) {
      String line = reader.readLine();
      assertTrue(line.contains("[DEBUG]"));
      assertTrue(line.contains("test message"));
    }
  }

  @Test
  void logHeader() throws IOException {
    File logFile = new File(tempDir, "test.log");
    Log log = new Log(logFile.getAbsolutePath());
    log.logHeader("Test Title", "Test Description");
    try (BufferedReader reader = new BufferedReader(new FileReader(log.getFilePath()))) {
      assertTrue(reader.lines().anyMatch(line -> line.contains("TEST TITLE")));
    }
  }

  @Test
  void clearLog() throws IOException {
    File logFile = new File(tempDir, "test.log");
    Log log = new Log(logFile.getAbsolutePath());
    log.logTransition(1);
    log.clearLog();
    try (BufferedReader reader = new BufferedReader(new FileReader(log.getFilePath()))) {
      assertNull(reader.readLine());
    }
  }
}
