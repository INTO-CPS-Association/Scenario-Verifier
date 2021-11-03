package core

object Reactivity extends Enumeration {
  type Reactivity = Value
  val reactive, delayed, noPort = Value
}

import core.Reactivity.Reactivity
import io.circe._, io.circe.generic.semiauto._
import io.circe.generic.auto._

case class InputPortModel(reactivity: Reactivity)

case class OutputPortModel(dependenciesInit: List[String], dependencies: List[String])

case class FmuModel(
                     inputs: Map[String, InputPortModel],
                     outputs: Map[String, OutputPortModel],
                     canRejectStep: Boolean,
                     path: String
                   )

case class ConnectionModel(
                            srcPort: PortRef,
                            trgPort: PortRef,
                          )

case class ScenarioModel(
                          fmus: Map[String, FmuModel],
                          config: AdaptiveModel,
                          connections: List[ConnectionModel],
                          maxPossibleStepSize: Int
                        )

sealed abstract class InitializationInstruction

case class InitSet(port: PortRef) extends InitializationInstruction

case class InitGet(port: PortRef) extends InitializationInstruction

case class EnterInitMode(fmu: String) extends InitializationInstruction

case class ExitInitMode(fmu: String) extends InitializationInstruction

case class AlgebraicLoopInit(untilConverged: List[PortRef],
                             iterate: List[InitializationInstruction]) extends InitializationInstruction

sealed abstract class InstantiationInstruction

case class Instantiate(fmu: String) extends InstantiationInstruction

case class SetupExperiment(fmu: String) extends InstantiationInstruction

sealed abstract class StepSize

case class DefaultStepSize() extends StepSize

case class RelativeStepSize(fmu: String) extends StepSize

case class AbsoluteStepSize(H: Int) extends StepSize

sealed abstract class CosimStepInstruction

case class Set(port: PortRef) extends CosimStepInstruction

case class Get(port: PortRef) extends CosimStepInstruction

case class GetTentative(port: PortRef) extends CosimStepInstruction

case class SetTentative(port: PortRef) extends CosimStepInstruction

case class Step(fmu: String, by: StepSize) extends CosimStepInstruction

case class SaveState(fmu: String) extends CosimStepInstruction

case class RestoreState(fmu: String) extends CosimStepInstruction

case class AlgebraicLoop(untilConverged: List[PortRef],
                         iterate: List[CosimStepInstruction],
                         ifRetryNeeded: List[CosimStepInstruction]) extends CosimStepInstruction

case class StepLoop(untilStepAccept: List[String], // List of FMU ids
                    iterate: List[CosimStepInstruction],
                    ifRetryNeeded: List[CosimStepInstruction]) extends CosimStepInstruction

case object NoOP extends CosimStepInstruction

sealed abstract class TerminationInstruction

case class Terminate(fmu: String) extends TerminationInstruction

case class FreeInstance(fmu: String) extends TerminationInstruction

case class Unload(fmu: String) extends TerminationInstruction

case class MasterModel(
                        name: String,
                        scenario: ScenarioModel,
                        instantiation: List[InstantiationInstruction],
                        initialization: List[InitializationInstruction],
                        cosimStep: Map[String, List[CosimStepInstruction]],
                        terminate: List[TerminationInstruction]
                      )

case class MasterModelDTO(
                        name: String,
                        scenario: ScenarioModel,
                        initialization: List[InitializationInstruction],
                        cosimStep: Map[String, List[CosimStepInstruction]],
                      )


case class AdaptiveModel(
                          configurableInputs: List[PortRef],
                          configurations: Map[String, ConfigurationModel]
                        )

case class ConfigurationModel(
                               inputs: Map[PortRef, InputPortModel],
                               cosimStep: String,
                               connections: List[ConnectionModel]
                             )


object MasterModelDTO {
  implicit val masterModelEncoder : Encoder.AsObject[MasterModelDTO] = deriveEncoder[MasterModelDTO]
  implicit val masterModelDecoder : Decoder[MasterModelDTO] = deriveDecoder[MasterModelDTO]

  implicit val portRefKeyEncoder: KeyEncoder[PortRef] = (portRef: PortRef) => portRef.fmu + "." + portRef.port
  implicit val portRefKeyDecoder: KeyDecoder[PortRef] = (portRef: String) => {
    val p = portRef.split(".").map(_.replace(".", ""))
    Some(PortRef(p.head, p.last))
  }

  implicit val reactiveDecoder: Decoder[Reactivity.Value] = Decoder.decodeEnumeration(Reactivity)
  implicit val reactiveEncoder: Encoder[Reactivity.Value] = Encoder.encodeEnumeration(Reactivity)



}

