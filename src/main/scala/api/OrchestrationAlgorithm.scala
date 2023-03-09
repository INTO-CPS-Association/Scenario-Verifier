package api

import core.SimulationInstruction

final case class OrchestrationAlgorithm(initialization: List[SimulationInstruction], step: List[SimulationInstruction])