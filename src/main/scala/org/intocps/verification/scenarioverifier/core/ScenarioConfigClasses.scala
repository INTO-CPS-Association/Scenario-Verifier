package org.intocps.verification.scenarioverifier.core

final case class InputPortConfig(reactivity: String) // Parsed into enums later.

final case class OutputPortConfig(dependenciesInit: List[String], dependencies: List[String]) // Input ports

final case class FmuConfig(
    inputs: Map[String, InputPortConfig] = Map.empty,
    outputs: Map[String, OutputPortConfig] = Map.empty,
    canRejectStep: Boolean = false,
    path: String = "")

final case class ScenarioConfig(
    fmus: Map[String, FmuConfig],
    configuration: Option[AdaptiveConfig],
    connections: List[String], // Each connection is notes as for usability "msd1.x1 -> msd2.x1"
    maxPossibleStepSize: Int = 1)

trait LoopConfig[A] {
  def untilCriteria: List[String]

  def iterate: List[A]

  def ifRetryNeeded: List[A]
}

case class LoopConfigStep(
    untilConverged: List[String] = Nil,
    untilStepAccept: List[String] = Nil,
    iterate: List[NestedStepStatement],
    ifRetryNeeded: List[NestedStepStatement])
    extends LoopConfig[NestedStepStatement] {
  override def untilCriteria: List[String] = untilConverged ++ untilStepAccept
}

case class LoopConfigInit(untilConverged: List[String], override val iterate: List[NestedInitStatement])
    extends LoopConfig[NestedInitStatement] {
  override val ifRetryNeeded: List[NestedInitStatement] = Nil

  override def untilCriteria: List[String] = untilConverged
}

object NoConfiguration extends AdaptiveConfig(Nil, Map.empty)

object NoLoop extends LoopConfigStep(Nil, Nil, Nil, Nil)

object NoLoopInit extends LoopConfigInit(Nil, Nil)

sealed trait InitializationStatement {
  def get: String

  def set: String
}

sealed trait StepStatement {
  def get: String

  def set: String

  def step: String

  def saveState: String

  def restoreState: String

  def by: Int // Optionally defines the absolute value for a step size. If this and bySameAs is not defines, uses the default step size.

  /*
  Optionally defines the step size to be used relative to the step size accepted by another FMU.
  If that step has never been taken before (like in the first cosim step), then this is ignored for that cosim step.
   */
  def bySameAs: String
}

case class NestedStepStatement(
    get: String = "",
    set: String = "",
    step: String = "",
    saveState: String = "",
    restoreState: String = "",
    loop: LoopConfigStep = NoLoop,
    getTentative: String = "",
    setTentative: String = "",
    by: Int = -1,
    bySameAs: String = "")
    extends StepStatement {
  require(
    get.nonEmpty || getTentative.nonEmpty || setTentative.nonEmpty || set.nonEmpty || step.nonEmpty ||
      saveState.nonEmpty || restoreState.nonEmpty || loop.iterate.nonEmpty,
    "At least one of get, set, step, saveState, or restoreState must be defined")

}

case class RootStepStatement(
    get: String = "",
    set: String = "",
    step: String = "",
    saveState: String = "",
    restoreState: String = "",
    loop: LoopConfigStep = NoLoop,
    by: Int = -1,
    bySameAs: String = "")
    extends StepStatement {
  require(
    get.nonEmpty || set.nonEmpty || step.nonEmpty || saveState.nonEmpty || restoreState.nonEmpty || loop.iterate.nonEmpty,
    "At least one of get, set, step, saveState, or restoreState must be defined")
}

case class NestedInitStatement(get: String = "", set: String = "") extends InitializationStatement {
  require(get.nonEmpty || set.nonEmpty, "At least one of get or set must be defined")
}

case class RootInitStatement(get: String = "", set: String = "", loop: LoopConfigInit = NoLoopInit) extends InitializationStatement {
  require(get.nonEmpty || set.nonEmpty | loop.iterate.nonEmpty, "At least one of get or set must be defined")
}

case class MasterConfig(
    name: String,
    scenario: ScenarioConfig,
    initialization: List[RootInitStatement],
    cosimStep: Map[String, List[RootStepStatement]])

case class AdaptiveConfig(configurableInputs: List[String] = Nil, configurations: Map[String, SettingConfig] = Map.empty)

case class SettingConfig(inputs: Map[String, InputPortConfig] = Map.empty, connections: List[String], cosimStep: String)
