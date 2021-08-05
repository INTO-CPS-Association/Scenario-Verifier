package core

import core.Reactivity.reactive

class MaudeModelEncoding(model: MasterModel) {

  val scenario : String = generateScenario(model.scenario)

  def generateInputs(inputs: Map[String, InputPortModel]): String = {
    if (inputs.isEmpty) "none"
    else inputs.map(input => {
      s"(< '${input._1} : Input | time : 0, type : ${getReactivity(input)}, status : Undef  >)"
    }).mkString(" ")
  }

  private def getReactivity(input: (String, InputPortModel)): String = if (input._2.reactivity == reactive) "r" else "d"

  //Should be improved
  def getFeedthrough(dependencies: List[String]): String = {
    if (dependencies.isEmpty) return "emptySet"
    dependencies.map(i => s"'$i").mkString("( ", " :: ", " )")
  }

  def generateOutputs(outputs: Map[String, OutputPortModel]): String = {
    if (outputs.isEmpty) "none"
    else outputs.map(output => {
      s"(< '${output._1} : Output | time : 0, status : Undef, dependsOn : ${getFeedthrough(output._2.dependencies)} >)"
    }).mkString(" ")
  }

  def generateScenario(scenario: ScenarioModel): String = {
    val connections = scenario.connections.map(connectionModel => {
      s"('${connectionModel.srcPort.fmu} ! '${connectionModel.srcPort.port} ==> '${connectionModel.trgPort.fmu} ! '${connectionModel.trgPort.port})"
    }).mkString("eq externalConnection = ", " ", " .\n\n")

    connections ++ scenario.fmus.map(fmu => {
      (s"(< '${fmu._1} : SU | time : 0, inputs : ${generateInputs(fmu._2.inputs)}, outputs : ${generateOutputs(fmu._2.outputs)}, state : Instantiated, canReject : ${if (fmu._2.canRejectStep) "true" else "false"} >)")
    }).mkString("eq simulationUnits = ", "\n\n \t ", " .\n")

  }

}
