package org.intocps.verification.scenarioverifier.api

import org.intocps.verification.scenarioverifier.core.SimulationInstruction

final case class OrchestrationAlgorithm(initialization: List[SimulationInstruction], step: List[SimulationInstruction])