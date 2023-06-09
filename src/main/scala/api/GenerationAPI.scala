package api

import core.ScenarioLoader.simplifyScenario
import core._
import org.apache.logging.log4j.scala.Logging
import synthesizer.SynthesizerSimple

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
    MasterModel(name, simplifiedScenario, instantiationModel, expandedInitModel.toList, cosimStepModel, terminateModel)
  }
}