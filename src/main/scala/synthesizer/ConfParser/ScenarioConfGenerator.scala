package synthesizer.ConfParser

import core._
import org.apache.logging.log4j.scala.Logging

object ScenarioConfGenerator extends Logging {
  def generate(model: MasterModel, name: String): String = {
    val scenario = model.scenario
    val builder = new StringBuilder(generateScenario(scenario, name))
    builder.append(s"initialization = [${generateInit(model.initialization)}]\n")
    builder.append(s"cosim-step = {${generateCoSimStep(model.cosimStep)}}")
    builder.toString()
  }

  def generateCoSimStep(steps: Map[String, List[CosimStepInstruction]]): String = {
    val builder = new StringBuilder()
    steps.foreach(keyValue => {
      builder.append(s"${keyValue._1} = ")
      builder.append(s"[${generateStep(keyValue._2)}]\n")
    })
    builder.toString()
  }

  def generateInit(inits: List[InitializationInstruction]): String = {
    inits.map {
      case InitSet(port) => s"{set: ${generatePort(port)}}\n"
      case InitGet(port) => s"{get: ${generatePort(port)}}\n"
      case EnterInitMode(_) => ""
      case ExitInitMode(_) => ""
      case AlgebraicLoopInit(ports, iterate) => s"{loop: { \n until-converged: ${ports.map(generatePort).mkString("[", ",", "]\n")} iterate: [${generateInit(iterate)}]} \n }\n"
    }.mkString("\n")
  }

  def formatStepSize(by: StepSize): String = by match {
    case DefaultStepSize() => ""
    case RelativeStepSize(fmu) => f", by-same-as: $fmu"
    case AbsoluteStepSize(h) => f", by: $h"
  }

  def generateStep(steps: List[CosimStepInstruction]): String = {
    steps.map {
      case Set(port) => s"{set: ${generatePort(port)}}\n"
      case Get(port) => s"{get: ${generatePort(port)}}\n"
      case GetTentative(port) => s"{get-tentative: ${generatePort(port)}}\n"
      case SetTentative(port) => s"{set-tentative: ${generatePort(port)}}\n"
      case Step(fmu, by) => s"{step: ${fmu} ${formatStepSize(by)}}\n"
      case SaveState(fmu) => s"{save-state: ${fmu}}\n"
      case RestoreState(fmu) => s"{restore-state: ${fmu}}\n"
      case AlgebraicLoop(untilConverged, iterate, ifRetryNeeded) => s"{loop: { \n until-converged: ${untilConverged.map(generatePort).mkString("[", ",", "]")} \n iterate: [${generateStep(iterate)}] \n if-retry-needed: [${generateStep(ifRetryNeeded)}]} \n }\n"
      case StepLoop(untilStepAccept, iterate, ifRetryNeeded) => s"{loop: { \n until-step-accept: ${untilStepAccept.mkString("[", ",", "]")} \n iterate: [${generateStep(iterate)}] \n if-retry-needed: [${generateStep(ifRetryNeeded)}]} \n }\n"
      case NoOP => ""
    }.mkString("\n")
  }

  def generatePort(port: PortRef): String = port.fmu + "." + port.port

  def generateConnections(connections: List[ConnectionModel]): String = {
    connections.map(o => f"${generatePort(o.srcPort)} -> ${generatePort(o.trgPort)}").mkString("connections = [\n", "\n", "]\n")
  }

  def generateFMUs(fmus: Map[String, FmuModel]): String = {
    fmus.map(o => {
      val can_reject_string = if (o._2.canRejectStep) "can-reject-step = true,\n" else ""
      val inputs = o._2.inputs.map(i => f"${i._1} = {reactivity=${i._2.reactivity.toString}}").mkString("inputs = {\n", "\n", "},\n")
      val outputs = o._2.outputs.map(e => f"${e._1} = {${e._2.dependenciesInit.mkString("dependencies-init=[", ",", "]")}, ${e._2.dependencies.mkString("dependencies=[", ",", "]")}}").mkString("outputs = {\n", "\n", "}\n")
      f"${o._1} = { \n ${can_reject_string} ${inputs} ${outputs} }"
    }).mkString("fmus = {\n", "\n", "}\n")
  }

  def generateScenario(scenario: ScenarioModel, name: String): String = {
    val builder = new StringBuilder()
    builder.addAll(f"name = ${name}\n")
    builder.addAll("scenario = {\n")
    builder.addAll(generateFMUs(scenario.fmus))
    builder.addAll(generateConnections(scenario.connections))
    builder.addAll("}\n")
    builder.toString()
  }
}
