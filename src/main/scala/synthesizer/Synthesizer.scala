package synthesizer

import core._
import synthesizer.LoopStrategy.{LoopStrategy, maximum}

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
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

case class StepLoop(nodes: List[Node]) extends SCCType

case class SimpleAction(nodes: List[Node]) extends SCCType


class Synthesizer(scenarioModel: ScenarioModel, strategy: LoopStrategy, optimized: Boolean = false) {
  val graphBuilder: GraphBuilder = new GraphBuilder(scenarioModel)
  val FMUsStepped: mutable.HashSet[String] = new mutable.HashSet[String]()
  val FMUsSaved: mutable.HashSet[String] = new mutable.HashSet[String]()
  val FMUsMayRejectStepped: mutable.HashSet[String] = new mutable.HashSet[String]()

  def formatInitLoop(scc: List[Node]): InitializationInstruction = {
    val gets = graphBuilder.GetNodes.values.flatten.filter(o => scc.contains(o)).toList
    var edgesInGraph = getEdgesInSCC(scc, false)
    if (strategy == maximum)
    //Remove all connections between FMUs
      edgesInGraph = edgesInGraph.filterNot(e => gets.contains(e.trgNode))
    else {
      //Remove all connections to a single FMU
      val reducedList = gets.groupBy(o => o.port.fmu).head._2
      edgesInGraph = edgesInGraph.filter(e => reducedList.contains(e.trgNode))
    }
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edgesInGraph)

    AlgebraicLoopInit(gets.map(o => o.port), tarjanGraph.topologicalSCC.flatten.flatMap(formatInitialInstruction))
  }

  def IsStepNode(node: Node): Boolean = node match {
    case DoStepNode(_) => true
    case _ => false
  }

  def IsLoopInstruction(instruction: CosimStepInstruction): Boolean = instruction match {
    case RestoreState(fmu) => false
    case SaveState(fmu) => false
    case _ => true
  }

  def formatStepLoop(scc: List[Node]): List[CosimStepInstruction] = {
    //Remove Artificial edges between DoStep-edges
    val edges = getEdgesInSCC(scc, true).filterNot(o => IsStepNode(o.srcNode) && IsStepNode(o.trgNode))
    val FMUs = scc.filter(IsStepNode).map { case DoStepNode(name) => name }.toSet

    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edges)
    //Cycles are not yet supported
    var instructions = List[CosimStepInstruction]()
    //assert(!tarjanGraph.hasCycle)

    if (tarjanGraph.hasCycle) {
      tarjanGraph.topologicalSCC.foreach(o =>
        if (o.size == 1)
          instructions ++= formatStepInstruction(scc.head)
        else
          instructions ++= formatAlgebraicLoop(o))
    } else instructions = tarjanGraph.topologicalSCC.flatten.flatMap(i => formatStepInstruction(i))
    val saves = createSaves(FMUs)
    val restores = createRestores(FMUs)
    saves.:+(core.StepLoop(FMUs.toList, instructions, restores))
  }

  def formatAlgebraicLoop(scc: List[Node]): List[CosimStepInstruction] = {
    val steps = graphBuilder.stepNodes.filter(o => scc.contains(o))
    val gets = graphBuilder.GetNodes.values.flatten.filter(o => scc.contains(o)).toList
    val setsDelayed = graphBuilder.SetNodesDelayed.values.flatten.filter(o => scc.contains(o)).toList
    val setsReactive = graphBuilder.SetNodesReactive.values.flatten.filter(o => scc.contains(o)).toList

    var edgesInSCC = getEdgesInSCC(scc, true)
    val FMUs = steps.map(o => o.name)

    val reactiveGets = gets.filter(o => (edgesInSCC.exists(edge => edge.srcNode == o && setsReactive.contains(edge.trgNode)))).toSet

    //Add restore and save nodes:
    ExpandReactiveSCC(FMUs, setsDelayed, setsReactive, reactiveGets.toList)

    if (strategy == maximum)
    //Remove all connections between FMUs
      edgesInSCC = edgesInSCC.filterNot(e => setsReactive.contains(e.trgNode) && gets.contains(e.srcNode))
    else {
      //Remove all connections to a single FMU
      val reducedList = setsReactive.groupBy(o => o.port.fmu).head._2
      edgesInSCC = edgesInSCC.filterNot(e => reducedList.contains(e.trgNode) && gets.contains(e.srcNode))
    }

    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edgesInSCC)

    val instructions = tarjanGraph.topologicalSCC.flatten.flatMap(formatStepInstruction(_, true)).filter(IsLoopInstruction).toList
    val saves = createSaves(FMUs)
    val restores = createRestores(FMUs)
    saves.:+(AlgebraicLoop(reactiveGets.map(_.port).toList, instructions, restores))
  }

  private def ExpandReactiveSCC(FMUs: scala.collection.Set[String], setsDelayed: List[SetNode], setsReactive: List[SetNode], reactiveGets: List[GetNode]): HashSet[Edge[Node]] = {
    var edgesInSCC: HashSet[Edge[Node]] = HashSet.empty
    FMUs.foreach(fmu => {
      edgesInSCC += (Edge[Node](SaveNode(fmu), RestoreNode(fmu)))
      edgesInSCC += (Edge[Node](SaveNode(fmu), DoStepNode(fmu)))
      (setsDelayed ++ setsReactive).foreach(s => {
        edgesInSCC += (Edge[Node](SaveNode(fmu), s))
      })
      reactiveGets.foreach(g => {
        edgesInSCC += (Edge[Node](g, RestoreNode(fmu)))
      })
    })
    edgesInSCC
  }

  private def getEdgesInSCC(scc: List[Node], isStep: Boolean) = {
    if (isStep)
      graphBuilder.stepEdges.filter(e => scc.contains(e.srcNode) && scc.contains(e.trgNode))
    else
      graphBuilder.initialEdges.filter(e => scc.contains(e.srcNode) && scc.contains(e.trgNode))
  }

  def synthesizeInitialization(): List[InitializationInstruction] = {
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](graphBuilder.initialEdges)
    val SCCs = tarjanGraph.topologicalSCC

    SCCs.flatMap(scc => {
      if (scc.size == 1) {
        formatInitialInstruction(scc.head)
      } else {
        List(formatInitLoop(scc))
      }
    })
  }

  def isStepLoop(scc: List[Node]): Boolean = {
    val edges = getEdgesInSCC(scc, true)
    edges.count(o => IsStepNode(o.srcNode) && IsStepNode(o.trgNode)) > 0
  }

  def checkSCC(scc: List[Node]): SCCType = {
    if (scc.size == 1)
      SimpleAction(scc)
    else if (isStepLoop(scc)) {
      StepLoop(scc)
    } else if (scc.count(IsStepNode) > 0)
      ReactiveLoop(scc)
    else
      FeedthroughLoop(scc)
  }

  @tailrec
  final def handLoops(SCCs: List[List[Node]], instructions: List[CosimStepInstruction]): List[CosimStepInstruction] = {
    SCCs match {
      case ::(scc, next) => checkSCC(scc) match {
        case FeedthroughLoop(nodes) => throw new UnsupportedOperationException("Unsupported SCC in Graph")
        case ReactiveLoop(nodes) => handLoops(next, instructions ++ formatAlgebraicLoop(nodes))
        case StepLoop(nodes) => handLoops(next, instructions ++ formatStepLoop(nodes))
        case SimpleAction(node) => handLoops(next, instructions ++ (formatStepInstruction(node.head, false)))
        case _ => throw new UnsupportedOperationException("Unknown SCC in Graph")
      }
      case Nil => instructions
    }
  }

  def synthesizeStep(): List[CosimStepInstruction] = {
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](graphBuilder.stepEdges)
    val SCCs = tarjanGraph.topologicalSCC
    handLoops(SCCs, List[CosimStepInstruction]())
  }

  def formatInitialInstruction(node: Node): List[InitializationInstruction] = {
    node match {
      case GetNode(port) => List(InitGet(port))
      case SetNode(port) => List(InitSet(port))
      case GetOptimizedNode(ports) => ports.flatMap(o => formatInitialInstruction(GetNode(o))).toList
      case SetOptimizedNode(ports) => ports.flatMap(o => formatInitialInstruction(SetNode(o))).toList
      case _ => throw new UnsupportedOperationException()
    }
  }

  def formatStepInstruction(node: Node, isReactiveLoop: Boolean = false): List[CosimStepInstruction] = {
    node match {
      case DoStepNode(name) => {
        FMUsStepped.add(name)
        List(Step(name, DefaultStepSize()))
      }
      case GetNode(port) => if (FMUsStepped.contains(port.fmu) && isReactiveLoop) List(GetTentative(port)) else List(Get(port))
      case SetNode(port) => if (FMUsStepped.contains(port.fmu) && isReactiveLoop) List(SetTentative(port)) else List(core.Set(port))
      case GetOptimizedNode(ports) => ports.flatMap(o => formatStepInstruction(GetNode(o), isReactiveLoop)).toList
      case SetOptimizedNode(ports) => ports.flatMap(o => formatStepInstruction(SetNode(o), isReactiveLoop)).toList
      case RestoreNode(name) => List(RestoreState(name))
      case SaveNode(name) => List(SaveState(name))
    }
  }

  def ComplexityOfScenario(): Double = {
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](graphBuilder.stepEdges)
    val SCCs = tarjanGraph.topologicalSCC
    SCCs.map(o => if (o.size == 1) 1 else Math.pow(o.size, 2)).sum
  }

  private def createRestores(FMUs: Predef.Set[String]) = {
    FMUs.flatMap(o => formatStepInstruction(RestoreNode(o))).toList
  }

  private def createSaves(FMUs: Predef.Set[String]) = {
    FMUs.diff(FMUsSaved).flatMap(o => formatStepInstruction(SaveNode(o))).toList
  }
}


