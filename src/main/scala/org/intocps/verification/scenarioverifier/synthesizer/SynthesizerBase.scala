package org.intocps.verification.scenarioverifier.synthesizer

import scala.annotation.tailrec
import scala.collection.mutable

import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier
import org.intocps.verification.scenarioverifier.core.CosimStepInstruction
import org.intocps.verification.scenarioverifier.core.Get
import org.intocps.verification.scenarioverifier.core.GetTentative
import org.intocps.verification.scenarioverifier.core.InitializationInstruction
import org.intocps.verification.scenarioverifier.core.PortRef
import org.intocps.verification.scenarioverifier.core.RestoreState
import org.intocps.verification.scenarioverifier.core.SaveState
import org.intocps.verification.scenarioverifier.core.SetTentative
import org.intocps.verification.scenarioverifier.core.StepLoop
import LoopStrategy.LoopStrategy

object LoopStrategy extends Enumeration {
  type LoopStrategy = Value
  val minimum, maximum = Value
}

trait SCCType {
  def nodes: List[Node]
}

final case class ReactiveLoop(nodes: List[StepInstructionNode]) extends SCCType

final case class FeedthroughLoop(nodes: List[StepInstructionNode]) extends SCCType

final case class StepLoopNodes(nodes: List[StepInstructionNode]) extends SCCType

final case class SimpleAction(nodes: List[StepInstructionNode]) extends SCCType

final case class CoSimAlgorithm(steppedFMUs: Set[String], savedFMUs: Set[String], instructions: List[CosimStepInstruction])

trait SynthesizerBase extends Logging {
  def FMUsMayRejectStepped: mutable.HashSet[String]

  def StepEdges: Map[String, Set[Edge[StepInstructionNode]]]

  def InitEdge: Set[Edge[InitializationInstructionNode]]

  def strategy: LoopStrategy

  def isAdaptive: Boolean

  private def sccType(scc: List[StepInstructionNode], edges: Set[Edge[StepInstructionNode]]): SCCType = {
    if (scc.size == 1) SimpleAction(scc)
    else if (isStepLoop(scc, edges)) StepLoopNodes(scc)
    else if (scc.count(_.isInstanceOf[DoStepNode]) > 0) ReactiveLoop(scc)
    else FeedthroughLoop(scc)
  }

  def formatInitLoop(nodes: List[InitializationInstructionNode]): InitializationInstruction

  def formatAlgebraicLoop(
      sccNodes: List[StepInstructionNode],
      edges: Set[Edge[StepInstructionNode]],
      algorithm: CoSimAlgorithm,
      isReactive: Boolean,
      isNested: Boolean): CoSimAlgorithm

  def onlyReactiveConnections(srcFMU: String, trgFMU: String): Boolean

  @tailrec
  private final def removeOneEdge(edgesInSCC: List[Edge[Node]], value: Set[Edge[Node]]): Set[Edge[Node]] =
    edgesInSCC match {
      case ::(head, next) =>
        if (!value.exists(i => i.trgNode.fmuName == head.srcNode.fmuName && i.srcNode.fmuName == head.trgNode.fmuName))
          removeOneEdge(next, value + head)
        else
          removeOneEdge(next, value)
      case Nil => value
    }

  private def formatStepLoop(
      scc: List[StepInstructionNode],
      edge: Set[Edge[StepInstructionNode]],
      coSimAlgorithm: CoSimAlgorithm): CoSimAlgorithm = {
    val FMUs = scc.map(_.fmuName).toSet
    val emptyEdges = scc.map(i => Edge[StepInstructionNode](EmptyNode(), i)).toSet
    val edges = edgesInSCC(edge, scc).filterNot(o => IsStepNode(o.srcNode) && IsStepNode(o.trgNode)) ++ emptyEdges

    val tarjanGraph = new TarjanGraph[StepInstructionNode](edges)
    val algorithm = if (!tarjanGraph.hasCycle) {
      tarjanGraph.topologicalSCC.flatten.foldLeft(coSimAlgorithm)((acc, node) => formatStepInstruction(node, acc))
    } else formatAlgebraicLoop(tarjanGraph.topologicalSCC.flatten, edges, coSimAlgorithm, isReactive = true, isNested = true)

    val savesAlgorithm = createSaves(FMUs, algorithm.copy(instructions = List.empty))
    val restoresAlgorithm = createRestores(FMUs, savesAlgorithm.copy(instructions = List.empty))
    val stepLoop = StepLoop(FMUs.toList, algorithm.instructions, restoresAlgorithm.instructions)
    val algo = savesAlgorithm.copy(instructions = savesAlgorithm.instructions.:+(stepLoop))
    algo
  }

  def removeMinimumNumberOfEdges[A <: Node](fmus: List[String], edgesInSCC: Set[Edge[A]]): Set[Edge[A]] = {
    val edges = edgesInSCC.filterNot(i => i.srcNode.fmuName != i.trgNode.fmuName && i.trgNode.fmuName == fmus.head)
    val tarjanGraph = new TarjanGraph[A](edges)
    if (tarjanGraph.hasCycle) removeMinimumNumberOfEdges(fmus.tail, edges) else edges
  }

  @tailrec
  private final def handleSCC(
      SCCs: List[List[StepInstructionNode]],
      edges: Set[Edge[StepInstructionNode]],
      coSimAlgorithm: CoSimAlgorithm): CoSimAlgorithm = {
    SCCs match {
      case ::(scc, next) =>
        sccType(scc, edges) match {
          case FeedthroughLoop(nodes) =>
            handleSCC(next, edges, formatAlgebraicLoop(nodes, edges, coSimAlgorithm, isReactive = false, isNested = false))
          case ReactiveLoop(nodes) =>
            handleSCC(next, edges, formatAlgebraicLoop(nodes, edges, coSimAlgorithm, isReactive = true, isNested = false))
          case StepLoopNodes(nodes) => handleSCC(next, edges, formatStepLoop(nodes, edges, coSimAlgorithm))
          case SimpleAction(node) => handleSCC(next, edges, formatStepInstruction(node.head, coSimAlgorithm))
          case _ => throw new UnsupportedOperationException("Unknown SCC in Graph")
        }
      case Nil => coSimAlgorithm
    }
  }

  def synthesizeInitialization(): List[InitializationInstruction] = {
    val tarjanGraph: TarjanGraph[InitializationInstructionNode] =
      new TarjanGraph[InitializationInstructionNode](InitEdge)
    tarjanGraph.topologicalSCC.flatMap(scc => {
      if (scc.size == 1) List(scc.head.formatInitInstruction)
      else List(formatInitLoop(scc))
    })
  }

  def synthesizeStep(): Map[String, List[CosimStepInstruction]] = {
    StepEdges.map(keyValue => {
      val edges = keyValue._2
      val tarjanGraph: TarjanGraph[StepInstructionNode] = new TarjanGraph[StepInstructionNode](edges)
      val SCCs = tarjanGraph.topologicalSCC
      (keyValue._1, handleSCC(SCCs, edges, CoSimAlgorithm(Set.empty, Set.empty, List.empty[CosimStepInstruction])).instructions)
    })
  }

  def formatStepInstruction(node: StepInstructionNode, coSimAlgorithm: CoSimAlgorithm, isTentative: Boolean = false): CoSimAlgorithm = {
    node match {
      case DoStepNode(name) =>
        coSimAlgorithm.copy(
          steppedFMUs = coSimAlgorithm.steppedFMUs + name,
          instructions = coSimAlgorithm.instructions.:+(node.formatStepInstruction))
      case GetNode(_, _) =>
        createPortInstruction(node.asInstanceOf[PortNode], coSimAlgorithm, isTentative, GetTentative, Get)
      case SetNode(_, _) =>
        createPortInstruction(node.asInstanceOf[PortNode], coSimAlgorithm, isTentative, SetTentative, scenarioverifier.core.Set)
      case SetTentativeNode(_, _) =>
        createPortInstruction(node.asInstanceOf[PortNode], coSimAlgorithm, isTentative, SetTentative, scenarioverifier.core.Set)
      case RestoreNode(_) =>
        require(coSimAlgorithm.savedFMUs.contains(node.fmuName), s"FMU ${node.fmuName} has not been saved before restoring")
        coSimAlgorithm.copy(instructions = coSimAlgorithm.instructions.:+(node.formatStepInstruction))
      case SaveNode(name) =>
        coSimAlgorithm.copy(
          savedFMUs = coSimAlgorithm.savedFMUs + name,
          instructions = coSimAlgorithm.instructions.:+(node.formatStepInstruction))
      case EmptyNode() => coSimAlgorithm
      case _ => throw new UnsupportedOperationException("Unknown Step Instruction")
    }
  }

  private def createPortInstruction(
      port: PortNode,
      algorithm: CoSimAlgorithm,
      isTentative: Boolean,
      tentativeAction: PortRef => CosimStepInstruction,
      action: PortRef => CosimStepInstruction): CoSimAlgorithm = {
    val instruction =
      if (algorithm.steppedFMUs.contains(port.fmuName) && isTentative)
        tentativeAction(port.port)
      else action(port.port)
    algorithm.copy(instructions = algorithm.instructions.:+(instruction))
  }

  protected def createFMUActions(
      FMUs: Set[String],
      coSimAlgorithm: CoSimAlgorithm,
      actionCreator: String => CosimStepInstruction): CoSimAlgorithm = {
    FMUs.foldLeft(coSimAlgorithm)((algorithm, fmu) => algorithm.copy(instructions = algorithm.instructions.:+(actionCreator(fmu))))
  }

  protected def createRestores(FMUs: Set[String], coSimAlgorithm: CoSimAlgorithm): CoSimAlgorithm = {
    FMUs.foldLeft(coSimAlgorithm)((algorithm, fmu) =>
      algorithm.copy(instructions = algorithm.instructions.:+(RestoreNode(fmu).formatStepInstruction)))
  }

  protected def createSaves(FMUs: Set[String], coSimAlgorithm: CoSimAlgorithm): CoSimAlgorithm = {
    var algorithm = coSimAlgorithm
    FMUs
      .diff(coSimAlgorithm.savedFMUs)
      .foreach(fmu => {
        algorithm = formatStepInstruction(SaveNode(fmu), algorithm)
      })
    algorithm
  }

  private def isStepLoop(scc: List[StepInstructionNode], edges: Set[Edge[StepInstructionNode]]): Boolean = {
    edgesInSCC(edges, scc).count(o => IsStepNode(o.srcNode) && IsStepNode(o.trgNode)) > 0
  }

  protected def IsLoopInstruction(instruction: CosimStepInstruction): Boolean = instruction match {
    case RestoreState(_) | SaveState(_) => false
    case _ => true
  }

  private def IsStepNode(node: StepInstructionNode): Boolean = node match {
    case DoStepNode(_) => true
    case _ => false
  }

  protected def edgesInSCC[A <: Node](edges: Set[Edge[A]], scc: List[A]): Set[Edge[A]] = {
    edges.filter(e => scc.contains(e.srcNode) && scc.contains(e.trgNode))
  }
}
