package core

case class InputPortConfig(reactivity: String) // Parsed into enums later.
case class OutputPortConfig(dependenciesInit: List[String], dependencies: List[String]) // Input ports
case class FmuConfig(
                      inputs: Map[String, InputPortConfig] = Map.empty,
                      outputs: Map[String, OutputPortConfig] = Map.empty,
                      canRejectStep: Boolean = false
                    )

case class ScenarioConfig(
                           fmus: Map[String, FmuConfig],
                           configuration : Option[AdaptiveConfig],
                           connections: List[String], // Each connection is notes as for usability "msd1.x1 -> msd2.x1"
                           maxPossibleStepSize: Int = 1
                         )

case class LoopConfig(untilConverged: List[String] = Nil,
                      untilStepAccept: List[String] = Nil,
                      iterate: List[NestedStepStatement],
                      ifRetryNeeded: List[NestedStepStatement],
                      )

case class LoopInitConfig(untilConverged: List[String],
                      iterate: List[NestedInitStatement],
                     )

object NoConfiguration extends AdaptiveConfig(Nil, Map.empty)
object NoLoop extends LoopConfig(Nil, Nil, Nil, Nil)
object NoLoopInit extends LoopInitConfig(Nil, Nil)

sealed trait InitializationStatement{
  def get: String
  def set: String
}

sealed trait StepStatement{
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

case class NestedStepStatement(get: String = "",
                               set: String = "",
                               step: String = "",
                               saveState: String = "",
                               restoreState: String = "",
                               loop: LoopConfig = NoLoop,
                               getTentative: String = "",
                               setTentative: String = "",
                               by: Int = -1,
                               bySameAs: String = "") extends StepStatement

case class RootStepStatement(get: String = "",
                             set: String = "",
                             step: String = "",
                             saveState: String = "",
                             restoreState: String = "",
                             loop: LoopConfig = NoLoop,
                             by: Int = -1,
                             bySameAs: String = "") extends StepStatement

case class NestedInitStatement(get: String = "",
                               set: String = "") extends InitializationStatement

case class RootInitStatement(get: String = "",
                             set: String = "",
                             loop: LoopInitConfig = NoLoopInit) extends InitializationStatement

case class MasterConfig(
                         name: String,
                         scenario: ScenarioConfig,
                         initialization: List[RootInitStatement],
                         cosimStep: Map[String, List[RootStepStatement]]
                       )

case class AdaptiveConfig(
                           configurableInputs : List[String] = Nil,
                           configurations : Map[String, SettingConfig]  = Map.empty
                         )

case class SettingConfig(
                          inputs: Map[String, InputPortConfig] = Map.empty,
                          cosimStep : String
                        )