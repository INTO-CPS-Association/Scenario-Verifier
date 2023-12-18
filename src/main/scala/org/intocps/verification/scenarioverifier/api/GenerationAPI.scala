package org.intocps.verification.scenarioverifier.api

import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier.cli.Z3.SMTEncoder
import org.intocps.verification.scenarioverifier.core.masterModel.FMI2ScenarioModel
import org.intocps.verification.scenarioverifier.core.masterModel.FMI3ScenarioModel
import org.intocps.verification.scenarioverifier.core.masterModel.MasterModel
import org.intocps.verification.scenarioverifier.core.masterModel.MasterModelFMI2
import org.intocps.verification.scenarioverifier.core.masterModel.MasterModelFMI3
import org.intocps.verification.scenarioverifier.core.masterModel.ScenarioModel
import org.intocps.verification.scenarioverifier.core.ScenarioLoader
import org.intocps.verification.scenarioverifier.core.ScenarioLoader.simplifyScenario
import org.intocps.verification.scenarioverifier.synthesizer.SynthesizerSimple

object GenerationAPI extends Logging {
  /*
   * Synthesize an orchestration algorithm with respect to the scenario model.
   */
  def synthesizeAlgorithm(name: String, scenarioModel: ScenarioModel): MasterModel = {
    scenarioModel match {
      case _: FMI3ScenarioModel =>
        synthesizeAlgorithmFMI3(name, scenarioModel.asInstanceOf[FMI3ScenarioModel])
      case _: FMI2ScenarioModel =>
        synthesizeAlgorithmFMI2(name, scenarioModel.asInstanceOf[FMI2ScenarioModel])
    }
  }

  private def synthesizeAlgorithmFMI3(name: String, scenarioModel: FMI3ScenarioModel): MasterModel = {
    val masterModel = MasterModelFMI3("master", scenarioModel, List.empty, List.empty, Map.empty, Map.empty, List.empty)
    logger.debug(s"""
         |Synthesizing algorithm for scenario with ${scenarioModel.fmus.size} FMUs and ${scenarioModel.connections.size} connections
         |The scenario has ${scenarioModel.eventEntrances.size} event entrances.
         |""".stripMargin)
    val modelWithAlgorithm = SMTEncoder.synthesizeAlgorithm(masterModel)
    logger.debug("Synthesized algorithm:" + modelWithAlgorithm.toConf(0))
    modelWithAlgorithm
  }

  private def synthesizeAlgorithmFMI2(name: String, scenarioModel: FMI2ScenarioModel): MasterModel = {
    logger.debug("Synthesizing algorithm for scenario: " + scenarioModel.toConf(0))
    val simplifiedScenario = simplifyScenario(scenarioModel)
    val synthesizer = new SynthesizerSimple(simplifiedScenario)
    val instantiationModel = ScenarioLoader.generateInstantiationInstructions(simplifiedScenario).toList
    val expandedInitModel = ScenarioLoader.generateEnterInitInstructions(simplifiedScenario) ++
      synthesizer.synthesizeInitialization() ++
      ScenarioLoader.generateExitInitInstructions(simplifiedScenario)
    val cosimStepModel = synthesizer.synthesizeStep()
    val terminateModel = ScenarioLoader.generateTerminateInstructions(simplifiedScenario).toList
    logger.info("Algorithm synthesized")
    MasterModelFMI2(name, simplifiedScenario, instantiationModel, expandedInitModel.toList, cosimStepModel, terminateModel)
  }
}
