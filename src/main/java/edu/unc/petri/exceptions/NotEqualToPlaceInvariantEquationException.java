package edu.unc.petri.exceptions;

/**
 * Exception thrown when a marking does not satisfy a computed place-invariant equation.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-31-08
 */
public class NotEqualToPlaceInvariantEquationException extends Exception {

  /** Constructs the exception with no details. */
  public NotEqualToPlaceInvariantEquationException() {
    super();
  }

  /** Constructs the exception with a custom message. */
  public NotEqualToPlaceInvariantEquationException(String message) {
    super(message);
  }
}
