package edu.unc.petri.monitor;

/**
 * The MonitorInterface defines the methods that a monitor must implement. It is used to interact
 * with the Petri net simulator.
 *
 * @author Der Landsknecht
 * @version 1.0
 * @since 2025-29-07
 */
public interface MonitorInterface {

  /**
   * Fires a transition in the Petri net.
   *
   * @param t the transition to fire
   * @return true if the transition was successfully fired, false otherwise
   */
  boolean fireTransition(int t);
}
