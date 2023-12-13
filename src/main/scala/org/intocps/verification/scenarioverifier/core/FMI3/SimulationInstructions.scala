package org.intocps.verification.scenarioverifier.core.FMI3

import org.intocps.verification.scenarioverifier.core.InitializationInstruction
import org.intocps.verification.scenarioverifier.core.PortRef
import org.intocps.verification.scenarioverifier.core.SMTLibElement
import org.intocps.verification.scenarioverifier.core.SimulationInstruction

final case class GetShift(port: PortRef) extends InitializationInstruction {
  override def fmu: String = port.fmu

  override def toUppaal: String = s"{$fmu, getShift, ${fmuPortName(port)}, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{get-shift: ${generatePort(port)}}"

  override def toSMTLib: String = s"${fmu}_${port.port}_shift"
}

final case class GetInterval(port: PortRef) extends InitializationInstruction {
  override def fmu: String = port.fmu

  override def toUppaal: String = s"{$fmu, getInterval, ${fmuPortName(port)}, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{get-interval: ${generatePort(port)}}"

  override def toSMTLib: String = s"${fmu}_${port.port}_shift"
}

sealed abstract class EventInstruction extends SimulationInstruction with SMTLibElement

final case class Set(port: PortRef) extends EventInstruction {
  override def fmu: String = port.fmu

  override def toUppaal: String = s"{$fmu, set, ${fmuPortName(port)}, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{set: ${generatePort(port)}}"

  override def toSMTLib: String = s"${fmu}_${port.port}"
}

final case class Get(port: PortRef) extends EventInstruction {
  override def fmu: String = port.fmu

  override def toUppaal: String = s"{$fmu, get, ${fmuPortName(port)}, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{get: ${generatePort(port)}}"

  override def toSMTLib: String = s"${fmu}_${port.port}"
}

final case class StepE(fmu: String) extends EventInstruction {
  override def toUppaal: String =
    s"{$fmu, stepE, noPort, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{stepE: $fmu}"

  override def toSMTLib: String = s"${fmu}_stepE"
}

final case class GetClock(port: PortRef) extends EventInstruction {
  override def toUppaal: String =
    s"{$fmu, getClock, ${fmuPortName(port)}, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{getClock: ${generatePort(port)}}"

  override def fmu: String = port.fmu

  override def toSMTLib: String = s"${fmu}_${port.port}"
}

final case class SetClock(port: PortRef) extends EventInstruction {
  override def toUppaal: String =
    s"{$fmu, setClock, ${fmuPortName(port)}, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{getClock: ${generatePort(port)}}"

  override def fmu: String = port.fmu

  override def toSMTLib: String = s"${fmu}_${port.port}"
}

final case class NextClock(port: PortRef) extends EventInstruction with SMTLibElement {
  override def toUppaal: String =
    s"{$fmu, nextClock, ${fmuPortName(port)}, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{next: ${generatePort(port)}}"

  override def fmu: String = port.fmu

  override def toSMTLib: String = s"${fmu}_${port.port}"
}
