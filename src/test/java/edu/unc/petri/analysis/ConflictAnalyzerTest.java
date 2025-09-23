package edu.unc.petri.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.unc.petri.core.IncidenceMatrix;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConflictAnalyzerTest {

  @Test
  void constructor_nullIncidenceMatrix_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new ConflictAnalyzer(null));
  }

  @Test
  void getConflicts_noConflicts_returnsEmptyMap() {
    IncidenceMatrix incidenceMatrix = new IncidenceMatrix(new byte[][] {{1, -1, 0}, {0, 1, -1}});
    ConflictAnalyzer analyzer = new ConflictAnalyzer(incidenceMatrix);
    Map<Integer, List<Integer>> conflicts = analyzer.getConflicts();
    assertTrue(conflicts.isEmpty());
  }

  @Test
  void getConflicts_singleConflict_returnsCorrectMap() {
    // Place 0 is an input to transitions 0 and 1 (a conflict)
    IncidenceMatrix incidenceMatrix = new IncidenceMatrix(new byte[][] {{-1, -1, 0}, {1, 0, -1}});
    ConflictAnalyzer analyzer = new ConflictAnalyzer(incidenceMatrix);
    Map<Integer, List<Integer>> conflicts = analyzer.getConflicts();
    assertEquals(1, conflicts.size());
    assertTrue(conflicts.containsKey(0));
    List<Integer> conflictingTransitions = conflicts.get(0);
    assertEquals(2, conflictingTransitions.size());
    assertTrue(conflictingTransitions.contains(0));
    assertTrue(conflictingTransitions.contains(1));
  }

  @Test
  void getConflicts_multipleConflicts_returnsCorrectMap() {
    // Place 0 is an input to transitions 0 and 2
    // Place 1 is an input to transitions 1 and 2
    IncidenceMatrix incidenceMatrix =
        new IncidenceMatrix(new byte[][] {{-1, 0, -1}, {0, -1, -1}, {1, 1, 1}});
    ConflictAnalyzer analyzer = new ConflictAnalyzer(incidenceMatrix);
    Map<Integer, List<Integer>> conflicts = analyzer.getConflicts();

    assertEquals(2, conflicts.size());

    // Check conflict at place 0
    assertTrue(conflicts.containsKey(0));
    List<Integer> conflict1 = conflicts.get(0);
    assertEquals(2, conflict1.size());
    assertTrue(conflict1.contains(0));
    assertTrue(conflict1.contains(2));

    // Check conflict at place 1
    assertTrue(conflicts.containsKey(1));
    List<Integer> conflict2 = conflicts.get(1);
    assertEquals(2, conflict2.size());
    assertTrue(conflict2.contains(1));
    assertTrue(conflict2.contains(2));
  }
}
