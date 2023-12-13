package org.intocps.verification.scenarioverifier.core.FMI3

import scala.collection.immutable

import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import org.intocps.verification.scenarioverifier
import org.intocps.verification.scenarioverifier.core._
import org.intocps.verification.scenarioverifier.core.AdaptiveModel
import org.intocps.verification.scenarioverifier.core.AlgorithmType
import org.intocps.verification.scenarioverifier.core.AlgorithmType.AlgorithmType
import org.intocps.verification.scenarioverifier.core.ConfElement
import org.intocps.verification.scenarioverifier.core.ConnectionModel
import org.intocps.verification.scenarioverifier.core.CosimStepInstruction
import org.intocps.verification.scenarioverifier.core.FmuModel
import org.intocps.verification.scenarioverifier.core.Get
import org.intocps.verification.scenarioverifier.core.InitGet
import org.intocps.verification.scenarioverifier.core.InitSet
import org.intocps.verification.scenarioverifier.core.InitializationInstruction
import org.intocps.verification.scenarioverifier.core.InstantiationInstruction
import org.intocps.verification.scenarioverifier.core.PortRef
import org.intocps.verification.scenarioverifier.core.Reactivity
import org.intocps.verification.scenarioverifier.core.Reactivity.Reactivity
import org.intocps.verification.scenarioverifier.core.SMTLibElement
import org.intocps.verification.scenarioverifier.core.ScenarioModel
import org.intocps.verification.scenarioverifier.core.Step
import org.intocps.verification.scenarioverifier.core.TerminationInstruction
import org.intocps.verification.scenarioverifier.core.UppaalModel
import ClockType.timed
import ClockType.ClockType

object ClockType extends Enumeration {
  type ClockType = Value
  val triggered, timed = Value
}

final case class Fmu3Model(
    inputs: Map[String, InputPortModel],
    outputs: Map[String, OutputPortModel],
    inputClocks: Map[String, InputClockModel],
    outputClocks: Map[String, OutputClockModel],
    canRejectStep: Boolean,
    path: String = "")
    extends ConfElement {

  lazy val timeBasedClocks: Map[String, InputClockModel] = inputClocks.filter(_._2.typeOfClock == ClockType.timed)

  private def clockedInputs: Map[String, InputPortModel] = inputs.filter(input => input._2.clocks.nonEmpty)

  private def clockedOutputs: Map[String, OutputPortModel] = outputs.filter(output => output._2.clocks.nonEmpty)

  def reactiveInputs: Map[String, InputPortModel] =
    inputs.filter(input => input._2.reactivity == Reactivity.reactive && input._2.clocks.isEmpty)

  def delayedInputs: Map[String, InputPortModel] =
    inputs.filter(input => input._2.reactivity == Reactivity.delayed && input._2.clocks.isEmpty)

  require(
    inputs.keySet.intersect(outputs.keySet).isEmpty,
    s"FMU inputs (${inputs.keySet.mkString(", ")}) and outputs (${outputs.keySet.mkString(", ")}) must be disjoint.")

  override def toConf(indentationLevel: Int): String = {
    s"""
       |${indentBy(indentationLevel)}{
       |${indentBy(indentationLevel + 1)}can-reject-step = $canRejectStep,
       |${indentBy(indentationLevel + 1)}inputs = {
       |${indentBy(indentationLevel + 2)}${inputs
        .map { case (port, inputPortModel) => s"${sanitizeString(port)} = ${inputPortModel.toConf()}" }
        .mkString("\n")}
       |${indentBy(indentationLevel + 1)}},
       |${indentBy(indentationLevel + 1)}outputs = {
       |${indentBy(indentationLevel + 2)}${outputs
        .map { case (port, outputPortModel) => s"${sanitizeString(port)} = ${outputPortModel.toConf()}" }
        .mkString("\n")}
       |${indentBy(indentationLevel + 1)}}
       |${indentBy(indentationLevel + 1)}input-clocks = {
       |${indentBy(indentationLevel + 2)}${inputClocks
        .map { case (port, inputClockModel) => s"${sanitizeString(port)} = ${inputClockModel.toConf()}" }
        .mkString("\n")}
       |${indentBy(indentationLevel + 1)}},
       |${indentBy(indentationLevel + 1)}output-clocks = {
       |${indentBy(indentationLevel + 2)}${outputClocks
        .map { case (port, outputClockModel) => s"${sanitizeString(port)} = ${outputClockModel.toConf()}" }
        .mkString("\n")}
       |${indentBy(indentationLevel + 1)}},
       |${indentBy(indentationLevel)}}""".stripMargin
  }

  /*
  def coSimStepSMTLIB(fmuName: String): String = {
    s"""
       |; FMU $fmuName constraints
       |; Delayed inputs are after Step
       |${delayedInputs.map(input => s"(assert (> ${sanitizeString(fmuName)}_step ${sanitizeString(fmuName)}_${sanitizeString(input._1)}))").mkString("\n")}
       |; Reactive inputs are before Step
       |${reactiveInputs.map(input => s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(input._1)} ${sanitizeString(fmuName)}_step))").mkString("\n")}
       |; Step is before outputs
       |${outputs.filter(output => output._2.clocks.isEmpty).map(output => s"(assert (> ${sanitizeString(fmuName)}-step ${sanitizeString(fmuName)}_${sanitizeString(output._1)}))").mkString("\n")}
       |; Feed-through means that the input is before the output
       |${outputs.filter(output => output._2.dependencies.nonEmpty).map(output => output._2.dependencies.map(dependency => s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(dependency)} ${sanitizeString(fmuName)}_${sanitizeString(output._1)}))").mkString("\n")).mkString("\n")}
       |""".stripMargin
  }

  private def initSMTLIB(fmuName: String): String = {
    s"""
       |; FMU $fmuName constraints
       |; Feedthrough means that the input is before the output
       |${outputs.filter(output => output._2.dependenciesInit.nonEmpty).map(output => output._2.dependencies.map(dependency => s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(dependency)} ${sanitizeString(fmuName)}_${sanitizeString(output._1)}))").mkString("\n")).mkString("\n")}
       |""".stripMargin
  }

  def eventSMTLib(fmuName: String, clocks: List[String]): String = {
    val clockNamesActions = clocks.map(clock => s"${fmuName}_$clock")
    val clockedVariables = clocks.foreach(clock => {
      val inputsOfClock = clockedInputs.filter(input => input._2.clocks.contains(clock))
      val outputsOfClock = clockedOutputs.filter(output => output._2.clocks.contains(clock))
      val inputClockActionNames = inputsOfClock.map(input => s"${fmuName}_${input._1}_set")
      val outputClockActionNames = outputsOfClock.map(output => s"${fmuName}_${output._1}_get")
      val clockedVariableActions = (inputClockActionNames ++ outputClockActionNames).map(port => s"(declare-const $port Int)")
      val clockedVariableConstraints = (inputClockActionNames ++ outputClockActionNames).map(port => s"(assert (>= $port 0))")
      // Clocked variables are before the step
      val clockedVariableStep = (inputClockActionNames ++ outputClockActionNames).map(port => s"(assert (> ${fmuName}_step $port))")
      // All actions of a clock happen after a clock event
      val clockedVariableClock = (inputClockActionNames ++ outputClockActionNames).map(port => s"(assert (< $clock $port))")
    })
    val clockActions = clockNamesActions.map(clock => s"(declare-const $clock Int)")
    val clockConstraints = clockNamesActions.map(clock => s"(assert (>= $clock 0))")
    val clockStep = clockNamesActions.map(clock => s"(assert (> ${fmuName}_step $clock))")


    s"""
       |; FMU $fmuName constraints
       |; Declare clock variables
       |${clockActions.mkString("\n")}
       |${clockConstraints.mkString("\n")}
       |; Clocked Variables
       |""".stripMargin
  }
   */
}

final case class FMI3ScenarioModel(
    fmus: Map[String, Fmu3Model],
    connections: List[ConnectionModel],
    clockConnections: List[ConnectionModel],
    maxPossibleStepSize: Int)
    extends ConfElement {

  require(fmus.nonEmpty, "fmus must not be empty")
  require(maxPossibleStepSize > 0, "maxPossibleStepSize must be greater than 0")

  private val connectedInputPorts = fmus.map(fmu =>
    (
      fmu._1,
      fmu._2.inputs.filter(input =>
        input._2.clocks.isEmpty &&
          connections.exists(connection =>
            connection.trgPort == PortRef(fmu._1, input._1) || connection.srcPort == PortRef(fmu._1, input._1)))))
  private val connectedOutputPorts = fmus.map(fmu =>
    (
      fmu._1,
      fmu._2.outputs.filter(output =>
        output._2.clocks.isEmpty &&
          connections.exists(connection =>
            connection.trgPort == PortRef(fmu._1, output._1) || connection.srcPort == PortRef(fmu._1, output._1)))))

  private lazy val inputPorts: List[PortRef] = connectedInputPorts.flatMap(fmu => fmu._2.map(port => PortRef(fmu._1, port._1))).toList
  private lazy val outputPorts: List[PortRef] = connectedOutputPorts.flatMap(fmu => fmu._2.map(port => PortRef(fmu._1, port._1))).toList
  private lazy val inputActions: List[String] = inputPorts.map(_.toSMTLib)
  private lazy val outputActions: List[String] = outputPorts.map(_.toSMTLib)

  private lazy val connectionsBetweenClockedVariables: List[ConnectionModel] =
    clockConnections.filter(connection => inputPorts.contains(connection.trgPort) && outputPorts.contains(connection.srcPort))

  private lazy val connectionsBetweenNonClockedVariables: List[ConnectionModel] =
    connections.filter(connection => inputPorts.contains(connection.trgPort) && outputPorts.contains(connection.srcPort))

  private def clockedOutputs(clock: PortRef): List[PortRef] = {
    require(fmus.contains(clock.fmu), s"FMU ${clock.fmu} does not exist in scenario model")
    fmus(clock.fmu).outputs.filter(output => output._2.clocks.contains(clock.port)).map(output => PortRef(clock.fmu, output._1)).toList
  }

  private def clockedInputs(clock: PortRef): List[PortRef] = {
    require(fmus.contains(clock.fmu), s"FMU ${clock.fmu} does not exist in scenario model")
    val fmu = fmus(clock.fmu)
    val inputs = fmu.inputs.filter(input => input._2.clocks.contains(clock.port))
    inputs.map(input => PortRef(clock.fmu, input._1)).toList
  }

  // **
  // * Returns all event entrances of the scenario model
  // *
  lazy val eventEntrances: List[EventEntrance] = {
    // All possible subsets of input clocks
    val partitions = fmus
      .flatMap(fmu => fmu._2.inputClocks.map(c => PortRef(fmu._1, c._1)) ++ fmu._2.outputClocks.map(c => PortRef(fmu._1, c._1)))
      .toSet
      .subsets()
      .filter(_.nonEmpty)
      .toList
    // A partition is only valid if:
    val validPartitions = partitions.filter(partition => {
      // All src ports of the partition are connected to an output port in the partition
      val sourceClocks = partition.filter(port => clockConnections.exists(connection => connection.srcPort == port))
      val targetClocks = partition.filter(port => clockConnections.exists(connection => connection.trgPort == port))
      val sourceClocksConnected = sourceClocks.forall(clock => {
        val relevantConnections = clockConnections.filter(connection => connection.srcPort == clock)
        val connectedClocks = relevantConnections.map(connection => connection.trgPort)
        connectedClocks.forall(targetClock => targetClocks.contains(targetClock))
      })
      // All trg ports of the partition are connected to an input port in the partition
      val targetClocksConnected = targetClocks.forall(clock => {
        val relevantConnections = clockConnections.filter(connection => connection.trgPort == clock)
        val connectedClocks = relevantConnections.map(connection => connection.srcPort)
        connectedClocks.forall(sourceClock => sourceClocks.contains(sourceClock))
      })
      // All timebased clocks with the same interval are contained in the partition
      val timeBasedClocksPerInterval =
        fmus.flatMap(fmu => fmu._2.inputClocks.filter(clock => clock._2.typeOfClock == timed).map(clock => PortRef(fmu._1, clock._1) -> 1))
      val timeBasedClocks = timeBasedClocksPerInterval.keySet
      val timeBasedClocksInPartition = partition.intersect(timeBasedClocks)
      val timeBasedClocksNotInPartition = timeBasedClocks.diff(timeBasedClocksInPartition)
      val timeBasedRestriction =
        if (timeBasedClocksInPartition.isEmpty) true
        else {
          // Clocks outside the partition must have a different interval than clocks inside the partition
          timeBasedClocksInPartition.forall(clock => {
            val interval = timeBasedClocksPerInterval(clock)
            timeBasedClocksNotInPartition.forall(clockNotInPartition => {
              val intervalNotInPartition = timeBasedClocksPerInterval(clockNotInPartition)
              intervalNotInPartition != interval
            })
          })
        }
      sourceClocksConnected && targetClocksConnected & timeBasedRestriction
    })
    validPartitions.map(EventEntrance)
  }

  def enrich(): FMI3ScenarioModel = {
    val enrichedFmus = fmus.map { fmu =>
      connections
        .filter(c => c.srcPort.fmu.equalsIgnoreCase(fmu._1) || c.trgPort.fmu.equalsIgnoreCase(fmu._1))
        .foldLeft(fmu)((fmuModel, c) => {
          val model =
            if (c.trgPort.fmu.equalsIgnoreCase(fmu._1) && !fmuModel._2.inputs.contains(c.trgPort.port))
              fmuModel._2.copy(inputs = fmuModel._2.inputs + (c.trgPort.port -> InputPortModel(Reactivity.delayed, List.empty)))
            else if (c.srcPort.fmu.equalsIgnoreCase(fmu._1) && !fmuModel._2.outputs.contains(c.srcPort.port))
              fmuModel._2.copy(outputs = fmuModel._2.outputs + (c.srcPort.port -> OutputPortModel(List.empty, List.empty, List.empty)))
            else fmuModel._2
          (fmuModel._1, model)
        })
    }
    this.copy(fmus = enrichedFmus)
  }

  lazy val scenarioModel: ScenarioModel =
    ScenarioModel(
      fmus.map(fmu =>
        (
          fmu._1,
          FmuModel(
            fmu._2.inputs.map(input => (input._1, scenarioverifier.core.InputPortModel(input._2.reactivity))),
            fmu._2.outputs.map(output =>
              (output._1, scenarioverifier.core.OutputPortModel(output._2.dependenciesInit, output._2.dependencies))),
            canRejectStep = fmu._2.canRejectStep,
            path = fmu._2.path))),
      AdaptiveModel(List.empty, Map.empty),
      connections,
      maxPossibleStepSize)

  override def toConf(indentationLevel: Int): String = {
    s"""
       |${indentBy(indentationLevel)}fmus = {
       |${indentBy(indentationLevel + 1)}${fmus
        .map { case (fmu, fmuModel) => s"$fmu = ${fmuModel.toConf(indentationLevel + 1)}" }
        .mkString("\n")}
       |${indentBy(indentationLevel)}}
       |${indentBy(indentationLevel)}connections = ${toArray(connections.map(_.toConf(indentationLevel + 1)))}
       |${indentBy(indentationLevel)}clock-connections = ${toArray(clockConnections.map(_.toConf(indentationLevel + 1)))}
       |""".stripMargin
  }

  def toSMTLIB(algorithmType: AlgorithmType, isParallel: Boolean): String = {
    val algorithmConstrains = algorithmType match {
      case AlgorithmType.init => initializationToSMTLib(isParallel)
      case scenarioverifier.core.AlgorithmType.step => stepToSMTLib(isParallel)
      case scenarioverifier.core.AlgorithmType.event => eventEntrances.map(entry => eventSMTLib(entry, isParallel)).mkString("\n")
      case _ => throw new RuntimeException("Algorithm type not supported")
    }
    algorithmConstrains
  }

  private def commonSMTLib(actions: List[String], isParallel: Boolean): String = {
    s"""
       |; Minimum Action Number
       |(declare-const minAction Int)
       |(assert (= minAction 0))
       |; Maximum Action Number
       |(declare-const maxAction Int)
       |${if (!isParallel) s"(assert (= maxAction ${actions.length}))" else "(minimize maxAction)"}
       |; No actions are allowed to be executed in parallel
       |(assert (distinct ${actions.mkString(" ")}))
       |; Actions are between minAction and maxAction
       |${actions.map(p => s"(assert (and (>= $p minAction) (< $p maxAction)))").mkString("\n")}
       |""".stripMargin
  }

  private def feedthroughConstraints(dependencyExtractor: OutputPortModel => List[String]): String = connectedOutputPorts
    .flatMap(fmu =>
      fmu._2.flatMap(outputPort =>
        dependencyExtractor(outputPort._2).map(inputPort =>
          s"(assert (>= ${PortRef(fmu._1, outputPort._1).toSMTLib} ${PortRef(fmu._1, inputPort).toSMTLib}))")))
    .mkString("\n")

  private def initializationToSMTLib(isParallel: Boolean): String = {
    val actions = outputActions ++ inputActions
    s"""
       |; Input Ports - Actions
       |${inputActions.map(declareAction).mkString("\n")}
       |; Output Ports - Actions
       |${outputActions.map(declareAction).mkString("\n")}
       |; Feedthrough Constraints - input ports are set before output ports are read
       |${feedthroughConstraints(_.dependenciesInit)}
       |; Connections - the output value must be obtained before the coupled input can be set
       |${connectionsBetweenNonClockedVariables.map(_.toSMTLib).mkString("\n")}
       |${commonSMTLib(actions, isParallel)}
       |""".stripMargin
  }

  private def declareAction(action: String): String =
    s"(declare-const $action Int)"

  private def stepToSMTLib(isParallel: Boolean): String = {
    val stepNames = fmus.keys.toList
    val actions = outputActions ++ inputActions ++ stepNames.map(fmu => s"$fmu-step")
    s"""
       |; Input Ports - Actions
       |${inputActions.map(declareAction).mkString("\n")}
       |; Output Ports - Actions
       |${outputActions.map(declareAction).mkString("\n")}
       |; Step - Actions
       |${stepNames.map(fmu => declareAction(s"$fmu-step")).mkString("\n")}
       |; Reactive Ports - Actions are before Step Operations
       |${connectedInputPorts
        .map(p => (p._1, p._2.filter(_._2.reactivity == Reactivity.reactive)))
        .filter(_._2.nonEmpty)
        .flatMap(p => p._2.map(action => PortRef(p._1, action._1)))
        .map(p => s"(assert (< ${p.toSMTLib} ${p.fmu}-step))")
        .mkString("\n")}
       |; Delayed Ports - Actions are after Step Operations
       |${connectedInputPorts
        .map(p => (p._1, p._2.filter(_._2.reactivity == Reactivity.delayed)))
        .filter(_._2.nonEmpty)
        .flatMap(p => p._2.map(action => PortRef(p._1, action._1)))
        .map(p => s"(assert (> ${p.toSMTLib} ${p.fmu}-step))")
        .mkString("\n")}
       |; Feedthrough Constraints - input ports are set before output ports are read
       |${feedthroughConstraints(_.dependencies)}
       |; Connections - the output value must be obtained before the coupled input can be set
       |${connectionsBetweenNonClockedVariables.map(_.toSMTLib).mkString("\n")}
       |${commonSMTLib(actions, isParallel)}
       |""".stripMargin
  }

  private def clocksOfPort(portRef: PortRef, activeClocks: Predef.Set[PortRef]): List[PortRef] = {
    require(fmus.contains(portRef.fmu), s"FMU ${portRef.fmu} does not exist")
    val fmuModel = fmus(portRef.fmu)
    val clocks =
      if (fmuModel.inputs.contains(portRef.port))
        fmuModel.inputs(portRef.port).clocks
      else if (fmuModel.outputs.contains(portRef.port))
        fmuModel.outputs(portRef.port).clocks
      else
        throw new RuntimeException(s"Port $portRef does not exist")
    clocks.map(clock => PortRef(portRef.fmu, clock)).filter(activeClocks.contains)
  }

  private def clockDependenciesOfClock(clock: PortRef): List[PortRef] = {
    if (fmus(clock.fmu).inputClocks.contains(clock.port))
      List.empty
    else if (fmus(clock.fmu).outputClocks.contains(clock.port))
      fmus(clock.fmu).outputClocks(clock.port).dependenciesClocks.map(PortRef(clock.fmu, _))
    else
      throw new RuntimeException(s"Clock $clock does not exist")
  }

  def eventSMTLib(eventEntrance: EventEntrance, isParallel: Boolean): String = {
    val relevantClockedConnections = clockConnections.filter(connection =>
      eventEntrance.clocks.contains(connection.srcPort) || eventEntrance.clocks.contains(connection.trgPort))
    val relevantInputPorts = eventEntrance.clocks.flatMap(clock => clockedInputs(clock)).toList
    val relevantOutputPorts = eventEntrance.clocks.flatMap(clock => clockedOutputs(clock)).toList
    val outputActions: List[String] = relevantOutputPorts.map(_.toSMTLib)
    val inputActions: List[String] = relevantInputPorts.map(_.toSMTLib)
    val clockNamesActions = eventEntrance.clocks.map(_.toSMTLib)
    val relevantConnections =
      connections.filter(connection => relevantOutputPorts.contains(connection.srcPort) || relevantInputPorts.contains(connection.trgPort))

    val actions = inputActions ++ outputActions ++ clockNamesActions
    // assert(inputActions.size >= outputActions.size, "Input actions must be greater than output actions")
    s"""
       |; Event Entrance constraint
       |; Input Ports - Actions
       |${inputActions.map(declareAction).mkString("\n")}
       |; Output Ports - Actions
       |${outputActions.map(declareAction).mkString("\n")}
       |; Clocks - Actions
       |${clockNamesActions.map(declareAction).mkString("\n")}
       |; All clocked actions are performed after their clock actions - input actions (Rule 4 + 5)
       |${relevantInputPorts
        .flatMap(portAction =>
          clocksOfPort(portAction, eventEntrance.clocks).map(clock => s"(assert (> ${portAction.toSMTLib} ${clock.toSMTLib}))"))
        .mkString("\n")}
       |; All clocked actions are performed before their clock actions - outputs actions (Rule 4 + 5)
       |${relevantOutputPorts
        .flatMap(action => clocksOfPort(action, eventEntrance.clocks).map(clock => s"(assert (< ${action.toSMTLib} ${clock.toSMTLib}))"))
        .mkString("\n")}
       |; Connections - get before set (Rule 3)
       |${relevantConnections.map(_.toSMTLib).mkString("\n")}
       |; Clock Connections - get before set (Rule 1)
       |${relevantClockedConnections.map(_.toSMTLib).mkString("\n")}
       |; Clock Dependencies - set before get (Rule 2)
       |${eventEntrance.clocks
        .flatMap(clock => clockDependenciesOfClock(clock).map(dep => s"(assert (> ${clock.toSMTLib} $dep))"))
        .mkString("\n")}
       |${commonSMTLib(actions, isParallel)}
       |""".stripMargin
  }
}

final case class InputPortModel(reactivity: Reactivity, clocks: List[String]) extends ConfElement {
  override def toConf(indentationLevel: Int = 0): String = s"{reactivity=${reactivity.toString}, clocks=${toArray(clocks)}}"
}

final case class OutputPortModel(dependenciesInit: List[String], dependencies: List[String], clocks: List[String])
    extends UppaalModel
    with ConfElement {
  override def toUppaal: String = s"{${dependencies.mkString(",")}}"

  override def toConf(indentationLevel: Int = 0): String =
    s"{dependencies-init=${toArray(dependenciesInit)}, dependencies=${toArray(dependencies)}, clocks=${toArray(clocks)}}"
}

final case class OutputClockModel(typeOfClock: ClockType, dependencies: List[String], dependenciesClocks: List[String])
    extends ConfElement {
  override def toConf(indentationLevel: Int = 0): String =
    s"{typeOfClock=${typeOfClock.toString}, dependencies=${toArray(dependencies)}, dependencies-clocks=${toArray(dependenciesClocks)}}"
}

final case class InputClockModel(typeOfClock: ClockType, interval: Int) extends ConfElement {

  require(
    typeOfClock == ClockType.triggered && interval == 0 || typeOfClock == ClockType.timed && interval > 0,
    "Interval must be greater than 0 for time-based clocks and 0 for triggered clocks")

  override def toConf(indentationLevel: Int = 0): String = s"{typeOfClock=${typeOfClock.toString}, interval=$interval}"
}

final case class MasterModel3(
    name: String,
    scenario: FMI3ScenarioModel,
    instantiation: List[InstantiationInstruction] = List.empty,
    initialization: List[InitializationInstruction] = List.empty,
    cosimStep: List[CosimStepInstruction] = List.empty,
    eventStrategies: Map[String, EventStrategy] = Map.empty,
    terminate: List[TerminationInstruction] = List.empty)
    extends ConfElement
    with SMTLibElement {
  require(name.nonEmpty, "Master model name cannot be empty")

  /**
   * Formats the master model to a CONF file
   */
  override def toConf(indentationLevel: Int = 0): String = {
    val init = toArray(initialization.map(_.toConf(indentationLevel + 1)), "\n")
    val step = toArray(cosimStep.map(_.toConf(indentationLevel + 1)), "\n")
    s"""name = $name
       |scenario = {
       |  ${scenario.toConf(indentationLevel + 1)}
       |}
       |initialization = $init
       |cosim-step = $step
       |""".stripMargin
  }

  private def formatEvents(synthesize: Boolean, isParallel: Boolean): String = {
    scenario.eventEntrances
      .map(eventEntrance => {
        val algorithmAssertions = if (!synthesize) {
          val instructions = eventStrategies.values
            .find(_.eventEntrance == eventEntrance)
            .getOrElse(throw new RuntimeException(s"Event entrance ${eventEntrance.clocks.mkString(",")} not found"))
            .algorithm
          instructions.indices.map(i => s"(assert (= ${instructions(i).toSMTLib} $i))").mkString("\n")
        } else ""
        s"""
         |; Event Entrance for the clocks ${eventEntrance.clocks.map(_.toSMTLib).mkString(", ")}
         |(push 1)
         |${scenario.eventSMTLib(eventEntrance, isParallel)}
         |$algorithmAssertions
         |(check-sat)
         |${if (synthesize) "(get-model)" else ""}
         |(pop 1)
         |""".stripMargin
      })
      .mkString("\n")
  }

  /**
   * Formats the master model to a SMT-LIB file
   *
   * @return
   *   The SMT-LIB representation of the master model as a string
   */
  def toSMTLib(algorithmTypes: List[AlgorithmType], synthesize: Boolean, isParallel: Boolean): String = {
    def formatAlgorithm(algorithmType: AlgorithmType, instructions: List[SMTLibElement]): String = {
      val algorithmAssertions = instructions.indices.map(i => s"(assert (= ${instructions(i).toSMTLib} $i))").mkString("\n")
      s"""
         |; ${algorithmType.toString} algorithm
         |(push 1)
         |${scenario.toSMTLIB(algorithmType, isParallel)}
         |${if (!synthesize) algorithmAssertions else ""}
         |(check-sat)
         |${if (synthesize) "(get-model)" else ""}
         |(pop 1)
         |""".stripMargin
    }

    val initInstructions = initialization
      .filter(instruction =>
        instruction.isInstanceOf[InitGet] || instruction.isInstanceOf[InitSet] || instruction.isInstanceOf[GetShift] || instruction
          .isInstanceOf[GetInterval])
    val stepInstructions = cosimStep
      .filter(instruction =>
        instruction.isInstanceOf[Get] || instruction.isInstanceOf[scenarioverifier.core.Set] || instruction.isInstanceOf[Step])

    s"""
       |(set-option :produce-models true)
       |(set-logic QF_LIA)
       |${if (algorithmTypes.contains(AlgorithmType.init)) formatAlgorithm(AlgorithmType.init, initInstructions) else ""}
       |${if (algorithmTypes.contains(AlgorithmType.step)) formatAlgorithm(AlgorithmType.step, stepInstructions) else ""}
       |${if (algorithmTypes.contains(AlgorithmType.event)) formatEvents(synthesize, isParallel) else ""}
       |""".stripMargin
  }

  override def toSMTLib: String =
    toSMTLib(List(AlgorithmType.init, AlgorithmType.step, AlgorithmType.event), synthesize = false, isParallel = false)
}

case class MasterModelDTO(
    name: String,
    scenario: FMI3ScenarioModel,
    initialization: List[InitializationInstruction],
    cosimStep: List[CosimStepInstruction],
    eventStrategies: Map[String, EventStrategy])

final case class EventEntrance(clocks: immutable.Set[PortRef]) {
  require(clocks.nonEmpty, "Event entrance must contain at least one clock")
}

final case class EventStrategy(eventEntrance: EventEntrance, algorithm: List[EventInstruction])
