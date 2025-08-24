package edu.unc.petri.policy;

import java.util.ArrayList;

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
   * Chooses a transition to fire based on the enabled transitions.
   *
   * @param transitions an ArrayList of integers representing the enabled transitions. Each element
   *     contains the number of the enabled transition.
   * @return the number of the chosen transition
   */
  int choose(ArrayList<Integer> transitions);
}
