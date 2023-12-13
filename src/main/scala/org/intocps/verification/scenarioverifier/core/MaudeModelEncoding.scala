package org.intocps.verification.scenarioverifier.core

import Reactivity.reactive

class MaudeModelEncoding(model: MasterModel) {

  val scenario: String = generateScenario(model.scenario)

  private def generateInputs(inputs: Map[String, InputPortModel]): String = {
    if (inputs.isEmpty) "none"
    else
      inputs
        .map(input => {
          s"(< ${'"'}${input._1}${'"'} : Input | value : < 0 >, type : integer, time : 0, contract : ${getReactivity(input)}, status : Undef  >)"
        })
        .mkString(" ")
  }

  private def getReactivity(input: (String, InputPortModel)): String = if (input._2.reactivity == reactive) "reactive" else "delayed"

  // Should be improved
  private def getFeedthrough(dependencies: List[String]): String = {
    if (dependencies.isEmpty) return "empty"
    dependencies.map(i => s"${'"'}$i${'"'}").mkString("( ", " , ", " )")
  }

  private def generateOutputs(outputs: Map[String, OutputPortModel]): String = {
    if (outputs.isEmpty) "none"
    else
      outputs
        .map(output => {
          s"(< ${'"'}${output._1}${'"'} : Output | value : < 0 >, type : integer, time : 0, status : Undef, dependsOn : ${getFeedthrough(
              output._2.dependencies)} >)"
        })
        .mkString(" ")
  }

  def generateScenario(scenario: ScenarioModel): String = {
    val connections = scenario.connections
      .map(connectionModel => {
        s"(${'"'}${connectionModel.srcPort.fmu}${'"'} ! ${'"'}${connectionModel.srcPort.port}${'"'} ==> ${'"'}${connectionModel.trgPort.fmu}${'"'} ! ${'"'}${connectionModel.trgPort.port}${'"'})"
      })
      .mkString("eq externalConnection = ", " ", " .\n\n")

    val scenarioString = connections ++ scenario.fmus
      .map(fmu => {
        s"(< ${'"'}${fmu._1}${'"'} : SU | path : ${'"'}${fmu._2.path}${'"'}, parameters : empty, localState : empty, time : 0, inputs : ${generateInputs(
            fmu._2.inputs)}, outputs : ${generateOutputs(fmu._2.outputs)}, fmistate : Instantiated, canReject : ${if (fmu._2.canRejectStep) "true"
          else "false"} >)"
      })
      .mkString("eq simulationUnits = ", "\n\n \t ", " .\n")

    scenarioString ++ scenario.fmus
      .map(fmu => {
        s"eq step(< ${'"'}${fmu._1}${'"'} : SU | time : TIME, outputs : OUTPUTS >, STEPSIZE) = < ${'"'}${fmu._1}${'"'} : SU | time : (STEPSIZE + TIME), outputs : undefPorts(OUTPUTS, (STEPSIZE + TIME)) > ."
      })
      .mkString("\n", "\n\n \t ", "\n")

  }

}
