package org.intocps.verification.scenarioverifier.synthesizer

import scala.collection.immutable.HashSet

import org.intocps.verification.scenarioverifier
import org.intocps.verification.scenarioverifier.core.ConnectionModel
import org.intocps.verification.scenarioverifier.core.CosimStepInstruction
import org.intocps.verification.scenarioverifier.core.DefaultStepSize
import org.intocps.verification.scenarioverifier.core.Get
import org.intocps.verification.scenarioverifier.core.InitGet
import org.intocps.verification.scenarioverifier.core.InitSet
import org.intocps.verification.scenarioverifier.core.InitializationInstruction
import org.intocps.verification.scenarioverifier.core.NoOP
import org.intocps.verification.scenarioverifier.core.OutputPortModel
import org.intocps.verification.scenarioverifier.core.PortRef
import org.intocps.verification.scenarioverifier.core.Reactivity.reactive
import org.intocps.verification.scenarioverifier.core.RestoreState
import org.intocps.verification.scenarioverifier.core.SaveState
import org.intocps.verification.scenarioverifier.core.ScenarioModel
import org.intocps.verification.scenarioverifier.core.SetTentative
import org.intocps.verification.scenarioverifier.core.Step

sealed trait Node {
  def fmuName: String
}

trait InitializationInstructionNode extends Node {
  def formatInitInstruction: InitializationInstruction
}

trait StepInstructionNode extends Node {
  def formatStepInstruction: CosimStepInstruction
}

trait PortNode extends InitializationInstructionNode with StepInstructionNode {
  def port: PortRef
}

case class DoStepNode(override val fmuName: String) extends StepInstructionNode {
  override def formatStepInstruction: CosimStepInstruction = Step(fmuName, DefaultStepSize())
}

case class GetNode(override val fmuName: String, override val port: PortRef) extends PortNode {
  override def formatInitInstruction: InitializationInstruction = InitGet(port)

  override def formatStepInstruction: CosimStepInstruction = Get(port)
}

//case class SetOptimizedNode(override val fmuName: String, ports: Set[PortRef])

// case class GetOptimizedNode(override val fmuName: String, ports: Set[PortRef]) extends Node(fmuName)

case class SetNode(override val fmuName: String, override val port: PortRef) extends PortNode {
  override def formatInitInstruction: InitializationInstruction = InitSet(port)

  override def formatStepInstruction: CosimStepInstruction = scenarioverifier.core.Set(port)
}

case class SetTentativeNode(override val fmuName: String, port: PortRef) extends PortNode {
  override def formatStepInstruction: CosimStepInstruction = SetTentative(port)

  override def formatInitInstruction: InitializationInstruction = scenarioverifier.core.InitSet(port)
}

case class RestoreNode(override val fmuName: String) extends StepInstructionNode {
  override def formatStepInstruction: CosimStepInstruction = RestoreState(fmuName)
}

final case class SaveNode(override val fmuName: String) extends StepInstructionNode {
  override def formatStepInstruction: CosimStepInstruction = SaveState(fmuName)
}

final case class EmptyNode() extends StepInstructionNode {
  override val fmuName: String = "EmptyNode"

  override def formatStepInstruction: CosimStepInstruction = NoOP
}

final case class Edge[A](srcNode: A, trgNode: A)

class GraphBuilder(scenario: ScenarioModel, val removeTransitive: Boolean = false) {
  private val stepNodes: Set[DoStepNode] = scenario.fmus.map(fmu => DoStepNode(fmu._1)).toSet
  val GetNodes: Map[String, Set[GetNode]] =
    scenario.fmus.map(fmu => (fmu._1, fmu._2.outputs.map(o => GetNode(fmu._1, PortRef(fmu._1, o._1))).toSet))
  private val SetNodesReactive: Map[String, Set[SetTentativeNode]] =
    scenario.fmus.map(fmu =>
      (fmu._1, fmu._2.inputs.filter(i => i._2.reactivity == reactive).map(i => SetTentativeNode(fmu._1, PortRef(fmu._1, i._1))).toSet))
  private val SetNodesDelayed: Map[String, Set[SetNode]] =
    scenario.fmus.map(fmu =>
      (fmu._1, fmu._2.inputs.filter(i => i._2.reactivity != reactive).map(i => SetNode(fmu._1, PortRef(fmu._1, i._1))).toSet))
  // assert(SetNodesReactive.values.flatten.map(_.isInstanceOf[StepInstructionNode]).toSet.intersect(SetNodesDelayed.values.flatten.map(_.isInstanceOf[StepInstructionNode]).toSet).isEmpty, "Reactive and delayed inputs are not disjoint")
  // private val SetOptimizedNodesReactive: Map[String, SetOptimizedNode] = SetNodesReactive.map(node => (node._1, SetOptimizedNode(node._1, node._2.map(_.port)))).filter(i => i._2.ports.nonEmpty)
  // private val SetOptimizedNodesDelayed: Map[String, SetOptimizedNode] = SetNodesDelayed.map(node => (node._1, SetOptimizedNode(node._1, node._2.map(_.port)))).filter(i => i._2.ports.nonEmpty)
  // private val GetOptimizedNodes: Map[String, GetOptimizedNode] = GetNodes.map(node => (node._1, GetOptimizedNode(node._1, node._2.map(_.port))))
  // private val SetOptimizedNodes: Map[String, SetOptimizedNode] = scenario.fmus.map(fmu => (fmu._1, SetOptimizedNode(fmu._1, fmu._2.inputs.map(i => PortRef(fmu._1, i._1)).toSet))).filter(i => i._2.ports.nonEmpty)

  val reactiveConnections: Seq[ConnectionModel] = scenario.connections.filter(c => isReactive(c.trgPort))

  lazy val RejectFMUs: Set[String] =
    mayReject(HashSet.empty[String], HashSet.empty[String], scenario.fmus.filter(o => o._2.canRejectStep).keys.to(HashSet))

  @scala.annotation.tailrec
  private final def mayReject(rejects: HashSet[String], normal: HashSet[String], toAdd: HashSet[String]): HashSet[String] = {
    if (toAdd.isEmpty) rejects
    else {
      val FMUsToAdd = reactiveConnections
        .filter(o => rejects.contains(o.trgPort.fmu) && normal.contains(o.srcPort.fmu))
        .map(o => o.srcPort.fmu)
        .to(HashSet)
      mayReject(rejects ++ toAdd, normal -- toAdd, FMUsToAdd)
    }
  }

  private def feedthroughByType(dependencyTypes: OutputPortModel => List[String]): Set[Edge[PortNode]] =
    scenario.fmus
      .flatMap(f => {
        f._2.outputs.map(o =>
          (
            GetNodes(f._1).find(_ == GetNode(f._1, PortRef(f._1, o._1))).get,
            dependencyTypes(o._2).map(i => {
              if (SetNodesDelayed.contains(f._1) && SetNodesDelayed(f._1).contains(SetNode(f._1, PortRef(f._1, i)))) {
                SetNodesDelayed(f._1).find(_ == SetNode(f._1, PortRef(f._1, i))).get
              } else {
                SetNodesReactive(f._1).find(_ == SetTentativeNode(f._1, PortRef(f._1, i))).get
              }
            })))
      })
      .flatMap(f => f._2.map(i => Edge[PortNode](i, f._1)))
      .toSet

  lazy val connectionEdges: Set[Edge[PortNode]] = {
    scenario.connections
      .map(c => {
        val src = GetNodes.values.flatten.find(_ == GetNode(c.srcPort.fmu, c.srcPort)).get
        val trg =
          if (SetNodesDelayed.values.flatten.exists(_ == SetNode(c.trgPort.fmu, c.trgPort)))
            SetNodesDelayed.values.flatten.find(_ == SetNode(c.trgPort.fmu, c.trgPort)).get
          else
            SetNodesReactive.values.flatten.find(_ == SetTentativeNode(c.trgPort.fmu, c.trgPort)).get
        Edge[PortNode](src, trg)
      })
      .toSet
  }

  private def edgesLinkedToStep(
      steps: Set[DoStepNode],
      gets: Map[String, Set[GetNode]],
      setsReactive: Map[String, Set[SetTentativeNode]],
      setsDelayed: Map[String, Set[SetNode]]): Set[Edge[CosimStepInstruction]] = {
    val stepToGet = steps.flatMap(step => gets(step.fmuName).map(o => Edge(step, o)))
    val reactiveToStep = steps.flatMap(step => setsReactive(step.fmuName).map(i => Edge(i, step)))
    val stepToDelayed = steps.flatMap(step => setsDelayed(step.fmuName).map(i => Edge(step, i)))
    (stepToGet ++ reactiveToStep ++ stepToDelayed).map(i => i.asInstanceOf[Edge[CosimStepInstruction]])
  }

  private def stepFindingEdges: Set[Edge[Node]] = {
    // FMUs connected reactively that both may reject a step should be connected
    val combinations = RejectFMUs.subsets(2).map(_.toList).toList
    val edgesBetweenRejectFMUs = combinations.map(i => Edge[Node](DoStepNode(i.head), DoStepNode(i.last))).toSet ++ combinations
      .map(i => Edge[Node](DoStepNode(i.last), DoStepNode(i.head)))
      .toSet

    // This makes sure step loops will be run first
    val FMUsCanTakeArbitraryStep = scenario.fmus.keySet.diff(RejectFMUs)
    val edgesFromRejectFMUsToNonRejectFMUs =
      RejectFMUs.flatMap(i => FMUsCanTakeArbitraryStep.map(o => Edge[Node](DoStepNode(i), DoStepNode(o))))
    val edges = edgesBetweenRejectFMUs ++ edgesFromRejectFMUsToNonRejectFMUs
    edges
  }

  def reactiveOutAndDelayedIn(fmu: String): Boolean = {
    val reactiveConnectionsFromFMU = reactiveConnections.filter(i => i.srcPort.fmu == fmu).map(_.trgPort.fmu).toSet
    val delayedConnectionsToFMU = scenario.connections.filter(i => !isReactive(i.trgPort) && i.trgPort.fmu == fmu).map(_.srcPort.fmu).toSet
    reactiveConnectionsFromFMU.intersect(delayedConnectionsToFMU).isEmpty
  }

  def inputFromStepRejectFMU(fmu: String): Boolean = {
    if (RejectFMUs.contains(fmu)) {
      val delayedConnectionsToFMU =
        scenario.connections.filterNot(reactiveConnections.contains).filter(_.trgPort.fmu == fmu).map(_.srcPort.fmu).toSet
      return delayedConnectionsToFMU.intersect(RejectFMUs).isEmpty
    }
    true
  }

  def isReactive(port: PortRef): Boolean = {
    SetNodesReactive.values.exists(o => o.map(o => o.port).contains(port))
  }

  /*
  lazy val connectionEdgesOptimizedInit: Set[Edge[Node]] = {
    scenario.connections.map(c => Edge[Node](GetOptimizedNodes(c.srcPort.fmu), SetOptimizedNodes(c.trgPort.fmu))).toSet
  }

  lazy val connectionEdgesOptimized: Set[Edge[Node]] = {
    scenario.connections.filter(ifReactiveNoDelayedConnection).map(c => Edge[Node](GetOptimizedNodes(c.srcPort.fmu),
      if (isReactive(c.trgPort)) SetOptimizedNodesReactive(c.trgPort.fmu)
      else SetOptimizedNodesDelayed(c.trgPort.fmu))).toSet
  }
   */

  lazy val initialEdges: Set[Edge[InitializationInstructionNode]] =
    (connectionEdges ++ feedthroughByType(_.dependenciesInit))
      .map(i =>
        Edge[InitializationInstructionNode](
          i.srcNode.asInstanceOf[InitializationInstructionNode],
          i.trgNode.asInstanceOf[InitializationInstructionNode]))

  lazy val stepEdges: Set[Edge[StepInstructionNode]] =
    (connectionEdges ++ feedthroughByType(_.dependencies) ++
      edgesLinkedToStep(stepNodes, GetNodes, SetNodesReactive, SetNodesDelayed) ++
      stepFindingEdges)
      .map(i => Edge[StepInstructionNode](i.srcNode.asInstanceOf[StepInstructionNode], i.trgNode.asInstanceOf[StepInstructionNode]))

  // lazy val initialEdgesOptimized: Set[Edge[Node]] = connectionEdgesOptimizedInit ++ feedthroughOptimizedInit

  /*
  lazy val stepEdgesOptimized: Set[Edge[Node]] =
    connectionEdgesOptimized ++ feedthroughOptimized ++
      edgesLinkedToStep(stepNodes,
        GetOptimizedNodes,
        SetOptimizedNodesReactive,
        SetOptimizedNodesDelayed
      ) ++ stepFindingEdges
   */

  private def removeEdges(edges: Set[Edge[Node]]): Set[Edge[Node]] = {
    val tarjanGraph = new TarjanGraph[Node](edges)
    GraphUtil.removeTransitiveEdges(edges, tarjanGraph.tarjanCycle.toSet)
  }
}
