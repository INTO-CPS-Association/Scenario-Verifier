package org.intocps.verification.scenarioverifier.core.masterModel
import org.intocps.verification.scenarioverifier.core.PortRef

final case class ConnectionModel(srcPort: PortRef, trgPort: PortRef) extends UppaalModel with ConfElement with SMTLibElement {
  require(srcPort.fmu != trgPort.fmu, "srcPort and trgPort must not be in the same FMU")

  override def toUppaal: String =
    f"""{${sanitize(srcPort.fmu)}, ${sanitize(fmuPortName(srcPort))}, ${sanitize(trgPort.fmu)}, ${sanitize(fmuPortName(trgPort))}}"""

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}${generatePort(srcPort)} -> ${generatePort(trgPort)}"

  def toSMTLib: String =
    s"(assert (< ${srcPort.toSMTLib} ${trgPort.toSMTLib}))"
}
