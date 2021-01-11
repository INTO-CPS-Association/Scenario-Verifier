package synthesizer

import core.Reactivity.{delayed, noPort, reactive}
import core.{ConnectionModel, FmuModel, InputPortConfig, PortRef, ScenarioModel}
import org.apache.logging.log4j.scala.Logging

import scala.collection.immutable.{AbstractSet, HashSet, Queue, SortedSet}

sealed abstract class Node(val fmuName:String)

case class DoStepNode(override val fmuName: String) extends Node(fmuName)

case class GetNode(override val fmuName: String, port: PortRef) extends Node(fmuName)

case class SetOptimizedNode(override val fmuName: String, ports: Set[PortRef]) extends Node(fmuName)

case class GetOptimizedNode(override val fmuName: String, ports: Set[PortRef]) extends Node(fmuName)

case class SetNode(override val fmuName: String, port: PortRef) extends Node(fmuName)

case class RestoreNode(override val fmuName: String) extends Node(fmuName)

case class SaveNode(override val fmuName: String) extends Node(fmuName)

case class Edge[Node](srcNode: Node, trgNode: Node)

case class EdgeCost(val srcNode: Node, val trgNode: Node, cost: Int)

class GraphBuilder(scenario: ScenarioModel, val removeTransitive: Boolean = false) extends Logging {
  val stepNodes: Set[DoStepNode] = scenario.fmus.map(fmu => DoStepNode(fmu._1)).toSet
  val GetNodes: Map[String, Set[GetNode]] = scenario.fmus.map(fmu => (fmu._1, fmu._2.outputs.map(o => GetNode(fmu._1, PortRef(fmu._1, o._1))).toSet))
  val SetNodesReactive: Map[String, Set[SetNode]] = scenario.fmus.map(fmu => (fmu._1, fmu._2.inputs.filter(i => i._2.reactivity == reactive).map(i => SetNode(fmu._1, PortRef(fmu._1, i._1))).toSet))
  val SetNodesDelayed: Map[String, Set[SetNode]] = scenario.fmus.map(fmu => (fmu._1, fmu._2.inputs.filter(i => i._2.reactivity != reactive).map(i => SetNode(fmu._1,PortRef(fmu._1, i._1))).toSet))
  val SetNodes: Map[String, Set[SetNode]] = scenario.fmus.map(fmu => (fmu._1, fmu._2.inputs.map(i => SetNode(fmu._1, PortRef(fmu._1, i._1))).toSet))
  val SetOptimizedNodesReactive: Map[String, SetOptimizedNode] = SetNodesReactive.map(node => (node._1, SetOptimizedNode(node._1, node._2.map(_.port)))).filter(i => i._2.ports.nonEmpty)
  val SetOptimizedNodesDelayed: Map[String, SetOptimizedNode] = SetNodesDelayed.map(node => (node._1, SetOptimizedNode(node._1, node._2.map(_.port)))).filter(i => i._2.ports.nonEmpty)
  val GetOptimizedNodes: Map[String, GetOptimizedNode] = GetNodes.map(node => (node._1, GetOptimizedNode(node._1, node._2.map(_.port))))
  val SetOptimizedNodes: Map[String, SetOptimizedNode] = scenario.fmus.map(fmu => (fmu._1, SetOptimizedNode(fmu._1, fmu._2.inputs.map(i => (PortRef(fmu._1, i._1))).toSet))).filter(i => i._2.ports.nonEmpty)

  val reactiveConnections: Seq[ConnectionModel] = scenario.connections.filter(c => isReactive(c.trgPort))

  lazy val RejectFMUs: Set[String] = mayReject(HashSet.empty[String], HashSet.empty[String], scenario.fmus.filter(o => o._2.canRejectStep).keys.to(HashSet))

  @scala.annotation.tailrec
  final def mayReject(rejects: HashSet[String], normal: HashSet[String], toAdd: HashSet[String]): HashSet[String] = {
    toAdd.isEmpty match {
      case true => rejects
      case false => {
        val FMUsToAdd = reactiveConnections.filter(o => rejects.contains(o.trgPort.fmu) && normal.contains(o.srcPort.fmu)).map(o => o.srcPort.fmu).to(HashSet)
        mayReject(rejects ++ toAdd, normal -- toAdd, FMUsToAdd)
      }
    }
  }

  private def feedthroughInit: Set[Edge[Node]] =
    scenario.fmus.flatMap(f => {
      f._2.outputs.map(o => (GetNodes(f._1).find(_ == GetNode(f._1, PortRef(f._1, o._1))).get,
        o._2.dependenciesInit.map(i => (SetNodes(f._1).find(_ == SetNode(f._1, PortRef(f._1, i))).get)).toSet))
    }).flatMap(f => f._2.map(i => Edge[Node](i, f._1))).toSet

  private def feedthrough: Set[Edge[Node]] =
    scenario.fmus.flatMap(f => {
      f._2.outputs.map(o => (GetNodes(f._1).find(_ == GetNode(f._1, PortRef(f._1, o._1))).get,
        o._2.dependencies.map(i => (SetNodes(f._1).find(_ == SetNode(f._1, PortRef(f._1, i))).get)).toSet))
    }).flatMap(f => f._2.map(i => Edge[Node](i, f._1))).toSet

  private def feedthroughOptimizedInit: List[Edge[Node]] =
    scenario.fmus.flatMap(f =>
      f._2.outputs.filter(o => o._2.dependenciesInit.nonEmpty).map(_ =>
        Edge[Node](SetOptimizedNodes(f._1), GetOptimizedNodes(f._1)))).toList

  private def feedthroughOptimized: List[Edge[Node]] = {
    var feedthroughConnections = List[Edge[Node]]()
    scenario.fmus.foreach(f => {
      if (f._2.outputs.flatMap(o => o._2.dependencies.map(i => !isReactive(PortRef(f._1, i)))).exists(i => i))
        feedthroughConnections ++= List(Edge[Node](SetOptimizedNodesDelayed(f._1), GetOptimizedNodes(f._1)))
    })
    feedthroughConnections
  }


  lazy val connectionEdges: Set[Edge[Node]] = {
    scenario.connections.map(c => Edge[Node](GetNodes.values.flatten.find(_ == GetNode(c.srcPort.fmu, c.srcPort)).get, SetNodes.values.flatten.find(_ == SetNode(c.trgPort.fmu, c.trgPort)).get)).toSet
  }


  private def doStepEdges: Set[Edge[Node]] = {
    val stepToGet = stepNodes.flatMap(step => GetNodes(step.fmuName).map(o => Edge[Node](step, o)))
    val reactiveToStep = stepNodes.flatMap(step => SetNodesReactive(step.fmuName).map(i => Edge[Node](i, step)))
    val stepToDelayed = stepNodes.flatMap(step => SetNodesDelayed(step.fmuName).map(i => Edge[Node](step, i)))
    stepToGet ++ reactiveToStep ++ stepToDelayed
  }

  private def StepFindingEdges: Set[Edge[Node]] = {
    //FMUs connected reactively that both may reject a step should be connected
    reactiveConnections.filter(o => RejectFMUs.contains(o.trgPort.fmu) && RejectFMUs.contains(o.srcPort.fmu))
      .map(o => Edge[Node](DoStepNode(o.trgPort.fmu), DoStepNode(o.srcPort.fmu))).toSet
  }


  def reactiveOutAndDelayedIn(fmu: String): Boolean = {
    val reactiveConnectionsFromFMU = reactiveConnections.filter(i => i.srcPort.fmu == fmu).map(_.trgPort.fmu).toSet
    val delayedConnectionsToFMU = scenario.connections.filter(i => !isReactive(i.trgPort) && i.trgPort.fmu == fmu).map(_.srcPort.fmu).toSet
    reactiveConnectionsFromFMU.intersect(delayedConnectionsToFMU).isEmpty
  }

  def inputFromStepRejectFMU(fmu: String): Boolean = {
    if(RejectFMUs.contains(fmu)) {
      val delayedConnectionsToFMU = scenario.connections.filterNot(reactiveConnections.contains).filter(_.trgPort.fmu == fmu).map(_.srcPort.fmu).toSet
      return delayedConnectionsToFMU.intersect(RejectFMUs).isEmpty
    }
    true
  }

  private def doStepEdgesOptimized: Set[Edge[Node]] = {
    val stepToGet = stepNodes.map(step => Edge[Node](step, GetOptimizedNodes(step.fmuName)))
    val reactiveToStep = SetOptimizedNodesReactive.map(i => Edge[Node](i._2, DoStepNode(i._1)))
    val stepToDelayed = SetOptimizedNodesDelayed.map(i => Edge[Node](DoStepNode(i._1), i._2))
    stepToGet ++ reactiveToStep ++ stepToDelayed
  }

  def isReactive(port: PortRef): Boolean = {
    SetNodesReactive.values.exists(o => o.map(o => o.port).contains(port))
  }

  lazy val connectionEdgesOptimizedInit: Set[Edge[Node]] = {
    scenario.connections.map(c => Edge[Node](GetOptimizedNodes(c.srcPort.fmu), SetOptimizedNodes(c.trgPort.fmu))).toSet
  }

  def ifNoReactiveConnectionFrom(srcFMU: String, trgFMU: String): Boolean = {
    !scenario.connections.exists(i => isReactive(i.trgPort) && i.trgPort.fmu == trgFMU && i.srcPort.fmu == srcFMU)
  }

  def ifReactiveNoDelayedConnection(connection: ConnectionModel): Boolean = {
    if (isReactive(connection.trgPort)) true
    else if (ifNoReactiveConnectionFrom(connection.srcPort.fmu, connection.trgPort.fmu)) true
    else false
  }

  lazy val connectionEdgesOptimized: Set[Edge[Node]] = {
    scenario.connections.filter(ifReactiveNoDelayedConnection).map(c => Edge[Node](GetOptimizedNodes(c.srcPort.fmu),
      if (isReactive(c.trgPort)) SetOptimizedNodesReactive(c.trgPort.fmu)
      else SetOptimizedNodesDelayed(c.trgPort.fmu))).toSet
  }

  lazy val initialEdges: Set[Edge[Node]] = removeEdges(connectionEdges ++ feedthroughInit)

  lazy val stepEdges: Set[Edge[Node]] =
    removeEdges(connectionEdges ++ feedthrough ++ doStepEdges ++ StepFindingEdges)

  lazy val initialEdgesOptimized: Set[Edge[Node]] = removeEdges(connectionEdgesOptimizedInit ++ feedthroughOptimizedInit)

  lazy val stepEdgesOptimized: Set[Edge[Node]] = removeEdges(connectionEdgesOptimized ++ feedthroughOptimized ++ doStepEdgesOptimized ++ StepFindingEdges)

  private def removeEdges(edges: Set[Edge[Node]]): Set[Edge[Node]] = {
    val tarjanGraph = new TarjanGraph[Node](edges)
    GraphUtil.removeTransitiveEdges(edges, tarjanGraph.tarjanCycle.toSet)
  }
}
