package edu.unc.petri.core;

/**
 * The CurrentMarking class represents the current marking of a Petri net. It holds the number of
 * tokens in each place and provides methods to manipulate and retrieve the marking.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public class CurrentMarking {
  /**
   * The current marking of the Petri net, represented as an array of integers where each index
   * corresponds to a place and the value at that index represents the number of tokens in that
   * place.
   */
  private int[] tokens;

  /**
   * Constructs a CurrentMarking from a given path. The marking is initialized with the number of
   * tokens in each place defined in the path.
   *
   * @param initialMarking An array of integers representing the initial marking of the Petri net.
   */
  public CurrentMarking(int[] initialMarking) {
    // TODO: Initialize the current marking based on the path
  }

  /**
   * Sets the current marking of the Petri net to a new vector of tokens.
   *
   * @param tokens The new vector of tokens, where each index corresponds to a place.
   */
  void setMarking(int[] tokens) {
    this.tokens = tokens;
  }

  /**
   * Sets the number of tokens in a specific place.
   *
   * @param placeIndex The index of the place to set the token count for.
   * @param tokenCount The number of tokens to set in the specified place.
   */
  void setPlaceMarking(int placeIndex, int tokenCount) {
    // TODO: Implement logic to set the number of tokens in the specified place
  }

  /**
   * Retrieves the current marking of the Petri net.
   *
   * @return An array of integers representing the number of tokens in each place.
   */
  public int[] getMarking() {
    return tokens;
  }

  /**
   * Retrieves the number of tokens in a specific place.
   *
   * @param placeIndex The index of the place to retrieve the token count for.
   * @return The number of tokens in the specified place.
   */
  public int getPlaceMarking(int placeIndex) {
    // TODO: Add validation and error handling for invalid placeIndex
    return tokens[placeIndex];
  }
}
