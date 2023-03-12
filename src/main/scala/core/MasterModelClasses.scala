package core

object Reactivity extends Enumeration {
  type Reactivity = Value
  val reactive, delayed, noPort = Value
}

import core.Reactivity.Reactivity
import io.circe._
import io.circe.generic.semiauto._
import io.circe.generic.auto._

trait ConfElement {
  def toConf(indentationLevel: Int): String

  private val indentation = "  "

  private def shouldBeSanitized(str: String): Boolean =
    str.replaceAll("\\W", "") != str

  protected def sanitizeString(str: String): String =
    if (shouldBeSanitized(str)) "\"" + str + "\""
    else str

  protected def indentBy(indentationLevel: Int): String = indentation * indentationLevel

  protected def generatePort(port: PortRef): String = sanitizeString(port.fmu + "." + port.port)

  protected def toArray(elements: List[String], delimiter: String = ","): String = {
    elements.filterNot(s => s.isEmpty || s.isBlank).mkString("[", delimiter, "]")
  }

  protected def toMap(elements: List[String]): String = {
    elements.mkString("{", ",", "}")
  }
}

trait UppaalModel {
  protected def sanitize(s: String): String = s
    .replaceAll("\\W", "")

  def fmuPortName(portRef: PortRef) = s"${sanitize(portRef.fmu)}_${sanitize(portRef.port)}"

  def toUppaal: String
}

case class InputPortModel(reactivity: Reactivity) extends ConfElement {
  override def toConf(indentationLevel: Int = 0): String = s"{reactivity=${reactivity.toString}}"
}

final case class OutputPortModel(dependenciesInit: List[String], dependencies: List[String]) extends UppaalModel with ConfElement {
  override def toUppaal: String = s"{${dependencies.mkString(",")}}"

  override def toConf(indentationLevel: Int = 0): String = s"{dependencies-init=${toArray(dependenciesInit)}, dependencies=${toArray(dependencies)}}"
}

final case class FmuModel(
                           inputs: Map[String, InputPortModel],
                           outputs: Map[String, OutputPortModel],
                           canRejectStep: Boolean,
                           path: String
                         ) extends ConfElement {
  require(inputs.keySet.intersect(outputs.keySet).isEmpty, "FMU inputs and outputs must be disjoint")

  override def toConf(indentationLevel: Int): String = {
    s"""
       |${indentBy(indentationLevel)}{
       |${indentBy(indentationLevel + 1)}can-reject-step = $canRejectStep,
       |${indentBy(indentationLevel + 1)}inputs = {
       |${indentBy(indentationLevel + 2)}${inputs.map { case (port, inputPortModel) => s"${sanitizeString(port)} = ${inputPortModel.toConf()}" }.mkString("\n")}
       |${indentBy(indentationLevel + 1)}},
       |${indentBy(indentationLevel + 1)}outputs = {
       |${indentBy(indentationLevel + 2)}${outputs.map { case (port, outputPortModel) => s"${sanitizeString(port)} = ${outputPortModel.toConf()}" }.mkString("\n")}
       |${indentBy(indentationLevel + 1)}}
       |${indentBy(indentationLevel)}}""".stripMargin

  }
}

final case class ConnectionModel(
                                  srcPort: PortRef,
                                  trgPort: PortRef,
                                ) extends UppaalModel with ConfElement {
  require(srcPort.fmu != trgPort.fmu, "srcPort and trgPort must not be in the same FMU")

  override def toUppaal: String =
    f"""{${sanitize(srcPort.fmu)}, ${sanitize(fmuPortName(srcPort))}, ${sanitize(trgPort.fmu)}, ${sanitize(fmuPortName(trgPort))}}"""

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}${generatePort(srcPort)} -> ${generatePort(trgPort)}"

}

final case class ScenarioModel(
                                fmus: Map[String, FmuModel],
                                config: AdaptiveModel,
                                connections: List[ConnectionModel],
                                maxPossibleStepSize: Int
                              ) extends ConfElement {

  require(fmus.nonEmpty, "fmus must not be empty")
  require(maxPossibleStepSize > 0, "maxPossibleStepSize must be greater than 0")

  def enrich(): ScenarioModel = {
    val enrichedFmus = fmus.map { case (fmu, fmuModel) =>
      connections.filter(_.srcPort.fmu == fmu |).map(
        con =>
        if(!fmuModel.inputs.contains(con.srcPort.port)) fmuModel.inputs + (con.srcPort.port -> InputPortModel(Reactivity.delayed))
      )
      connections.filter().map(
        con =>
        if(!fmuModel.outputs.contains(con.trgPort.port)) fmuModel.outputs + (con.trgPort.port -> OutputPortModel(List(), List()))



        .foreach(c => require(fmuModel.outputs.contains(c.srcPort.port), s"FMU $fmu does not have output port ${c.srcPort.port}"))
      connections.filter(_.trgPort.fmu == fmu).foreach(c => require(fmuModel.inputs.contains(c.trgPort.port), s"FMU $fmu does not have input port ${c.trgPort.port}"))

      val enrichedInputs = fmuModel.inputs.map { case (port, inputPortModel) =>
        val dependenciesInit = connections.filter(_.trgPort.fmu == fmu && connections.exists(_.srcPort.fmu == fmu && _.srcPort.port == port)).map(_.srcPort.port)
        val dependencies = connections.filter(_.trgPort.fmu == fmu && connections.exists(_.srcPort.fmu == fmu && _.srcPort.port == port)).map(_.srcPort.port)
        port -> inputPortModel.copy(dependenciesInit = dependenciesInit, dependencies = dependencies)
      }
      val enrichedOutputs = fmuModel.outputs.map { case (port, outputPortModel) =>
        val dependenciesInit = connections.filter(_.srcPort.fmu == fmu && connections.exists(_.trgPort.fmu == fmu && _.trgPort.port == port)).map(_.trgPort.port)
        val dependencies = connections.filter(_.srcPort.fmu == fmu && connections.exists(_.trgPort.fmu == fmu && _.trgPort.port == port)).map(_.trgPort.port)
        port -> outputPortModel.copy(dependenciesInit = dependenciesInit, dependencies = dependencies)
      }
      fmu -> fmuModel.copy(inputs = enrichedInputs, outputs = enrichedOutputs)
    }
    this.copy(fmus = enrichedFmus)
  }



  override def toConf(indentationLevel: Int): String = {
    s"""
       |${indentBy(indentationLevel)}fmus = {
       |${indentBy(indentationLevel + 1)}${fmus.map { case (fmu, fmuModel) => s"$fmu = ${fmuModel.toConf(indentationLevel + 1)}" }.mkString("\n")}
       |${indentBy(indentationLevel)}}
       |${indentBy(indentationLevel)}connections = ${toArray(connections.map(_.toConf(indentationLevel + 1)))}
       |""".stripMargin
  }
}


trait SimulationInstruction extends UppaalModel with ConfElement {
  def fmu: String

  require(fmu.nonEmpty, "fmu must not be empty")

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}"
}

sealed abstract class InitializationInstruction extends SimulationInstruction

final case class InitSet(port: PortRef) extends InitializationInstruction {
  override def fmu: String = port.fmu

  override def toUppaal: String = s"{$fmu, set, ${fmuPortName(port)}, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{set: ${generatePort(port)}}"

}

final case class InitGet(port: PortRef) extends InitializationInstruction {
  override def fmu: String = port.fmu

  override def toUppaal: String = s"{$fmu, get, ${fmuPortName(port)}, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{get: ${generatePort(port)}}"
}

final case class EnterInitMode(fmu: String) extends InitializationInstruction {
  override def toUppaal: String = s"{$fmu, enterInitialization, noPort, noStep, noFMU, noCommitment, noLoop}"

}

final case class ExitInitMode(fmu: String) extends InitializationInstruction {
  override def toUppaal: String = s"{$fmu, exitInitialization, noPort, noStep, noFMU, noCommitment, noLoop}"
}

final case class AlgebraicLoopInit(untilConverged: List[PortRef],
                                   iterate: List[InitializationInstruction]) extends InitializationInstruction {
  override def fmu: String = "noFMU"

  override def toUppaal: String = throw new Exception("AlgebraicLoopInit should not be serialized to Uppaal")

  override def toConf(indentationLevel: Int): String =
    s"""${indentBy(indentationLevel)}{
       |${indentBy(indentationLevel + 1)}loop: {
       |${indentBy(indentationLevel + 2)}until-converged: ${toArray(untilConverged.map(generatePort))}
       |${indentBy(indentationLevel + 2)}iterate: ${toArray(iterate.map(_.toConf(indentationLevel + 3)), "\n")}
       |${indentBy(indentationLevel + 1)}}
       |${indentBy(indentationLevel)}}
       """.stripMargin
}

sealed abstract class InstantiationInstruction extends SimulationInstruction {
  def action: String

  override def toUppaal: String = s"{$fmu, $action, noPort, noStep, noFMU, noCommitment, noLoop}"
}

final case class Instantiate(fmu: String) extends InstantiationInstruction {
  override def action: String = "instantiate"
}

final case class SetupExperiment(fmu: String) extends InstantiationInstruction {
  override def action: String = "setupExperiment"
}

sealed abstract class StepSize extends ConfElement

final case class DefaultStepSize() extends StepSize {
  override def toConf(indentationLevel: Int): String = ""
}

final case class RelativeStepSize(fmu: String) extends StepSize {
  override def toConf(indentationLevel: Int): String = s", by-same-as: $fmu"
}

final case class AbsoluteStepSize(H: Int) extends StepSize {
  override def toConf(indentationLevel: Int): String = s", by: $H"
}

sealed abstract class CosimStepInstruction extends SimulationInstruction

final case class Set(port: PortRef) extends CosimStepInstruction {
  override def fmu: String = port.fmu

  override def toUppaal: String = s"{$fmu, set, ${fmuPortName(port)}, noStep, noFMU, final, noLoop}"


  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{set: ${generatePort(port)}}"
}

final case class Get(port: PortRef) extends CosimStepInstruction {
  override def fmu: String = port.fmu

  override def toUppaal: String = s"{$fmu, get, ${fmuPortName(port)}, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{get: ${generatePort(port)}}"
}

final case class GetTentative(port: PortRef) extends CosimStepInstruction {
  override def fmu: String = port.fmu

  override def toUppaal: String = s"{$fmu, get, ${fmuPortName(port)}, noStep, noFMU, tentative, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{get-tentative: ${generatePort(port)}}"
}

final case class SetTentative(port: PortRef) extends CosimStepInstruction {
  override def fmu: String = port.fmu

  override def toUppaal: String =
    s"{$fmu, set, ${fmuPortName(port)}, noStep, noFMU, tentative, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{set-tentative: ${generatePort(port)}}"
}

final case class Step(fmu: String, by: StepSize) extends CosimStepInstruction {
  override def toUppaal: String =
    by match {
      case DefaultStepSize() => s"{$fmu, step, noPort, H, noFMU, noCommitment, noLoop}"
      case RelativeStepSize(fmu_step) => s"{$fmu, step, noPort, noStep, $fmu_step, noCommitment, noLoop}"
      case AbsoluteStepSize(step_size) => s"{$fmu, step, noPort, $step_size, noFMU, noCommitment, noLoop}"
    }

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{step: $fmu ${by.toConf(0)}}"
}

final case class SaveState(fmu: String) extends CosimStepInstruction {
  override def toUppaal: String = s"{$fmu, save, noPort, noStep, noFMU, noCommitment, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{save-state: $fmu}"
}

case class RestoreState(fmu: String) extends CosimStepInstruction {
  override def toUppaal: String = s"{$fmu, restore, noPort, noStep, noFMU, noCommitment, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{restore-state: $fmu}"
}

case class AlgebraicLoop(untilConverged: List[PortRef],
                         iterate: List[CosimStepInstruction],
                         ifRetryNeeded: List[CosimStepInstruction]) extends CosimStepInstruction {
  override def fmu: String = "noFMU"

  override def toUppaal: String = throw new Exception("Algebraic loop not supported in Uppaal")

  override def toConf(indentationLevel: Int): String =
    s"""${indentBy(indentationLevel)}{
       |${indentBy(indentationLevel + 1)}loop: {
       |${indentBy(indentationLevel + 2)}until-converged: ${toArray(untilConverged.map(generatePort))}
       |${indentBy(indentationLevel + 2)}iterate: ${toArray(iterate.map(_.toConf(indentationLevel + 3)), "\n")}
       |${indentBy(indentationLevel + 2)}if-retry-needed: ${toArray(ifRetryNeeded.map(_.toConf(indentationLevel + 3)))}
       |${indentBy(indentationLevel + 1)}}
       |${indentBy(indentationLevel)}}
       """.stripMargin
}

case class StepLoop(untilStepAccept: List[String], // List of FMU ids
                    iterate: List[CosimStepInstruction],
                    ifRetryNeeded: List[CosimStepInstruction]) extends CosimStepInstruction {
  override def fmu: String = "noFMU"

  override def toUppaal: String = s"{noFMU, findStep, noPort, noStep, noFMU, noCommitment, noLoop}"

  override def toConf(indentationLevel: Int): String =
    s"""${indentBy(indentationLevel)}{
       |${indentBy(indentationLevel + 1)}loop: {
       |${indentBy(indentationLevel + 2)}until-step-accept: ${toArray(untilStepAccept)}
       |${indentBy(indentationLevel + 2)}iterate: ${toArray(iterate.map(_.toConf(indentationLevel + 3)), "\n")}
       |${indentBy(indentationLevel + 2)}if-retry-needed: ${toArray(ifRetryNeeded.map(_.toConf(indentationLevel + 3)))}
       |${indentBy(indentationLevel + 1)}}
       |${indentBy(indentationLevel)}}
       """.stripMargin
}

case object NoOP extends CosimStepInstruction {
  override def fmu: String = "noFMU"

  override def toUppaal: String = "{noFMU, noOp, noPort, noStep, noFMU, noCommitment, noLoop}"
}

sealed abstract class TerminationInstruction extends SimulationInstruction {
  def actionName: String

  require(actionName.nonEmpty, "Termination instruction name cannot be empty")

  override def toUppaal: String = s"{ $fmu,  $actionName, noPort, noStep, noFMU, noCommitment, noLoop}"
}

case class Terminate(fmu: String) extends TerminationInstruction {
  override def actionName: String = "terminate"
}

case class FreeInstance(fmu: String) extends TerminationInstruction {
  override def actionName: String = "freeInstance"
}

case class Unload(fmu: String) extends TerminationInstruction {
  override def actionName: String = "unload"
}

final case class MasterModel(
                              name: String,
                              scenario: ScenarioModel,
                              instantiation: List[InstantiationInstruction] = List.empty,
                              initialization: List[InitializationInstruction] = List.empty,
                              cosimStep: Map[String, List[CosimStepInstruction]] = Map.empty,
                              terminate: List[TerminationInstruction] = List.empty
                            ) extends ConfElement {
  require(name.nonEmpty, "Master model name cannot be empty")

  override def toConf(indentationLevel: Int = 0): String = {
    val init = toArray(initialization.map(_.toConf(indentationLevel + 1)), "\n")
    val step = cosimStep.map { case (stepName, stepInstructions) =>
      s""" $stepName =
         |${indentBy(indentationLevel + 1)}${toArray(stepInstructions.map(_.toConf(indentationLevel + 2)), "\n")}
          """
    }.mkString("{", ",", "}")
    s"""name = $name
       |scenario = {
       |  ${scenario.toConf(indentationLevel + 1)}
       |}
       |initialization = $init
       |cosim-step = $step
       |""".stripMargin
  }
}

case class MasterModelDTO(
                           name: String,
                           scenario: ScenarioModel,
                           initialization: List[InitializationInstruction],
                           cosimStep: Map[String, List[CosimStepInstruction]],
                         )


case class AdaptiveModel(
                          configurableInputs: List[PortRef],
                          configurations: Map[String, ConfigurationModel]
                        )

case class ConfigurationModel(
                               inputs: Map[PortRef, InputPortModel],
                               cosimStep: String,
                               connections: List[ConnectionModel]
                             )

object MasterModelDTO {
  implicit val masterModelEncoder: Encoder.AsObject[MasterModelDTO] = deriveEncoder[MasterModelDTO]
  implicit val masterModelDecoder: Decoder[MasterModelDTO] = deriveDecoder[MasterModelDTO]

  implicit val portRefKeyEncoder: KeyEncoder[PortRef] = (portRef: PortRef) => portRef.fmu + "." + portRef.port
  implicit val portRefKeyDecoder: KeyDecoder[PortRef] = (portRef: String) => {
    val p = portRef.split(".").map(_.replace(".", ""))
    Some(PortRef(p.head, p.last))
  }

  implicit val reactiveDecoder: Decoder[Reactivity.Value] = Decoder.decodeEnumeration(Reactivity)
  implicit val reactiveEncoder: Encoder[Reactivity.Value] = Encoder.encodeEnumeration(Reactivity)
}

