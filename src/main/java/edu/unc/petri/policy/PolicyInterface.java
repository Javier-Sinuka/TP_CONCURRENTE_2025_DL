package edu.unc.petri.policy;

import java.util.List;

/**
 * The interface for a policy that determines which transition to fire. This interface defines a
 * method to choose a transition based on the enabled transitions.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public interface PolicyInterface {

  /**
   * Selects and returns a transition to fire from the list of enabled transitions.
   *
   * @param transitions an ArrayList of integers, each representing an enabled transition number
   * @return the number corresponding to the chosen transition to fire
   */
  int choose(List<Integer> transitions);
}
