package edu.unc.petri.exceptions;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SimulationLimitReachedExceptionTest {

  @Test
  void testDefaultConstructor() {
    SimulationLimitReachedException exception = new SimulationLimitReachedException();
    assertNull(exception.getMessage());
  }
}
