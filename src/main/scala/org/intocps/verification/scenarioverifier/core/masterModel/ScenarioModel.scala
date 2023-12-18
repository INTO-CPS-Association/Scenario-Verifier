package org.intocps.verification.scenarioverifier.core.masterModel
import scala.collection.immutable

import org.intocps.verification.scenarioverifier.core.FMI3._
import org.intocps.verification.scenarioverifier.core.PortRef
import AlgorithmType.AlgorithmType
import ClockType.ClockType

trait ScenarioModel extends ConfElement {
  def fmus: Map[String, FmuModel]
  def connections: List[ConnectionModel]
  def maxPossibleStepSize: Int

  def toSMTLIB(algorithmType: AlgorithmType): String
  override def toConf(indentationLevel: Int): String = {
    s"""
       |${indentBy(indentationLevel)}fmus = {
       |${indentBy(indentationLevel + 1)}${fmus
        .map { case (fmu, fmuModel) => s"$fmu = ${fmuModel.toConf(indentationLevel + 1)}" }
        .mkString("\n")}
       |${indentBy(indentationLevel)}}
       |${indentBy(indentationLevel)}connections = ${toArray(connections.map(_.toConf(indentationLevel + 1)))}
       |""".stripMargin
  }
}

final case class FMI2ScenarioModel(
    fmus: Map[String, Fmu2Model],
    config: AdaptiveModel,
    connections: List[ConnectionModel],
    maxPossibleStepSize: Int)
    extends ScenarioModel {

  require(fmus.nonEmpty, "fmus must not be empty")
  require(maxPossibleStepSize > 0, "maxPossibleStepSize must be greater than 0")

  def enrich(): FMI2ScenarioModel = {
    val enrichedFmus = fmus.map { fmu =>
      connections
        .filter(c => c.srcPort.fmu.equalsIgnoreCase(fmu._1) || c.trgPort.fmu.equalsIgnoreCase(fmu._1))
        .foldLeft(fmu)((fmuModel, c) => {
          val model =
            if (c.trgPort.fmu.equalsIgnoreCase(fmu._1) && !fmuModel._2.inputs.contains(c.trgPort.port))
              fmuModel._2.copy(inputs = fmuModel._2.inputs + (c.trgPort.port -> FMI2InputPortModel(Reactivity.delayed)))
            else if (c.srcPort.fmu.equalsIgnoreCase(fmu._1) && !fmuModel._2.outputs.contains(c.srcPort.port))
              fmuModel._2.copy(outputs = fmuModel._2.outputs + (c.srcPort.port -> FMI2OutputPortModel(List.empty, List.empty)))
            else fmuModel._2
          (fmuModel._1, model)
        })
    }
    this.copy(fmus = enrichedFmus)
  }

  override def toSMTLIB(algorithmType: AlgorithmType): String = {
    require(fmus.nonEmpty, "fmus must not be empty")
    require(connections.nonEmpty, "connections must not be empty")
    require(config.configurations.size <= 1, "the scenario must not be adaptive")
    val actionsWithoutStep: immutable.Iterable[String] =
      fmus
        .foldLeft(immutable.Iterable.empty[String])((actions, fmu) =>
          fmu._2.inputs.map(port => s"${sanitizeString(fmu._1)}_${sanitizeString(port._1)}").toList ++
            fmu._2.outputs.map(port => s"${sanitizeString(fmu._1)}_${sanitizeString(port._1)}").toList ++
            actions)
        .toList
        .sorted

    val actions =
      if (algorithmType == AlgorithmType.step) actionsWithoutStep ++ fmus.map(fmu => s"${fmu._1}_step").toList else actionsWithoutStep
    val numberOfActions = actions.size
    val fmuDeclarations = fmus.keySet.toList.sorted.map(fmu => fmus(fmu).toSMTLib(fmu, algorithmType)).mkString("\n")
    val connectionAssertions = connections.map(_.toSMTLib).mkString("\n")
    val reactiveInputs = fmus.flatMap(fmu => fmu._2.reactiveInputs.map(i => PortRef(fmu._1, i._1))).toList
    val reactiveConnections = connections.filter(c => reactiveInputs.contains(c.srcPort))
    val delayedConnections = connections.filterNot(c => reactiveInputs.contains(c.trgPort))
    val delayedConstraints = delayedConnectionConstraints(delayedConnections)
    s"""$fmuDeclarations
       |; Connections - Assert that the source port is smaller than the target port
       |$connectionAssertions
       |; Delayed connections - The get and set can either be done before or after the step - but they need to be consistent across the connection
       |; Assert that all the actions are bigger than 0
       |(assert (and ${actions.map(a => s"(>= $a 0)").mkString("\n\t")}))
       |; Assert that all the actions are smaller than the maxAction
       |(assert (and ${actions.map(action => s"(< $action $numberOfActions)").mkString("\n\t")}))
       |; Assert that all actions are different
       |(assert (distinct ${actions.mkString("\n\t")}))
       |""".stripMargin
  }

  private def delayedConnectionConstraints(delayedConnections: List[ConnectionModel]) = {
    delayedConnections
      .map(c => {
        val srcActionName = s"${sanitizeString(c.srcPort.fmu)}_${sanitizeString(c.srcPort.port)}"
        val trgActionName = s"${sanitizeString(c.trgPort.fmu)}_${sanitizeString(c.trgPort.port)}"
        val srcFMUStep = s"${sanitizeString(c.srcPort.fmu)}_step"
        val trgFMUStep = s"${sanitizeString(c.trgPort.fmu)}_step"
        // Delayed connections - The get and set can either be done before or after the step
        s"""
         |(assert (or
         |          (and (< $srcActionName $srcFMUStep) (< $trgActionName $trgFMUStep))
         |          (and (> $srcActionName $srcFMUStep) (> $trgActionName $trgFMUStep))
         |))""".stripMargin
      })
      .mkString("\n")
  }
}

final case class FMI3ScenarioModel(
    fmus: Map[String, Fmu3Model],
    connections: List[ConnectionModel],
    clockConnections: List[ConnectionModel],
    maxPossibleStepSize: Int)
    extends ScenarioModel {

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
        fmus.flatMap(fmu =>
          fmu._2.inputClocks.filter(clock => clock._2.typeOfClock == ClockType.timed).map(clock => PortRef(fmu._1, clock._1) -> 1))
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
              fmuModel._2.copy(inputs = fmuModel._2.inputs + (c.trgPort.port -> FMI3InputPortModel(Reactivity.delayed, List.empty)))
            else if (c.srcPort.fmu.equalsIgnoreCase(fmu._1) && !fmuModel._2.outputs.contains(c.srcPort.port))
              fmuModel._2.copy(outputs = fmuModel._2.outputs + (c.srcPort.port -> FMI3OutputPortModel(List.empty, List.empty, List.empty)))
            else fmuModel._2
          (fmuModel._1, model)
        })
    }
    this.copy(fmus = enrichedFmus)
  }

  lazy val scenarioModel: FMI2ScenarioModel =
    FMI2ScenarioModel(
      fmus.map(fmu =>
        (
          fmu._1,
          Fmu2Model(
            fmu._2.inputs.map(input => (input._1, FMI2InputPortModel(input._2.reactivity))),
            fmu._2.outputs.map(output => (output._1, FMI2OutputPortModel(output._2.dependenciesInit, output._2.dependencies))),
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
       |${indentBy(indentationLevel)}connections = ${toArray(connections.map(_.toConf(indentationLevel + 1)), "\n")}
       |${indentBy(indentationLevel)}clock-connections = ${toArray(clockConnections.map(_.toConf(indentationLevel + 1)), "\n")}
       |""".stripMargin
  }

  def toSMTLIB(algorithmType: AlgorithmType, isParallel: Boolean): String = {
    val algorithmConstrains = algorithmType match {
      case AlgorithmType.init => initializationToSMTLib(isParallel)
      case AlgorithmType.step => stepToSMTLib(isParallel)
      case AlgorithmType.event => eventEntrances.map(entry => eventSMTLib(entry, isParallel)).mkString("\n")
      case _ => throw new RuntimeException("Algorithm type not supported")
    }
    algorithmConstrains
  }

  override def toSMTLIB(algorithmType: AlgorithmType): String = {
    toSMTLIB(algorithmType, false)
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

final case class EventStrategy(eventEntrance: EventEntrance, algorithm: List[EventInstruction]) extends ConfElement {

  /**
   * Formats the master model to a CONF file
   */
  override def toConf(indentationLevel: Int = 0): String = {
    s"""${indentBy(indentationLevel)}{
        |${indentBy(indentationLevel + 1)}clocks = ${eventEntrance.toConf(indentationLevel + 1)},
        |${indentBy(indentationLevel + 1)}iterate = ${toArray(algorithm.map(_.toConf(indentationLevel + 2)), "\n")} 
        |${indentBy(indentationLevel)}}
        |""".stripMargin
  }
}
