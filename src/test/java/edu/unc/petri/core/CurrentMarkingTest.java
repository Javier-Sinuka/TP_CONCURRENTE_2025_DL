package edu.unc.petri.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

  @Test
  void getPlaceMarkingShouldReturnCorrectTokenCount() {
    CurrentMarking currentMarking = new CurrentMarking(new int[] {0, 8, 3});
    assertEquals(8, currentMarking.getPlaceMarking(1));
  }

  @Test
  void getPlaceMarkingShouldThrowExceptionForInvalidIndex() {
    CurrentMarking currentMarking = new CurrentMarking(new int[] {1});
    assertThrows(IndexOutOfBoundsException.class, () -> currentMarking.getPlaceMarking(-1));
  }

  @Test
  void setPlaceMarkingShouldUpdateSinglePlace() {
    CurrentMarking currentMarking = new CurrentMarking(new int[] {5, 5, 5});
    currentMarking.setPlaceMarking(1, 99);
    assertEquals(99, currentMarking.getPlaceMarking(1));
    assertArrayEquals(new int[] {5, 99, 5}, currentMarking.getMarking());
  }

  @Test
  void setPlaceMarkingShouldThrowExceptionForInvalidIndex() {
    CurrentMarking currentMarking = new CurrentMarking(new int[] {1, 2, 3});
    assertThrows(IndexOutOfBoundsException.class, () -> currentMarking.setPlaceMarking(-1, 10));
  }

  @Test
  void setPlaceMarkingShouldThrowExceptionForNegativeTokens() {
    CurrentMarking currentMarking = new CurrentMarking(new int[] {1});
    assertThrows(IllegalArgumentException.class, () -> currentMarking.setPlaceMarking(0, -5));
  }
}
