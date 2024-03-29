package org.intocps.verification.scenarioverifier.core
import scala.util.parsing.combinator.RegexParsers

import org.intocps.verification.scenarioverifier.core.masterModel.ConnectionModel
import org.intocps.verification.scenarioverifier.core.masterModel.SMTLibElement

sealed abstract class AnyArgument

case class IdRef(str: String) extends AnyArgument

case class PortIdRef(str: String) extends AnyArgument

case class PortRef(fmu: String, port: String) extends AnyArgument with SMTLibElement {
  require(fmu.nonEmpty, "FMU name cannot be empty")
  require(port.nonEmpty, "Port name cannot be empty")

  override def toSMTLib: String = s"$fmu-$port"
}

case class DOT()

case class ARROW()

trait IdentifierParser extends RegexParsers {
  def identifier: Parser[IdRef] = "[a-zA-Z_][a-zA-Z0-9_]*".r ^^ { str => IdRef(str) }

  def portRef: Parser[PortIdRef] = "[a-zA-Z_][a-zA-Z0-9_\\.\\[\\]]*".r ^^ { str => PortIdRef(str) }
}

class FMURefParser extends IdentifierParser {
  private def dot: Parser[DOT] = "\\.".r ^^ { _ => DOT() }

  def fmu_port_ref: Parser[PortRef] = identifier ~ dot ~ portRef ^^ { case IdRef(fmu) ~ DOT() ~ PortIdRef(port) => PortRef(fmu, port) }
}

class ConnectionParser extends FMURefParser {
  private def arrow: Parser[ARROW] = "->".r ^^ { _ => ARROW() }

  def connection: Parser[ConnectionModel] = fmu_port_ref ~ arrow ~ fmu_port_ref ^^ {
    case PortRef(src_fmu, src_port) ~ ARROW() ~ PortRef(trg_fmu, trg_port) =>
      ConnectionModel(PortRef(src_fmu, src_port), PortRef(trg_fmu, trg_port))
  }
}

object FMURefParserSingleton extends FMURefParser

object ConnectionParserSingleton extends ConnectionParser
