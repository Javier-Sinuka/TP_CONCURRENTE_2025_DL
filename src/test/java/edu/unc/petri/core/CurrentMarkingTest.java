package edu.unc.petri.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CurrentMarkingTest {

  @Test
  void constructorShouldInitializeMarking() {
    int[] initialMarking = {1, 0, 5, 2};
    CurrentMarking currentMarking = new CurrentMarking(initialMarking);
    assertArrayEquals(initialMarking, currentMarking.getMarking());
  }

  @Test
  void constructorShouldThrowExceptionForNullMarking() {
    assertThrows(IllegalArgumentException.class, () -> new CurrentMarking(null));
  }

  @Test
  void constructorShouldThrowExceptionForEmptyMarking() {
    assertThrows(IllegalArgumentException.class, () -> new CurrentMarking(new int[] {}));
  }

  @Test
  void setMarkingShouldUpdateTokens() {
    CurrentMarking currentMarking = new CurrentMarking(new int[] {1, 1, 1});
    int[] newMarking = {2, 3, 4};
    currentMarking.setMarking(newMarking);
    assertArrayEquals(newMarking, currentMarking.getMarking());
  }
}
