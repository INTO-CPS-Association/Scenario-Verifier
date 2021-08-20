package synthesizer

import core.{AlgebraicLoop, AlgebraicLoopInit, CosimStepInstruction, DefaultStepSize, Get, GetTentative, InitGet, InitSet, InitializationInstruction, PortRef, RestoreState, SaveState, SetTentative, Step, StepLoop}
import org.apache.logging.log4j.scala.Logging
import org.graalvm.compiler.graph.Edges
import synthesizer.LoopStrategy.{LoopStrategy, Value}

import scala.annotation.tailrec
import scala.collection.mutable

object LoopStrategy extends Enumeration {
  type LoopStrategy = Value
  val minimum, maximum = Value
}

trait SCCType {
  def nodes: List[Node]
}

case class ReactiveLoop(nodes: List[Node]) extends SCCType

case class FeedthroughLoop(nodes: List[Node]) extends SCCType

case class StepLoopNodes(nodes: List[Node]) extends SCCType

case class SimpleAction(nodes: List[Node]) extends SCCType

trait SynthesizerBase extends Logging {
  def FMUsStepped: mutable.HashSet[String]
  def FMUsSaved: mutable.HashSet[String]
  def FMUsMayRejectStepped: mutable.HashSet[String]
  def StepEdges: Map[String, Set[Edge[Node]]]
  def InitEdge: Set[Edge[Node]]
  def strategy: LoopStrategy
  def isAdaptive : Boolean

  def checkSCC(scc: List[Node], edges: Set[Edge[Node]]): SCCType = {
    if (scc.size == 1)
      SimpleAction(scc)
    else if (isStepLoop(scc, edges)) {
      StepLoopNodes(scc)
    } else if (scc.count(IsStepNode) > 0)
      ReactiveLoop(scc)
    else
      FeedthroughLoop(scc)
  }

  def formatReactiveLoop(nodes: List[Node], edges: Set[Edge[Node]], isNested: Boolean = false): List[CosimStepInstruction]

  def formatInitLoop(nodes: List[Node]): InitializationInstruction

  def onlyReactiveConnections(srcFMU: String, trgFMU: String): Boolean

  def formatFeedthroughLoop(sccNodes: List[Node], edges: Set[Edge[Node]], isNested: Boolean = false): List[CosimStepInstruction]


  @tailrec
  private final def removeOneEdge(edgesInSCC: List[Edge[Node]], value: Set[Edge[Node]]): Set[Edge[Node]] = edgesInSCC match {
    case ::(head, next) => {
      if (!value.exists(i => i.trgNode.fmuName == head.srcNode.fmuName && i.srcNode.fmuName == head.trgNode.fmuName))
        removeOneEdge(next, value + head)
      else
        removeOneEdge(next, value)
    }
    case Nil => value
  }

  def filterEdges(edges: Set[Edge[Node]], scc: List[Node], isReactiveStepLoop: Boolean): Set[Edge[Node]] = {
    val edgesInSCC = getEdgesInSCC(edges, scc)
    if (isReactiveStepLoop)
      edgesInSCC.filterNot(o => IsStepNode(o.srcNode) && IsStepNode(o.trgNode))
    else {
      //There is two edges between all doStepNodes - one of them should be preserved
      removeOneEdge(edgesInSCC.toList, Set.empty[Edge[Node]])
    }
  }

  def formatStepLoop(scc: List[Node], edge: Set[Edge[Node]]): List[CosimStepInstruction] = {
    val FMUs = scc.filter(IsStepNode).map { case DoStepNode(name) => name }.toSet

    val emptyEdges = scc.map(i => Edge[Node](EmptyNode("Empty"),i)).toSet
    val edges = getEdgesInSCC(edge, scc).filterNot(o => IsStepNode(o.srcNode) && IsStepNode(o.trgNode)) ++ emptyEdges

    val tarjanGraph = new TarjanGraph[Node](edges)

    val instructions = if (!tarjanGraph.hasCycle)
      tarjanGraph.topologicalSCC.flatMap(o => formatStepInstruction(o.head))
    else formatReactiveLoop(tarjanGraph.topologicalSCC.flatten, edges, true)

    val saves = createSaves(FMUs)
    val restores = createRestores(FMUs)
    saves.:+(core.StepLoop(FMUs.toList, instructions, restores))
  }

  def removeMinimumNumberOfEdges(fmus: List[String], edgesInSCC: Set[Edge[Node]]): Set[Edge[Node]] = {
    val edges = edgesInSCC.filterNot(i => i.srcNode.fmuName != i.trgNode.fmuName && i.trgNode.fmuName == fmus.head)
    val tarjanGraph = new TarjanGraph[Node](edges)
    if (tarjanGraph.hasCycle) return removeMinimumNumberOfEdges(fmus.tail, edges) else edges
  }


  @tailrec
  final def handleSCC(SCCs: List[List[Node]], edges: Set[Edge[Node]], instructions: List[CosimStepInstruction]): List[CosimStepInstruction] = {
    SCCs match {
      case ::(scc, next) => checkSCC(scc, edges) match {
        case FeedthroughLoop(nodes) => handleSCC(next, edges, instructions ++ formatFeedthroughLoop(nodes, edges , false))
        case ReactiveLoop(nodes) => handleSCC(next, edges, instructions ++ formatReactiveLoop(nodes, edges, false))
        case StepLoopNodes(nodes) => handleSCC(next, edges, instructions ++ formatStepLoop(nodes, edges))
        case SimpleAction(node) => handleSCC(next, edges, instructions ++ formatStepInstruction(node.head, false))
        case _ => throw new UnsupportedOperationException("Unknown SCC in Graph")
      }
      case Nil => instructions
    }
  }

  def synthesizeInitialization(): List[InitializationInstruction] = {
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](InitEdge)
    val SCCs = tarjanGraph.topologicalSCC
    SCCs.flatMap(scc => {
      if (scc.size == 1) formatInitialInstruction(scc.head)
      else List(formatInitLoop(scc))
    })
  }

  def synthesizeStep(): Map[String, List[CosimStepInstruction]] = {
    StepEdges.map(keyValue => {
      val edges = keyValue._2
      val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edges)
      val SCCs = tarjanGraph.topologicalSCC
      FMUsSaved.empty
      FMUsStepped.empty
      (keyValue._1, handleSCC(SCCs, edges, List.empty[CosimStepInstruction]))
    })
  }

  def formatInitialInstruction(node: Node): List[InitializationInstruction] = {
    node match {
      case GetNode(_, port) => List(InitGet(port))
      case SetNode(_, port) => List(InitSet(port))
      case GetOptimizedNode(fmu, ports) => ports.flatMap(o => formatInitialInstruction(GetNode(fmu, o))).toList
      case SetOptimizedNode(fmu, ports) => ports.flatMap(o => formatInitialInstruction(SetNode(fmu, o))).toList
      case _ => throw new UnsupportedOperationException()
    }
  }

  def formatStepInstruction(node: Node, isTentative: Boolean = false): List[CosimStepInstruction] = {
    node match {
      case DoStepNode(name) => {
        FMUsStepped += name
        List(Step(name, DefaultStepSize()))
      }
      case GetNode(_, port) => if (FMUsStepped.contains(port.fmu) && isTentative) List(GetTentative(port)) else List(Get(port))
      case SetNode(_, port) => if (FMUsStepped.contains(port.fmu) && isTentative) List(SetTentative(port)) else List(core.Set(port))
      case GetOptimizedNode(fmu, ports) => ports.flatMap(o => formatStepInstruction(GetNode(fmu, o), isTentative)).toList
      case SetOptimizedNode(fmu, ports) => ports.flatMap(o => formatStepInstruction(SetNode(fmu, o), isTentative)).toList
      case RestoreNode(name) => List(RestoreState(name))
      case SaveNode(name) => {
        FMUsSaved += name
        List(SaveState(name))
      }
      case EmptyNode(_) => List.empty
    }
  }

  protected def createRestores(FMUs: Predef.Set[String]): List[CosimStepInstruction] = {
    FMUs.flatMap(o => formatStepInstruction(RestoreNode(o))).toList
  }

  protected def createSaves(FMUs: Predef.Set[String]): List[CosimStepInstruction] = {
    FMUs.diff(FMUsSaved).flatMap(o => formatStepInstruction(SaveNode(o))).toList
  }

  def isStepLoop(scc: List[Node], edges: Set[Edge[Node]]): Boolean = {
    getEdgesInSCC(edges, scc).count(o => IsStepNode(o.srcNode) && IsStepNode(o.trgNode)) > 0
  }

  protected def IsLoopInstruction(instruction: CosimStepInstruction): Boolean = instruction match {
    case RestoreState(_) => false
    case SaveState(_) => false
    case _ => true
  }

  def ComplexityOfScenario(edges: List[Edge[Node]]): Double = {
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edges)
    val SCCs = tarjanGraph.topologicalSCC
    SCCs.map(o => if (o.size == 1) 1 else Math.pow(o.size, 2)).sum
  }

  protected def IsStepNode(node: Node): Boolean = node match {
    case DoStepNode(_) => true
    case _ => false
  }

  protected def isGetNode(node: Node): Boolean = node match {
    case GetOptimizedNode(_, _) => true
    case GetNode(_, _) => true
    case _ => false
  }

  protected def getEdgesInSCC(edges: Set[Edge[Node]], scc: List[Node]) = {
    edges.filter(e => scc.contains(e.srcNode) && scc.contains(e.trgNode))
  }


}
