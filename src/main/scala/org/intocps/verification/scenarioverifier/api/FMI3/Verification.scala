package org.intocps.verification.scenarioverifier.api.FMI3

import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier.cli.Z3.SMTEncoder
import org.intocps.verification.scenarioverifier.core.FMI3.{FMI3ScenarioModel, MasterModel3}

object Verification extends Logging {

  /**
   * Verifies whether the algorithm is correct with respect to the scenario model.
   *
   * @param masterModel the algorithm and scenario to verify
   * @return true if the algorithm is correct, false otherwise
   */
  def verifyAlgorithm(masterModel: MasterModel3): Boolean = {
    SMTEncoder.verifyAlgorithm(masterModel)
  }

  /**
   * Synthesize an orchestration algorithm and verify it with respect to the scenario model.
   *
   * @param scenarioModel the scenario
   * @return the synthesized algorithm in a MasterModel
   */
  def synthesizeAlgorithm(scenarioModel: FMI3ScenarioModel): MasterModel3 = {
    val masterModel = MasterModel3("master", scenarioModel, List.empty, List.empty, List.empty, Map.empty, List.empty)
    val modelWithAlgorithm = SMTEncoder.synthesizeAlgorithm(masterModel)
    SMTEncoder.verifyAlgorithm(modelWithAlgorithm)
    modelWithAlgorithm
  }

  def synthesizeAlgorithmWithStatistics(scenarioModel: FMI3ScenarioModel): MasterModel3 = {
    val masterModel = MasterModel3("master", scenarioModel, List.empty, List.empty, List.empty, Map.empty, List.empty)
    logger.debug(
      s"""
         |Synthesizing algorithm for scenario with ${scenarioModel.fmus.size} FMUs and ${scenarioModel.connections.size} connections
         |The scenario has ${scenarioModel.eventEntrances.size} event entrances.
         |""".stripMargin)
    logger.debug("Synthesizing algorithm")
    val modelWithAlgorithm = SMTEncoder.synthesizeAlgorithm(masterModel)
    logger.debug("Verifying algorithm")
    SMTEncoder.verifyAlgorithm(modelWithAlgorithm)
    modelWithAlgorithm
  }
}

