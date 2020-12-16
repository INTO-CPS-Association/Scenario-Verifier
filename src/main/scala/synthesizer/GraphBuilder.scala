package synthesizer

import core.Reactivity.{delayed, reactive}
import core.{FmuModel, InputPortConfig, PortRef, ScenarioModel}

sealed abstract class Node
case class DoStepNode(name:String, fmu: FmuModel) extends Node
case class GetNode(port: PortRef) extends Node
case class SetNode(port: PortRef) extends Node
case class RestoreNode(name:String, fmu: FmuModel) extends Node
case class SaveNode(name:String, fmu: FmuModel) extends Node

case class Edge[Node](srcNode:Node, trgNode:Node)

class GraphBuilder(scenario: ScenarioModel) {
  val stepNodes: Set[DoStepNode] = scenario.fmus.map(f => DoStepNode(f._1, f._2)).toSet
  val GetNodes: Map[String, Set[GetNode]] = scenario.fmus.map(f => (f._1, f._2.outputs.map(o => GetNode(PortRef(f._1, o._1))).toSet))
  val SetNodes: Map[String, Set[SetNode]] = scenario.fmus.map(f => (f._1, f._2.inputs.map(i => SetNode(PortRef(f._1, i._1))).toSet))
  val SaveNodes: Set[SaveNode] = scenario.fmus.filter(o => o._2.canRejectStep).map(f => SaveNode(f._1, f._2)).toSet
  val RestoreNodes: Set[RestoreNode] = scenario.fmus.filter(o => o._2.canRejectStep).map(f => RestoreNode(f._1, f._2)).toSet

  private def feedthroughInit: Map[GetNode, Set[SetNode]] =
    scenario.fmus.flatMap(f => {
      f._2.outputs.map(o => (GetNodes(f._1).find(_==GetNode(PortRef(f._1, o._1))).get,
        o._2.dependenciesInit.map(i => (SetNodes(f._1).find(_==SetNode(PortRef(f._1, i))).get)).toSet))
    })

  private def feedthrough: Map[GetNode, Set[SetNode]] =
    scenario.fmus.flatMap(f => {
      f._2.outputs.map(o => (GetNodes(f._1).find(_==GetNode(PortRef(f._1, o._1))).get,
        o._2.dependencies.map(i => (SetNodes(f._1).find(_==SetNode(PortRef(f._1, i))).get)).toSet))
    })



  private def connectionEdges: Set[Edge[Node]]= {
    scenario.connections.map(c => Edge[Node](GetNodes.values.flatten.find(_ == GetNode(c.srcPort)).get, SetNodes.values.flatten.find(_ == SetNode(c.trgPort)).get)).toSet
  }

  private def doStepEdges: Set[Edge[Node]]= {
    val stepToGet = stepNodes.flatMap(step => step.fmu.outputs.map(o => Edge[Node](step, GetNode(PortRef(step.name, o._1)))))
    val reactiveToStep = stepNodes.flatMap(step => step.fmu.inputs.filter(i => i._2.reactivity == reactive).map(i => Edge[Node](SetNode(PortRef(step.name, i._1)), step)))
    val stepToDelayed = stepNodes.flatMap(step => step.fmu.inputs.filter(i => i._2.reactivity == delayed).map(i => Edge[Node](step, SetNode(PortRef(step.name, i._1)))))
    stepToGet ++ reactiveToStep ++ stepToDelayed
  }

/*
  private def createRestoreAndSaveNodes: Set[Edge[Node]]= {
    val mayReject = scenario.fmus.filter(o => o._2.canRejectStep)
    val reactiveInputsInStepLoop = mayReject.flatMap(fmu => fmu._2.inputs.filter(i => i._2.reactivity == reactive))
    var initialSaveNodes: Set[SaveNode] = mayReject.map(f => SaveNode(f._1, f._2)).toSet
    var initialRestoreNodes: Set[RestoreNode] = mayReject.map(f => RestoreNode(f._1, f._2)).toSet

  }
*/
  private def saveEdges: Set[Edge[Node]]= {
    val saveToStep = SaveNodes.map(save =>  Edge[Node](save, stepNodes.find(_ == DoStepNode(save.name, save.fmu)).get))
    val saveToSet : Set[Edge[Node]]= SaveNodes.flatMap(save =>  SetNodes(save.name).map(i => Edge[Node](save, i)))
    val saveToGet : Set[Edge[Node]]= SaveNodes.flatMap(save =>  GetNodes(save.name).map(i => Edge[Node](save, i)))
    val saveToRestore = SaveNodes.map(save =>  Edge[Node](save, RestoreNodes.find(_ == RestoreNode(save.name, save.fmu)).get))
    saveToGet ++ saveToSet ++ saveToStep ++ saveToRestore
  }

  def initialEdges: Set[Edge[Node]] = connectionEdges ++ feedthroughInit.flatMap(f => f._2.map(i => Edge[Node](i, f._1))).toSet

  def stepEdges: Set[Edge[Node]] = {
    connectionEdges ++ feedthrough.flatMap(f => f._2.map(i => Edge[Node](i, f._1))).toSet ++ doStepEdges ++ saveEdges
  }

}

