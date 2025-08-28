package edu.unc.petri.exceptions;

/** Exception thrown when a simulation reaches its predefined limit. */
public class SimulationLimitReachedException extends Exception {
  /** Constructs a new SimulationLimitReachedException with no detail message. */
  public SimulationLimitReachedException() {
    super();
  }
}
