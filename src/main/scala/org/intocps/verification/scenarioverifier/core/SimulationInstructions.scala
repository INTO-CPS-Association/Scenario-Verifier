package org.intocps.verification.scenarioverifier.core
import guru.nidi.graphviz.model.Port
import org.intocps.verification.scenarioverifier.core.masterModel.ConfElement
import org.intocps.verification.scenarioverifier.core.masterModel.SMTLibElement
import org.intocps.verification.scenarioverifier.core.masterModel.UppaalModel

trait SimulationInstruction extends UppaalModel with ConfElement {
  def fmu: String

  def portName: String = "noPort"

  require(fmu.nonEmpty, "fmu must not be empty")

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}"
}

trait InitializationInstruction extends SimulationInstruction with SMTLibElement

trait CosimStepInstruction extends SimulationInstruction with SMTLibElement

trait EventInstruction extends SimulationInstruction with SMTLibElement

trait PortAction extends InitializationInstruction with CosimStepInstruction with EventInstruction {
  def port: PortRef

  def action: String

  def UPPAALaction: String

  def tentativity: String = "final"
  override def fmu: String = port.fmu

  override def portName: String = port.port

  override def toSMTLib: String = s"${sanitizeString(fmu)}_${sanitizeString(port.port)}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{$action: ${generatePort(port)}}"

  override def toUppaal: String = s"{$fmu, $UPPAALaction, ${fmuPortName(port)}, noStep, noFMU, $tentativity, noLoop}"
}

final case class InitSet(port: PortRef) extends InitializationInstruction with PortAction {
  override def action: String = "set"

  override def UPPAALaction: String = "set"
}

final case class InitGet(port: PortRef) extends InitializationInstruction with PortAction {
  override def action: String = "get"

  override def UPPAALaction: String = "get"
}

final case class EnterInitMode(fmu: String) extends InitializationInstruction {
  override def toUppaal: String = s"{$fmu, enterInitialization, noPort, noStep, noFMU, noCommitment, noLoop}"

  override def toSMTLib: String = throw new Exception("EnterInitMode should not be serialized to SMTLib")
}

final case class ExitInitMode(fmu: String) extends InitializationInstruction {
  override def toUppaal: String = s"{$fmu, exitInitialization, noPort, noStep, noFMU, noCommitment, noLoop}"

  override def toSMTLib: String = throw new Exception("ExitInitMode should not be serialized to SMTLib")
}

final case class AlgebraicLoopInit(untilConverged: List[PortRef], iterate: List[InitializationInstruction])
    extends InitializationInstruction {
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

  override def toSMTLib: String = throw new Exception("AlgebraicLoopInit should not be serialized to SMTLib")
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
  require(H > 0, "H must be greater than 0")

  override def toConf(indentationLevel: Int): String = s", by: $H"
}

final case class Set(port: PortRef) extends CosimStepInstruction with PortAction {
  override def action: String = "set"
  override def UPPAALaction: String = "set"
}

final case class Get(port: PortRef) extends CosimStepInstruction with PortAction {
  override def action: String = "get"
  override def UPPAALaction: String = "get"
}

final case class GetTentative(port: PortRef) extends CosimStepInstruction with PortAction {
  override def action: String = "get-tentative"

  override def UPPAALaction: String = "get"

  override def tentativity: String = "tentative"
}

final case class SetTentative(port: PortRef) extends CosimStepInstruction with PortAction {
  override def action: String = "set-tentative"

  override def UPPAALaction: String = "set"
  override def tentativity: String = "tentative"
}

final case class Step(fmu: String, by: StepSize) extends CosimStepInstruction {
  override def toUppaal: String =
    by match {
      case DefaultStepSize() => s"{$fmu, step, noPort, H, noFMU, noCommitment, noLoop}"
      case RelativeStepSize(fmu_step) => s"{$fmu, step, noPort, noStep, $fmu_step, noCommitment, noLoop}"
      case AbsoluteStepSize(step_size) => s"{$fmu, step, noPort, $step_size, noFMU, noCommitment, noLoop}"
    }

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{step: $fmu ${by.toConf(0)}}"

  override def toSMTLib: String = s"${sanitizeString(fmu)}_step"
}

final case class SaveState(fmu: String) extends CosimStepInstruction {
  override def toUppaal: String = s"{$fmu, save, noPort, noStep, noFMU, noCommitment, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{save-state: $fmu}"

  override def toSMTLib: String = throw new Exception("SaveState should not be serialized to SMTLib")
}

case class RestoreState(fmu: String) extends CosimStepInstruction {
  override def toUppaal: String = s"{$fmu, restore, noPort, noStep, noFMU, noCommitment, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{restore-state: $fmu}"

  override def toSMTLib: String = throw new Exception("RestoreState should not be serialized to SMTLib")
}

case class AlgebraicLoop(untilConverged: List[PortRef], iterate: List[CosimStepInstruction], ifRetryNeeded: List[CosimStepInstruction])
    extends CosimStepInstruction {
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

  override def toSMTLib: String = throw new Exception("Algebraic loop not supported in SMTLib")
}

case class StepLoop(
    untilStepAccept: List[String], // List of FMU ids
    iterate: List[CosimStepInstruction],
    ifRetryNeeded: List[CosimStepInstruction])
    extends CosimStepInstruction {
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

  override def toSMTLib: String = throw new Exception("Step loop not supported in SMTLib")
}

case object NoOP extends CosimStepInstruction {
  override def fmu: String = "noFMU"

  override def toUppaal: String = "{noFMU, noOp, noPort, noStep, noFMU, noCommitment, noLoop}"

  override def toSMTLib: String = throw new Exception("NoOP should not be serialized to SMTLib")
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
