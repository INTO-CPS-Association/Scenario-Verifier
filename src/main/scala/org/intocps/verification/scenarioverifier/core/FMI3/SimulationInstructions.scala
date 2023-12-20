package org.intocps.verification.scenarioverifier.core.FMI3

import org.intocps.verification.scenarioverifier.core.EventInstruction
import org.intocps.verification.scenarioverifier.core.InitializationInstruction
import org.intocps.verification.scenarioverifier.core.PortAction
import org.intocps.verification.scenarioverifier.core.PortRef

final case class GetShift(port: PortRef) extends InitializationInstruction with PortAction {
  override def action: String = "get-shift"

  override def UPPAALaction: String = "getShift"

  override def toSMTLib: String = s"${fmu}_${port.port}_shift"
}

final case class GetInterval(port: PortRef) extends InitializationInstruction with PortAction {

  override def action: String = "get-interval"

  override def UPPAALaction: String = "getInterval"

  override def toSMTLib: String = s"${fmu}_${port.port}_shift"
}

final case class Set(port: PortRef) extends PortAction {
  override def action: String = "set"

  override def UPPAALaction: String = "set"
}

final case class Get(port: PortRef) extends PortAction {
  override def action: String = "get"

  override def UPPAALaction: String = "get"
}

final case class StepE(fmu: String) extends EventInstruction {
  override def toUppaal: String =
    s"{$fmu, stepE, noPort, noStep, noFMU, final, noLoop}"

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}{stepE: $fmu}"

  override def toSMTLib: String = s"${fmu}_stepE"
}

final case class GetClock(port: PortRef) extends PortAction {
  override def action: String = "get-clock"

  override def UPPAALaction: String = "getClock"
}

final case class SetClock(port: PortRef) extends PortAction {
  override def action: String = "set-clock"

  override def UPPAALaction: String = "setClock"
}

final case class NextClock(port: PortRef) extends PortAction {
  override def action: String = "next"

  override def UPPAALaction: String = "nextClock"

}
