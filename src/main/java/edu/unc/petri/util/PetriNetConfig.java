package edu.unc.petri.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * The PetriNetConfig class represents the configuration of a Petri Net. It contains the initial
 * marking, incidence matrix, and time ranges for transitions.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-31-07
 */
public final class PetriNetConfig {

  /**
   * The initial marking of the Petri Net. Represents the number of tokens in each place at the
   * start.
   */
  public final int[] initialMarking;

  /**
   * The incidence matrix of the Petri Net. Represents the relationship between transitions and
   * places.
   */
  public final byte[][] incidence;

  /**
   * The time ranges for each transition in the Petri Net. Each transition has a minimum and maximum
   * time range.
   */
  public final long[][] timeRanges;

  /** The segments of the Petri net, defining logical divisions and thread responsibilities. */
  public final List<Segment> segments;

  /**
   * Constructs a PetriNetConfig object with the specified initial marking, incidence matrix, and
   * time ranges.
   *
   * @param initialMarking The initial marking of the Petri Net.
   * @param incidence The incidence matrix of the Petri Net.
   * @param timeRanges The time ranges for each transition.
   * @param segments The segments of the Petri net.
   */
  @JsonCreator
  public PetriNetConfig(
      @JsonProperty("initialMarking") int[] initialMarking,
      @JsonProperty("incidence") byte[][] incidence,
      @JsonProperty("timeRanges") long[][] timeRanges,
      @JsonProperty("segments") List<Segment> segments) {

    this.initialMarking = initialMarking;
    this.incidence = incidence;
    this.timeRanges = timeRanges;
    this.segments = segments;
  }
}
