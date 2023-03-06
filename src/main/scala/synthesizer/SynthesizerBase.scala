package synthesizer

import core.{CosimStepInstruction, DefaultStepSize, Get, GetTentative, InitGet, InitSet, InitializationInstruction, RestoreState, SaveState, SetTentative, Step}
import org.apache.logging.log4j.scala.Logging
import synthesizer.LoopStrategy.LoopStrategy

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

case class CoSimAlgorithm(steppedFMUs: Set[String], savedFMUs: Set[String], instructions: List[CosimStepInstruction])

trait SynthesizerBase extends Logging {
  def FMUsMayRejectStepped: mutable.HashSet[String]

  def StepEdges: Map[String, Set[Edge[Node]]]

  def InitEdge: Set[Edge[Node]]

  def strategy: LoopStrategy

  def isAdaptive: Boolean

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

  def formatReactiveLoop(nodes: List[Node], edges: Set[Edge[Node]], coSimAlgorithm: CoSimAlgorithm, isNested: Boolean = false): CoSimAlgorithm

  def formatInitLoop(nodes: List[Node]): InitializationInstruction

  def onlyReactiveConnections(srcFMU: String, trgFMU: String): Boolean

  def formatFeedthroughLoop(sccNodes: List[Node], edges: Set[Edge[Node]], coSimAlgorithm: CoSimAlgorithm, isNested: Boolean = false): CoSimAlgorithm


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

  def formatStepLoop(scc: List[Node], edge: Set[Edge[Node]], coSimAlgorithm: CoSimAlgorithm): CoSimAlgorithm = {
    val FMUs = scc.filter(IsStepNode).map {
      case DoStepNode(name) => name
      case _ => throw new UnsupportedOperationException("Unknown node type")
    }.toSet

    val emptyEdges = scc.map(i => Edge[Node](EmptyNode("Empty"), i)).toSet
    val edges = getEdgesInSCC(edge, scc).filterNot(o => IsStepNode(o.srcNode) && IsStepNode(o.trgNode)) ++ emptyEdges

    val tarjanGraph = new TarjanGraph[Node](edges)
    var algorithm = coSimAlgorithm

    val instructions = if (!tarjanGraph.hasCycle) {
      val nodes = tarjanGraph.topologicalSCC.flatten
      nodes.foreach(i => {
        algorithm = formatStepInstruction(i, algorithm)
      })
      algorithm
    } else formatReactiveLoop(tarjanGraph.topologicalSCC.flatten, edges, coSimAlgorithm, isNested = true)

    val savesAlgorithm = createSaves(FMUs, CoSimAlgorithm(algorithm.steppedFMUs, algorithm.savedFMUs, List.empty))
    val restoresAlgorithm = createRestores(FMUs, CoSimAlgorithm(savesAlgorithm.steppedFMUs, savesAlgorithm.savedFMUs, List.empty))
    CoSimAlgorithm(algorithm.steppedFMUs, savesAlgorithm.savedFMUs, savesAlgorithm.instructions.:+(core.StepLoop(FMUs.toList, instructions.instructions, restoresAlgorithm.instructions)))
  }

  def removeMinimumNumberOfEdges(fmus: List[String], edgesInSCC: Set[Edge[Node]]): Set[Edge[Node]] = {
    val edges = edgesInSCC.filterNot(i => i.srcNode.fmuName != i.trgNode.fmuName && i.trgNode.fmuName == fmus.head)
    val tarjanGraph = new TarjanGraph[Node](edges)
    if (tarjanGraph.hasCycle) return removeMinimumNumberOfEdges(fmus.tail, edges) else edges
  }


  @tailrec
  final def handleSCC(SCCs: List[List[Node]], edges: Set[Edge[Node]], coSimAlgorithm: CoSimAlgorithm): CoSimAlgorithm = {
    SCCs match {
      case ::(scc, next) => checkSCC(scc, edges) match {
        case FeedthroughLoop(nodes) => handleSCC(next, edges, formatFeedthroughLoop(nodes, edges, coSimAlgorithm))
        case ReactiveLoop(nodes) => handleSCC(next, edges, formatReactiveLoop(nodes, edges, coSimAlgorithm))
        case StepLoopNodes(nodes) => handleSCC(next, edges, formatStepLoop(nodes, edges, coSimAlgorithm))
        case SimpleAction(node) => handleSCC(next, edges, formatStepInstruction(node.head, coSimAlgorithm))
        case _ => throw new UnsupportedOperationException("Unknown SCC in Graph")
      }
      case Nil => coSimAlgorithm
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
      (keyValue._1, handleSCC(SCCs, edges, CoSimAlgorithm(Set.empty, Set.empty, List.empty[CosimStepInstruction])).instructions)
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

  def formatStepInstruction(node: Node, coSimAlgorithm: CoSimAlgorithm, isTentative: Boolean = false): CoSimAlgorithm = {
    node match {
      case DoStepNode(name) =>
        CoSimAlgorithm(coSimAlgorithm.steppedFMUs + name, coSimAlgorithm.savedFMUs, coSimAlgorithm.instructions.:+(Step(name, DefaultStepSize())))
      case GetNode(_, port) => {
        val instruction = if (coSimAlgorithm.steppedFMUs.contains(port.fmu) && isTentative) GetTentative(port) else Get(port)
        CoSimAlgorithm(coSimAlgorithm.steppedFMUs, coSimAlgorithm.savedFMUs, coSimAlgorithm.instructions.:+(instruction))
      }
      case SetNode(_, port) => {
        val instruction = if (coSimAlgorithm.steppedFMUs.contains(port.fmu) && isTentative) SetTentative(port) else core.Set(port)
        CoSimAlgorithm(coSimAlgorithm.steppedFMUs, coSimAlgorithm.savedFMUs, coSimAlgorithm.instructions.:+(instruction))
      }
      case GetOptimizedNode(fmu, ports) => ports.toList.map(o => formatStepInstruction(GetNode(fmu, o), coSimAlgorithm, isTentative)).last
      case SetOptimizedNode(fmu, ports) => ports.toList.map(o => formatStepInstruction(SetNode(fmu, o), coSimAlgorithm, isTentative)).last
      case RestoreNode(name) =>
        CoSimAlgorithm(coSimAlgorithm.steppedFMUs, coSimAlgorithm.savedFMUs, coSimAlgorithm.instructions.:+(RestoreState(name)))
      case SaveNode(name) =>
        CoSimAlgorithm(coSimAlgorithm.steppedFMUs, coSimAlgorithm.savedFMUs + name, coSimAlgorithm.instructions.:+(SaveState(name)))
      case EmptyNode(_) => coSimAlgorithm
    }
  }

  protected def createRestores(FMUs: Set[String], coSimAlgorithm: CoSimAlgorithm): CoSimAlgorithm = {
    var algorithm = coSimAlgorithm
    FMUs.foreach(o => algorithm = formatStepInstruction(RestoreNode(o), algorithm))
    algorithm
  }

  protected def createSaves(FMUs: Predef.Set[String], coSimAlgorithm: CoSimAlgorithm): CoSimAlgorithm = {
    var algorithm = coSimAlgorithm
    FMUs.diff(coSimAlgorithm.savedFMUs).foreach(o => {
      algorithm = formatStepInstruction(SaveNode(o), algorithm)
    })
    algorithm
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
    SCCs.map(o => if (o.size == 1) 1 else Math.pow(o.size.toDouble, 2)).sum
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

  protected def getEdgesInSCC(edges: Set[Edge[Node]], scc: List[Node]): Set[Edge[Node]] = {
    edges.filter(e => scc.contains(e.srcNode) && scc.contains(e.trgNode))
  }


}
