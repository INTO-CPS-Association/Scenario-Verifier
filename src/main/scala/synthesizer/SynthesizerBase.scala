package synthesizer

import core.{CosimStepInstruction, DefaultStepSize, Get, GetTentative, InitGet, InitSet, InitializationInstruction, RestoreState, SaveState, SetTentative, Step, StepLoop}
import org.apache.logging.log4j.scala.Logging

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

  def StepEdges: Set[Edge[Node]]

  def InitEdge: Set[Edge[Node]]

  def checkSCC(scc: List[Node]): SCCType = {
    if (scc.size == 1)
      SimpleAction(scc)
    else if (isStepLoop(scc)) {
      StepLoopNodes(scc)
    } else if (scc.count(IsStepNode) > 0)
      ReactiveLoop(scc)
    else
      FeedthroughLoop(scc)
  }

  def formatAlgebraicLoop(nodes: List[Node], edges: Set[Edge[Node]], isNested: Boolean = false): List[CosimStepInstruction]

  def formatInitLoop(nodes: List[Node]): InitializationInstruction

  def onlyReactiveConnections(srcFMU: String, trgFMU: String): Boolean

  def removeOneEdge(edgesInSCC: List[Edge[Node]], value: Set[Edge[Node]]): Set[Edge[Node]] = {
    edgesInSCC match {
      case ::(head, next) => {
        if (!value.exists(i => i.trgNode.fmuName == head.srcNode.fmuName && i.srcNode.fmuName == head.trgNode.fmuName))
          removeOneEdge(next, value + head)
        else
          removeOneEdge(next, value)
      }
      case Nil => value
    }
  }

  def filterEdges(StepEdges: Set[Edge[Node]], scc: List[Node], isReactiveStepLoop: Boolean): Set[Edge[Node]] = {
    val edgesInSCC = getEdgesInSCC(StepEdges, scc)
    if (isReactiveStepLoop)
      edgesInSCC.filterNot(o => IsStepNode(o.srcNode) && IsStepNode(o.trgNode))
    else {
      //There is two edges between all doStepNodes - one of them should be preserved
      removeOneEdge(edgesInSCC.toList, Set.empty[Edge[Node]])
    }
  }

  def formatStepLoop(scc: List[Node]): List[CosimStepInstruction] = {
    //Remove Artificial edges between DoStep-edges
    val FMUs = scc.filter(IsStepNode).map { case DoStepNode(name) => name }.toSet

    val isReactiveStepLoop = scc.size > FMUs.size
    val edges = filterEdges(StepEdges, scc, isReactiveStepLoop)

    val tarjanGraph = new TarjanGraph[Node](edges)
    val instructions = if (!tarjanGraph.hasCycle)
      tarjanGraph.topologicalSCC.flatMap(o => formatStepInstruction(o.head))
    else formatAlgebraicLoop(tarjanGraph.topologicalSCC.flatten, edges, true)

    val saves = createSaves(FMUs)
    val restores = createRestores(FMUs)
    saves.:+(core.StepLoop(FMUs.toList, instructions, restores))
  }

  @tailrec
  final def handLoops(SCCs: List[List[Node]], instructions: List[CosimStepInstruction]): List[CosimStepInstruction] = {
    SCCs match {
      case ::(scc, next) => checkSCC(scc) match {
        case FeedthroughLoop(nodes) => throw new UnsupportedOperationException("Unsupported SCC in Graph")
        case ReactiveLoop(nodes) => handLoops(next, instructions ++ formatAlgebraicLoop(nodes, StepEdges, false))
        case StepLoopNodes(nodes) => handLoops(next, instructions ++ formatStepLoop(nodes))
        case SimpleAction(node) => handLoops(next, instructions ++ formatStepInstruction(node.head, false))
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

  def synthesizeStep(): List[CosimStepInstruction] = {
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](StepEdges)
    val SCCs = tarjanGraph.topologicalSCC
    handLoops(SCCs, List[CosimStepInstruction]())
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

  def formatStepInstruction(node: Node, isReactiveLoop: Boolean = false): List[CosimStepInstruction] = {
    node match {
      case DoStepNode(name) => {
        FMUsStepped += name
        List(Step(name, DefaultStepSize()))
      }
      case GetNode(_, port) => if (FMUsStepped.contains(port.fmu) && isReactiveLoop) List(GetTentative(port)) else List(Get(port))
      case SetNode(_, port) => if (FMUsStepped.contains(port.fmu) && isReactiveLoop) List(SetTentative(port)) else List(core.Set(port))
      case GetOptimizedNode(fmu, ports) => ports.flatMap(o => formatStepInstruction(GetNode(fmu, o), isReactiveLoop)).toList
      case SetOptimizedNode(fmu, ports) => ports.flatMap(o => formatStepInstruction(SetNode(fmu, o), isReactiveLoop)).toList
      case RestoreNode(name) => List(RestoreState(name))
      case SaveNode(name) => {
        FMUsSaved += name
        List(SaveState(name))
      }
    }
  }

  protected def createRestores(FMUs: Predef.Set[String]): List[CosimStepInstruction] = {
    FMUs.flatMap(o => formatStepInstruction(RestoreNode(o))).toList
  }

  protected def createSaves(FMUs: Predef.Set[String]): List[CosimStepInstruction] = {
    FMUs.diff(FMUsSaved).flatMap(o => formatStepInstruction(SaveNode(o))).toList
  }

  protected def isStepLoop(scc: List[Node]): Boolean = {
    getEdgesInSCC(StepEdges, scc).count(o => IsStepNode(o.srcNode) && IsStepNode(o.trgNode)) > 0
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

  protected def getEdgesInSCC(edges: Set[Edge[Node]], scc: List[Node]) = {
    edges.filter(e => scc.contains(e.srcNode) && scc.contains(e.trgNode))
  }
}
