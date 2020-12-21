package synthesizer

import core.Reactivity.{delayed, reactive}
import core.{FmuModel, InputPortConfig, PortRef, ScenarioModel}

sealed abstract class Node

case class DoStepNode(name: String) extends Node

case class GetNode(port: PortRef) extends Node

case class SetOptimizedNode(ports: Set[PortRef]) extends Node

case class GetOptimizedNode(ports: Set[PortRef]) extends Node

case class SetNode(port: PortRef) extends Node

case class RestoreNode(name: String) extends Node

case class SaveNode(name: String) extends Node

case class Edge[Node](srcNode: Node, trgNode: Node)

class GraphBuilder(scenario: ScenarioModel) {
  val stepNodes: Set[DoStepNode] = scenario.fmus.map(f => DoStepNode(f._1)).toSet
  val GetNodes: Map[String, Set[GetNode]] = scenario.fmus.map(f => (f._1, f._2.outputs.map(o => GetNode(PortRef(f._1, o._1))).toSet))
  val SetNodesReactive: Map[String, Set[SetNode]] = scenario.fmus.map(f => (f._1, f._2.inputs.filter(i => i._2.reactivity == reactive).map(i => SetNode(PortRef(f._1, i._1))).toSet))
  val SetNodesDelayed: Map[String, Set[SetNode]] = scenario.fmus.map(f => (f._1, f._2.inputs.filter(i => i._2.reactivity != reactive).map(i => SetNode(PortRef(f._1, i._1))).toSet))
  val SetNodes = scenario.fmus.map(f => (f._1, f._2.inputs.map(i => SetNode(PortRef(f._1, i._1))).toSet))
  val SetOptimizedNodesReactive: Map[String, SetOptimizedNode] = SetNodesReactive.map(node => (node._1, SetOptimizedNode(node._2.map(_.port))))
  val SetOptimizedNodesDelayed: Map[String, SetOptimizedNode] = SetNodesDelayed.map(node => (node._1, SetOptimizedNode(node._2.map(_.port))))
  val GetOptimizedNodes: Map[String, GetOptimizedNode] = GetNodes.map(node => (node._1, GetOptimizedNode(node._2.map(_.port))))
  val SetOptimizedNodes: Map[String, SetOptimizedNode] = scenario.fmus.map(f => (f._1, SetOptimizedNode(f._2.inputs.map(i => (PortRef(f._1, i._1))).toSet)))

  private def feedthroughInit: Set[Edge[Node]] =
    scenario.fmus.flatMap(f => {
      f._2.outputs.map(o => (GetNodes(f._1).find(_ == GetNode(PortRef(f._1, o._1))).get,
        o._2.dependenciesInit.map(i => (SetNodes(f._1).find(_ == SetNode(PortRef(f._1, i))).get)).toSet))
    }).flatMap(f => f._2.map(i => Edge[Node](i, f._1))).toSet

  private def feedthrough: Set[Edge[Node]] =
    scenario.fmus.flatMap(f => {
      f._2.outputs.map(o => (GetNodes(f._1).find(_ == GetNode(PortRef(f._1, o._1))).get,
        o._2.dependencies.map(i => (SetNodes(f._1).find(_ == SetNode(PortRef(f._1, i))).get)).toSet))
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
    scenario.connections.map(c => Edge[Node](GetNodes.values.flatten.find(_ == GetNode(c.srcPort)).get, SetNodes.values.flatten.find(_ == SetNode(c.trgPort)).get)).toSet
  }

  private def doStepEdges: Set[Edge[Node]] = {
    val stepToGet = stepNodes.flatMap(step => GetNodes(step.name).map(o => Edge[Node](step, o)))
    val reactiveToStep = stepNodes.flatMap(step => SetNodesReactive(step.name).map(i => Edge[Node](i, step)))
    val stepToDelayed = stepNodes.flatMap(step => SetNodesDelayed(step.name).map(i => Edge[Node](step, i)))
    stepToGet ++ reactiveToStep ++ stepToDelayed
  }

  private def StepFindingEdges: Set[Edge[Node]] = {
    var mayReject = scenario.fmus.filter(o => o._2.canRejectStep).keys.toSet
    var normalFMUs = scenario.fmus.filter(fmu => !mayReject.contains(fmu._1)).keys.toSet
    val reactiveInputs = scenario.fmus.map(fmu => (fmu._1, fmu._2.inputs.filter(i => i._2.reactivity == reactive).keys.toSet))
    val reactiveConnections = scenario.connections.filter(c => reactiveInputs(c.trgPort.fmu).contains(c.trgPort.port))
    var conv = false
    while (!conv) {
      //FMUs connected to an FMU that may reject a step
      var fmusToAdd = reactiveConnections.filter(o => mayReject.contains(o.trgPort.fmu) && normalFMUs.contains(o.srcPort.fmu)).map(o => o.srcPort.fmu)
      //These should be added to the set tha that May reject
      mayReject ++= fmusToAdd
      normalFMUs --= fmusToAdd
      conv = fmusToAdd.isEmpty
    }
    //FMUs connected reactively that both may reject a step should be connected
    reactiveConnections.filter(o => mayReject.contains(o.trgPort.fmu) && mayReject.contains(o.srcPort.fmu))
      .map(o => Edge[Node](DoStepNode(o.trgPort.fmu), DoStepNode(o.srcPort.fmu))).toSet
  }


  private def doStepEdgesOptimized: Set[Edge[Node]] = {
    val stepToGet = stepNodes.map(step => Edge[Node](step, GetOptimizedNodes(step.name)))
    val reactiveToStep = stepNodes.map(step => Edge[Node](SetOptimizedNodesReactive(step.name), step))
    val stepToDelayed = stepNodes.map(step => Edge[Node](step,SetOptimizedNodesDelayed(step.name)))
    stepToGet ++ reactiveToStep ++ stepToDelayed
  }

  def isReactive(port: PortRef): Boolean = {
    SetNodesReactive.values.exists(o => o.map(o => o.port).contains(port))
  }

  lazy val connectionEdgesOptimizedInit: Set[Edge[Node]] = {
    scenario.connections.map(c => Edge[Node](GetOptimizedNodes(c.srcPort.fmu), SetOptimizedNodes(c.trgPort.fmu))).toSet
  }

  lazy val connectionEdgesOptimized: Set[Edge[Node]] = {
    scenario.connections.map(c => Edge[Node](GetOptimizedNodes(c.srcPort.fmu),
      if(isReactive(c.trgPort)) SetOptimizedNodesReactive(c.trgPort.fmu)
      else SetOptimizedNodesDelayed(c.trgPort.fmu))).toSet
  }

  lazy val initialEdges: Set[Edge[Node]] = connectionEdges ++ feedthroughInit.toSet

  lazy val stepEdges: Set[Edge[Node]] = connectionEdges ++ feedthrough ++ doStepEdges ++ StepFindingEdges

  lazy val initialEdgesOptimized: Set[Edge[Node]] = connectionEdgesOptimizedInit ++ feedthroughOptimizedInit

  lazy val stepEdgesOptimized: Set[Edge[Node]] = {
    connectionEdgesOptimized ++ feedthroughOptimized ++ doStepEdgesOptimized ++ StepFindingEdges
  }

}
