package org.intocps.verification.scenarioverifier.core.masterModel
import org.intocps.verification.scenarioverifier.core.PortRef

object AlgorithmType extends Enumeration {
  type AlgorithmType = Value
  val init, step, event = Value
}

object Reactivity extends Enumeration {
  type Reactivity = Value
  val reactive, delayed, noPort = Value
}

object ClockType extends Enumeration {
  type ClockType = Value
  val triggered, timed = Value
}

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

trait SMTLibElement {
  def toSMTLib: String
}

trait UppaalModel {
  protected def sanitize(s: String): String = s
    .replaceAll("\\W", "")

  def fmuPortName(portRef: PortRef) = s"${sanitize(portRef.fmu)}_${sanitize(portRef.port)}"

  def toUppaal: String
}