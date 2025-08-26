package edu.unc.petri.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Segment class represents a logical division of a Petri net. It defines a set of transitions
 * and the number of concurrent threads that will attempt to fire them.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-14-08
 */
public class Segment {

  /** A string representing a unique, logical identifier for this segment. */
  public final String name;

  /**
   * An integer specifying the number of individual threads that should be created and assigned to
   * this specific segment.
   */
  public final int threadQuantity;

  /**
   * A JSON array of integers, listing the specific indices of the transitions that are part of this
   * segment.
   */
  public final int[] transitions;

  /**
   * Constructs a new Segment with the specified name, thread quantity, and transitions.
   *
   * @param name the name of the segment
   * @param threadQuantity the number of threads to be assigned to this segment
   * @param transitions an array of transition indices belonging to this segment
   */
  @JsonCreator
  public Segment(
      @JsonProperty("name") String name,
      @JsonProperty("threadQuantity") int threadQuantity,
      @JsonProperty("transitions") int[] transitions) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Segment name cannot be null or empty");
    }
    if (threadQuantity <= 0) {
      throw new IllegalArgumentException("Thread quantity must be a positive integer");
    }
    if (transitions == null || transitions.length == 0) {
      throw new IllegalArgumentException("Transitions array cannot be null or empty");
    }
    this.name = name;
    this.threadQuantity = threadQuantity;
    this.transitions = transitions;
  }
}
