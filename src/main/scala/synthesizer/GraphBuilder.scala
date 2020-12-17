package synthesizer

import core.Reactivity.{delayed, reactive}
import core.{FmuModel, InputPortConfig, PortRef, ScenarioModel}

sealed abstract class Node

case class DoStepNode(name: String) extends Node

case class GetNode(port: PortRef) extends Node

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


  private def feedthroughInit: Map[GetNode, Set[SetNode]] =
    scenario.fmus.flatMap(f => {
      f._2.outputs.map(o => (GetNodes(f._1).find(_ == GetNode(PortRef(f._1, o._1))).get,
        o._2.dependenciesInit.map(i => (SetNodes(f._1).find(_ == SetNode(PortRef(f._1, i))).get)).toSet))
    })

  private def feedthrough: Map[GetNode, Set[SetNode]] =
    scenario.fmus.flatMap(f => {
      f._2.outputs.map(o => (GetNodes(f._1).find(_ == GetNode(PortRef(f._1, o._1))).get,
        o._2.dependencies.map(i => (SetNodes(f._1).find(_ == SetNode(PortRef(f._1, i))).get)).toSet))
    })

  private def connectionEdges: Set[Edge[Node]] = {
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
      var FMUsThatShouldBeAdded = reactiveConnections.filter(o => mayReject.contains(o.trgPort.fmu) && normalFMUs.contains(o.srcPort.fmu)).map(o => o.srcPort.fmu)
      //These should be added to the set tha that May reject
      mayReject ++= FMUsThatShouldBeAdded
      normalFMUs --= FMUsThatShouldBeAdded

      conv = FMUsThatShouldBeAdded.isEmpty
    }
    //FMUs connected reactively that both may reject a step should be connected
    val artificalEdges = reactiveConnections.filter(o => mayReject.contains(o.trgPort.fmu) && mayReject.contains(o.srcPort.fmu)).map(o => Edge[Node](DoStepNode(o.trgPort.fmu), DoStepNode(o.srcPort.fmu))).toSet
    artificalEdges
  }

  def saveRestoreEdges(mayReject:Set[String]): Set[Edge[Node]] = {
    val SaveNodes: Set[SaveNode] = mayReject.map(SaveNode)
    val RestoreNodes: Set[RestoreNode] = mayReject.map(RestoreNode)
    val saveToStep = SaveNodes.map(save => Edge[Node](save, stepNodes.find(_ == DoStepNode(save.name)).get))
    val saveToSet: Set[Edge[Node]] = SaveNodes.flatMap(save => SetNodes(save.name).map(i => Edge[Node](save, i)))
    val saveToGet: Set[Edge[Node]] = SaveNodes.flatMap(save => GetNodes(save.name).map(i => Edge[Node](save, i)))
    val saveToRestore = SaveNodes.map(save => Edge[Node](save, RestoreNodes.find(_ == RestoreNode(save.name)).get))
    val stepToRestore = RestoreNodes.map(restore => Edge[Node](stepNodes.find(_ == DoStepNode(restore.name)).get, restore))
    saveToGet ++ saveToSet ++ saveToStep ++ saveToRestore ++ stepToRestore
  }

  lazy val initialEdges: Set[Edge[Node]] = connectionEdges ++ feedthroughInit.flatMap(f => f._2.map(i => Edge[Node](i, f._1))).toSet

  lazy val stepEdges: Set[Edge[Node]] = {
    connectionEdges ++ feedthrough.flatMap(f => f._2.map(i => Edge[Node](i, f._1))).toSet ++ doStepEdges ++ StepFindingEdges
  }

}

