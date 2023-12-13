package org.intocps.verification.scenarioverifier.core.FMI3

import org.intocps.verification.scenarioverifier.core.LoopConfigInit
import org.intocps.verification.scenarioverifier.core.NoLoopInit
import org.intocps.verification.scenarioverifier.core.RootStepStatement

final case class InputClockConfig(typeOfClock: String, interval: Int)

final case class OutputClockConfig(typeOfClock: String, dependencies: List[String], dependenciesClocks: List[String])

final case class InputPortConfig(reactivity: String, clocks: List[String]) // Parsed into enums later.

final case class OutputPortConfig(dependenciesInit: List[String], dependencies: List[String], clocks: List[String]) // Input ports

final case class FmuConfig(
    inputs: Map[String, InputPortConfig] = Map.empty,
    outputs: Map[String, OutputPortConfig] = Map.empty,
    inputClocks: Map[String, InputClockConfig] = Map.empty,
    outputClocks: Map[String, OutputClockConfig] = Map.empty,
    canRejectStep: Boolean = false,
    path: String = "")

final case class ScenarioConfig(
    fmus: Map[String, FmuConfig],
    connections: List[String], // Each connection is notes as for usability "msd1.x1 -> msd2.x1"
    clockConnections: List[String], // Each connection is notes as for usability "msd1.x1 -> msd2.x1"
    maxPossibleStepSize: Int = 1)

sealed trait InitializationStatement {
  def get: String

  def set: String

  def getInterval: String

  def getShift: String
}

sealed trait EventStatement {
  def get: String

  def set: String

  def setClock: String

  def getClock: String

  def step: String

  def next: String
}

case class RootEventStatement(
    get: String = "",
    set: String = "",
    setClock: String = "",
    getClock: String = "",
    step: String = "",
    next: String = "")
    extends EventStatement {
  require(get.nonEmpty || set.nonEmpty | step.nonEmpty | next.nonEmpty, "At least one of get or set must be defined")
}

case class FMI3RootInitStatement(
    get: String = "",
    set: String = "",
    getInterval: String = "",
    getShift: String = "",
    loop: LoopConfigInit = NoLoopInit)
    extends InitializationStatement {
  require(get.nonEmpty || set.nonEmpty | loop.iterate.nonEmpty | loop.ifRetryNeeded.nonEmpty, "At least one of get or set must be defined")
}

final case class FMI3MasterConfig(
    name: String,
    scenario: ScenarioConfig,
    initialization: List[FMI3RootInitStatement],
    cosimStep: List[RootStepStatement],
    eventStrategies: Map[String, EventStrategyStatement])

case class EventStrategyStatement(clocks: List[String] = Nil, iterate: List[RootEventStatement])
