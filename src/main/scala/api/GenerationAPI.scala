package api

import core._
import synthesizer.SynthesizerSimple

object GenerationAPI {
  /*
    * Synthesize an orchestration algorithm with respect to the scenario model.
   */
  def synthesizeAlgorithm(name: String, scenarioModel: ScenarioModel): MasterModel = {
    val synthesizer = new SynthesizerSimple(scenarioModel)
    val instantiationModel = ScenarioLoader.generateInstantiationInstructions(scenarioModel).toList
    val expandedInitModel = ScenarioLoader.generateEnterInitInstructions(scenarioModel) ++
      synthesizer.synthesizeInitialization() ++
      ScenarioLoader.generateExitInitInstructions(scenarioModel)
    val cosimStepModel = synthesizer.synthesizeStep()
    val terminateModel = ScenarioLoader.generateTerminateInstructions(scenarioModel).toList
    MasterModel(name, scenarioModel, instantiationModel, expandedInitModel.toList, cosimStepModel, terminateModel)
  }
}