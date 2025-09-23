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

  @Test
  void testFullConstructor() {
    String equation = "2*M(p0) + M(p3) = 5";
    long lhs = 10;
    long rhs = 5;
    NotEqualToPlaceInvariantEquationException exception =
        new NotEqualToPlaceInvariantEquationException(equation, lhs, rhs);
    assertEquals("2*M(p0) + M(p3) = 5 violated: LHS=10, RHS=5", exception.getMessage());
    assertEquals(equation, exception.getEquation());
    assertEquals(lhs, exception.getLhs());
    assertEquals(rhs, exception.getRhs());
  }
}
