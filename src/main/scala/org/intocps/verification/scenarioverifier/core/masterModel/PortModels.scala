package org.intocps.verification.scenarioverifier.core.masterModel
import scala.collection.immutable

import org.intocps.verification.scenarioverifier.core.PortRef
import ClockType.ClockType
import Reactivity.Reactivity

trait PortModel extends ConfElement

trait InputPortModel extends PortModel {
  def reactivity: Reactivity
}

trait OutputPortModel extends PortModel {
  def dependenciesInit: List[String]
  def dependencies: List[String]
}

case class FMI2InputPortModel(reactivity: Reactivity) extends InputPortModel {
  override def toConf(indentationLevel: Int = 0): String = s"{reactivity=${reactivity.toString}}"
}

final case class FMI2OutputPortModel(dependenciesInit: List[String], dependencies: List[String]) extends OutputPortModel with UppaalModel {
  override def toUppaal: String = s"{${dependencies.mkString(",")}}"

  override def toConf(indentationLevel: Int = 0): String =
    s"{dependencies-init=${toArray(dependenciesInit)}, dependencies=${toArray(dependencies)}}"
}

final case class FMI3InputPortModel(reactivity: Reactivity, clocks: List[String]) extends InputPortModel {
  override def toConf(indentationLevel: Int = 0): String = s"{reactivity=${reactivity.toString}, clocks=${toArray(clocks)}}"
}

final case class FMI3OutputPortModel(dependenciesInit: List[String], dependencies: List[String], clocks: List[String])
    extends UppaalModel
    with OutputPortModel {
  override def toUppaal: String = s"{${dependencies.mkString(",")}}"

  override def toConf(indentationLevel: Int = 0): String =
    s"{dependencies-init=${toArray(dependenciesInit)}, dependencies=${toArray(dependencies)}, clocks=${toArray(clocks)}}"
}

final case class OutputClockModel(typeOfClock: ClockType, dependencies: List[String], dependenciesClocks: List[String]) extends PortModel {
  override def toConf(indentationLevel: Int = 0): String =
    s"{type-of-clock=${typeOfClock.toString}, dependencies=${toArray(dependencies)}, dependencies-clocks=${toArray(dependenciesClocks)}}"
}

final case class InputClockModel(typeOfClock: ClockType, interval: Int) extends PortModel {
  require(
    typeOfClock == ClockType.triggered && interval == 0 || typeOfClock == ClockType.timed && interval > 0,
    "Interval must be greater than 0 for time-based clocks and 0 for triggered clocks")

  override def toConf(indentationLevel: Int = 0): String = s"{type-of-clock=${typeOfClock.toString}, interval=$interval}"
}

final case class EventEntrance(clocks: immutable.Set[PortRef]) extends ConfElement {
  require(clocks.nonEmpty, "Event entrance must contain at least one clock")

  override def toConf(indentationLevel: Int = 0): String = {
    toArray(clocks.map(generatePort).toList)
  }
}
