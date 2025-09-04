package edu.unc.petri.exceptions;

/**
 * Exception thrown when a marking does not satisfy a computed place-invariant equation.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-31-08
 */
public class NotEqualToPlaceInvariantEquationException extends Exception {

  /** The pretty-printed equation that was checked, if provided. */
  private final String equation;

  /** The computed left-hand side value (c^T * M), if provided. */
  private final long lhs;

  /** The expected right-hand side value, if provided. */
  private final long rhs;

  /** Constructs the exception with no details. */
  public NotEqualToPlaceInvariantEquationException() {
    super();
    this.equation = null;
    this.lhs = 0L;
    this.rhs = 0L;
  }

  /** Constructs the exception with a custom message. */
  public NotEqualToPlaceInvariantEquationException(String message) {
    super(message);
    this.equation = null;
    this.lhs = 0L;
    this.rhs = 0L;
  }

  /**
   * Constructs the exception with contextual details.
   *
   * @param equation a human-readable representation of the equation (e.g., "2*M(p0) + M(p3) = 5")
   * @param lhs the computed left-hand side value (c^T * M)
   * @param rhs the expected right-hand side value
   */
  public NotEqualToPlaceInvariantEquationException(String equation, long lhs, long rhs) {
    super(buildMessage(equation, lhs, rhs));
    this.equation = equation;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  private static String buildMessage(String equation, long lhs, long rhs) {
    String eq = (equation == null || equation.isEmpty()) ? "Place-invariant equation" : equation;
    return eq + " violated: LHS=" + lhs + ", RHS=" + rhs;
  }

  /** Returns the equation string if available. */
  public String getEquation() {
    return equation;
  }

  /** Returns the computed left-hand side value if available. */
  public long getLhs() {
    return lhs;
  }

  /** Returns the expected right-hand side value if available. */
  public long getRhs() {
    return rhs;
  }
}
