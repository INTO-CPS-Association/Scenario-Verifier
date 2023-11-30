package org.intocps.verification.scenarioverifier.api

import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier
import org.intocps.verification.scenarioverifier.core.ScenarioLoader.simplifyScenario
import org.intocps.verification.scenarioverifier.core.{MasterModel, ScenarioLoader, ScenarioModel}
import org.intocps.verification.scenarioverifier.synthesizer.SynthesizerSimple

object GenerationAPI extends Logging {

  /*
    * Synthesize an orchestration algorithm with respect to the scenario model.
   */
  def synthesizeAlgorithm(name: String, scenarioModel: ScenarioModel): MasterModel = {
    logger.info("Synthesizing algorithm")
    val simplifiedScenario = simplifyScenario(scenarioModel)
    val synthesizer = new SynthesizerSimple(simplifiedScenario)
    val instantiationModel = ScenarioLoader.generateInstantiationInstructions(simplifiedScenario).toList
    val expandedInitModel = ScenarioLoader.generateEnterInitInstructions(simplifiedScenario) ++
      synthesizer.synthesizeInitialization() ++
      ScenarioLoader.generateExitInitInstructions(simplifiedScenario)
    val cosimStepModel = synthesizer.synthesizeStep()
    val terminateModel = ScenarioLoader.generateTerminateInstructions(simplifiedScenario).toList
    logger.info("Algorithm synthesized")
    scenarioverifier.core.MasterModel(name, simplifiedScenario, instantiationModel, expandedInitModel.toList, cosimStepModel, terminateModel)
  }
}