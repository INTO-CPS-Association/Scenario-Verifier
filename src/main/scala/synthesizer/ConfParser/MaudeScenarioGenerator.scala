package synthesizer.ConfParser

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path, Paths}

import core.Reactivity.reactive
import core.{InputPortModel, MasterModel, OutputPortModel, ScenarioModel}
import org.apache.logging.log4j.scala.Logging
import synthesizer.ReactiveLoop

import scala.reflect.io.Directory

object MaudeScenarioGenerator extends Logging {

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
      s"('${connectionModel.srcPort.fmu} ! '${connectionModel.srcPort.port} ==> !${connectionModel.trgPort.fmu} ! '${connectionModel.trgPort.port})"
    }).mkString("eq externalConnection = ", " ", " .\n\n")

    connections ++ scenario.fmus.map(fmu => {
      (s"(<'${fmu._1} : SU | time : 0, inputs : ${generateInputs(fmu._2.inputs)}, outputs : ${generateOutputs(fmu._2.outputs)}, state : Instantiated, canReject : ${if (fmu._2.canRejectStep) "true" else "false"} >)")
    }).mkString("eq simulationUnits = ", "\n ", " .\n")
  }

  def generateScenario(scenario: ScenarioModel, name: String): Unit = {
    val builder = new StringBuilder(generateScenario(scenario))
    val outputFolder = System.getProperty("user.home") + "/Desktop" + "/MaudeScenarios"
    val path = Paths.get(outputFolder)
    if (!Files.exists(path))
      Files.createDirectory(path)
    val textFile = new File(path.toString, name + ".txt")
    val pw = new PrintWriter(textFile)
    pw.write(builder.toString())
    pw.close
  }

}
