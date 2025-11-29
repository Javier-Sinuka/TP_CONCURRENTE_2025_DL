package edu.unc.petri.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NotEqualToPlaceInvariantEquationExceptionTest {

  @Test
  void testDefaultConstructor() {
    NotEqualToPlaceInvariantEquationException exception =
        new NotEqualToPlaceInvariantEquationException();
    assertNull(exception.getMessage());
  }

  @Test
  void testMessageConstructor() {
    String message = "Test message";
    NotEqualToPlaceInvariantEquationException exception =
        new NotEqualToPlaceInvariantEquationException(message);
    assertEquals(message, exception.getMessage());
  }
}
