package api

import core.ScenarioLoader.{generateEnterInitInstructions, generateExitInitInstructions, generateInstantiationInstructions, generateTerminateInstructions}
import core.{CosimStepInstruction, InitializationInstruction, MasterModel, ScenarioModel}
import synthesizer.SynthesizerSimple

object GenerationAPI {

  private def synthesizeInitialization(scenarioModel: ScenarioModel): List[InitializationInstruction] = {
    val synthesizer = new SynthesizerSimple(scenarioModel)
    synthesizer.synthesizeInitialization()
  }

  private def synthesizeCoSimStep(scenarioModel: ScenarioModel): Map[String, List[CosimStepInstruction]] = {
    val synthesizer = new SynthesizerSimple(scenarioModel)
    synthesizer.synthesizeStep()
  }

  def synthesizeAlgorithm(name: String, scenarioModel: ScenarioModel): MasterModel = {
    val instantiationModel = generateInstantiationInstructions(scenarioModel).toList
    val expandedInitModel = generateEnterInitInstructions(scenarioModel) ++ synthesizeInitialization(scenarioModel) ++ generateExitInitInstructions(scenarioModel)
    val cosimStepModel = synthesizeCoSimStep(scenarioModel)
    val terminateModel = generateTerminateInstructions(scenarioModel).toList
    MasterModel(name, scenarioModel, instantiationModel, expandedInitModel.toList, cosimStepModel, terminateModel)
  }
}