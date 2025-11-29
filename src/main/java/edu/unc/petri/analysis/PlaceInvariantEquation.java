package edu.unc.petri.analysis;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Represents a place-invariant equation for a Petri net, of the form:
 *
 * <pre>
 *   sum_{place in coefficients} (coeff[place] * M(place)) == result
 * </pre>
 *
 * <p>Places are indexed from 0.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-31-08
 */
public final class PlaceInvariantEquation {

  /** Sorted by place index for deterministic behavior. */
  private final TreeMap<Integer, Integer> coefficients;

  /** The constant RHS of the equation. */
  private final int result;

  /**
   * Constructs a PlaceInvariantEquation with the given coefficients and result.
   *
   * @param coefficients map from place index (0-based) to coefficient (non-zero)
   * @param result the constant RHS (c^T * M for some invariant vector c and marking M)
   */
  public PlaceInvariantEquation(Map<Integer, Integer> coefficients, int result) {
    if (coefficients == null) {
      throw new IllegalArgumentException("Coefficients map cannot be null");
    }
    if (coefficients.isEmpty()) {
      throw new IllegalArgumentException("Coefficients map cannot be empty");
    }
    if (result < 0) {
      throw new IllegalArgumentException("Result must be non-negative");
    }

    this.coefficients = new TreeMap<>();

    for (Map.Entry<Integer, Integer> e : coefficients.entrySet()) {
      Integer k = Objects.requireNonNull(e.getKey(), "Coefficient map contains a null key");
      Integer v = Objects.requireNonNull(e.getValue(), "Coefficient for place " + k + " is null");

      if (k < 0) {
        throw new IllegalArgumentException("Place indices must be non-negative: " + k);
      }

      if (v == 0) {
        // We store only non-zeros to keep LHS clean
        continue;
      }

      this.coefficients.put(k, v);
    }

    this.result = result;
  }

  /**
   * Tests whether the provided marking satisfies the place-invariant equation.
   *
   * @param marking array of tokens per place (0-based indices)
   * @return true if sum(coeff[place] * marking[place]) == result, otherwise false
   * @throws IllegalArgumentException if marking is null or too small for any referenced place
   */
  public boolean testPlaceInvariant(int[] marking) {
    if (marking == null) {
      throw new IllegalArgumentException("Marking cannot be null");
    }
    if (marking.length == 0) {
      throw new IllegalArgumentException("Marking cannot be empty");
    }

    long lhs = 0L;

    for (Map.Entry<Integer, Integer> e : coefficients.entrySet()) {
      int place = e.getKey();
      int coeff = e.getValue();
      if (place >= marking.length) {
        throw new IllegalArgumentException(
            "Marking length " + marking.length + " is too small for place index " + place);
      }

      lhs += (long) coeff * (long) marking[place];

      if (lhs > Integer.MAX_VALUE || lhs < Integer.MIN_VALUE) {
        // Early avoid overflow surprises in comparison
        return lhs == (long) result;
      }
    }

    return lhs == (long) result;
  }

  /** Returns an unmodifiable view of the coefficients map. */
  public Map<Integer, Integer> getCoefficients() {
    return Collections.unmodifiableMap(coefficients);
  }

  public int getResult() {
    return result;
  }

  /**
   * Returns a pretty string representation of the equation.
   *
   * <p>Example output: {@code 2*M(P0) + M(P3) + 5*M(P7) = 12}
   *
   * <ul>
   *   <li>Each term is formatted as {@code [coefficient]*M(P[index])}.
   *   <li>Coefficients of 1 are omitted; -1 is shown as {@code -}.
   *   <li>Terms are joined with {@code +}.
   *   <li>If there are no coefficients, returns {@code 0 = [result]}.
   * </ul>
   *
   * @return the formatted equation string
   */
  @Override
  public String toString() {
    if (coefficients.isEmpty()) {
      return "0 = " + result;
    }

    StringBuilder sb = new StringBuilder();
    boolean first = true;

    for (Map.Entry<Integer, Integer> e : coefficients.entrySet()) {

      if (!first) {
        sb.append(" + ");
      }
      first = false;

      int coeff = e.getValue();
      if (Math.abs(coeff) != 1) {
        sb.append(coeff).append("*");
      } else if (coeff == -1) {
        sb.append("-");
      }

      int place = e.getKey();
      sb.append("M(P").append(place).append(")");
    }

    sb.append(" = ").append(result);

    return sb.toString();
  }
}
