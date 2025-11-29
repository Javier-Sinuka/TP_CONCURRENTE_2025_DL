package edu.unc.petri.analysis;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalysisManagerTest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  private PetriNetAnalyzer mockPetriNetAnalyzer;
  private ConflictAnalyzer mockConflictAnalyzer;
  private AnalysisManager analysisManager;

  @BeforeEach
  void setUp() {
    System.setOut(new PrintStream(outContent));
    mockPetriNetAnalyzer = mock(PetriNetAnalyzer.class);
    mockConflictAnalyzer = mock(ConflictAnalyzer.class);
    analysisManager = new AnalysisManager(mockPetriNetAnalyzer, mockConflictAnalyzer);
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void printAnalysisReport_noInvariantsOrConflicts() {
    when(mockPetriNetAnalyzer.getTransitionInvariants()).thenReturn(Collections.emptyList());
    when(mockPetriNetAnalyzer.getPlaceInvariants()).thenReturn(Collections.emptyList());
    when(mockPetriNetAnalyzer.getPlaceInvariantEquations()).thenReturn(Collections.emptyList());
    when(mockConflictAnalyzer.getConflicts()).thenReturn(Collections.emptyMap());

    analysisManager.printAnalysisReport();

    String output = outContent.toString();
    assertTrue(output.contains("No T-Invariants found."));
    assertTrue(output.contains("No P-Invariants found."));
    assertTrue(output.contains("No P-Invariant Equations found."));
    assertTrue(output.contains("No structural conflicts found."));
  }

  @Test
  void printAnalysisReport_withTransitionInvariants() {
    List<ArrayList<Integer>> transitionInvariants = new ArrayList<>();
    ArrayList<Integer> invariant1 = new ArrayList<>();
    invariant1.add(0);
    invariant1.add(2);
    transitionInvariants.add(invariant1);
    when(mockPetriNetAnalyzer.getTransitionInvariants()).thenReturn(transitionInvariants);

    analysisManager.printAnalysisReport();

    String output = outContent.toString();
    assertTrue(output.contains("Found 1 T-Invariant(s):"));
    assertTrue(output.contains("Invariant 1: {T0, T2}"));
  }

  @Test
  void printAnalysisReport_withPlaceInvariants() {
    List<ArrayList<Integer>> placeInvariants = new ArrayList<>();
    ArrayList<Integer> invariant1 = new ArrayList<>();
    invariant1.add(1);
    invariant1.add(3);
    placeInvariants.add(invariant1);
    when(mockPetriNetAnalyzer.getPlaceInvariants()).thenReturn(placeInvariants);

    analysisManager.printAnalysisReport();

    String output = outContent.toString();
    assertTrue(output.contains("Found 1 P-Invariant(s):"));
    assertTrue(output.contains("Invariant 1: {P1, P3}"));
  }

  @Test
  void printAnalysisReport_withPlaceInvariantEquations() {
    PlaceInvariantEquation mockEquation = mock(PlaceInvariantEquation.class);
    when(mockEquation.toString()).thenReturn("P0 + P1 = 1");
    List<PlaceInvariantEquation> equations = Collections.singletonList(mockEquation);
    when(mockPetriNetAnalyzer.getPlaceInvariantEquations()).thenReturn(equations);

    analysisManager.printAnalysisReport();

    String output = outContent.toString();
    assertTrue(output.contains("Found 1 P-Invariant Equation(s):"));
    assertTrue(output.contains("Equation 1: P0 + P1 = 1"));
  }

  @Test
  void printAnalysisReport_withConflicts() {
    Map<Integer, List<Integer>> conflicts = new HashMap<>();
    List<Integer> conflictingTransitions = new ArrayList<>();
    conflictingTransitions.add(0);
    conflictingTransitions.add(1);
    conflicts.put(0, conflictingTransitions);
    when(mockConflictAnalyzer.getConflicts()).thenReturn(conflicts);

    analysisManager.printAnalysisReport();

    String output = outContent.toString();
    assertTrue(output.contains("Found 1 unique structural conflict(s):"));
    assertTrue(output.contains("P0 is a shared input for: {T0, T1}"));
  }

  @Test
  void formatPlaceList_singlePlace() {
    Map<Integer, List<Integer>> conflicts = new HashMap<>();
    conflicts.put(0, Collections.singletonList(0)); // P0 conflicts on T0
    when(mockConflictAnalyzer.getConflicts()).thenReturn(conflicts);
    analysisManager.printAnalysisReport();
    String output = outContent.toString();
    Map<Integer, List<Integer>> complexConflicts = new HashMap<>();
    complexConflicts.put(0, Arrays.asList(0, 1)); // P0 for T0, T1
    complexConflicts.put(2, Arrays.asList(0, 1)); // P2 for T0, T1
    when(mockConflictAnalyzer.getConflicts()).thenReturn(complexConflicts);
    analysisManager.printAnalysisReport();
    output = outContent.toString();
    assertTrue(output.contains("P0 and P2 are a shared input for: {T0, T1}"));
  }

  @Test
  void formatPlaceList_threePlaces() {
    Map<Integer, List<Integer>> conflicts = new HashMap<>();
    conflicts.put(0, Arrays.asList(0, 1));
    conflicts.put(1, Arrays.asList(0, 1));
    conflicts.put(2, Arrays.asList(0, 1));
    when(mockConflictAnalyzer.getConflicts()).thenReturn(conflicts);
    analysisManager.printAnalysisReport();
    String output = outContent.toString();
    assertTrue(output.contains("P0, P1, and P2 are a shared input for: {T0, T1}"));
  }
}
