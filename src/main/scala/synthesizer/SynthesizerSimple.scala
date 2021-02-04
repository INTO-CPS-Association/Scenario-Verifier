package synthesizer

import core.{AlgebraicLoop, AlgebraicLoopInit, CosimStepInstruction, InitializationInstruction, PortRef, ScenarioModel}
import synthesizer.LoopStrategy.{LoopStrategy, maximum}

import scala.collection.mutable


class SynthesizerSimple(scenarioModel: ScenarioModel, chosenStrategy: LoopStrategy = maximum) extends SynthesizerBase {
  val graphBuilder: GraphBuilder = new GraphBuilder(scenarioModel)
  val FMUsStepped = new mutable.HashSet[String]()
  val FMUsSaved = new mutable.HashSet[String]()
  val FMUsMayRejectStepped: mutable.HashSet[String] = new mutable.HashSet[String]()
  val StepEdges = graphBuilder.stepEdges
  val InitEdge = graphBuilder.initialEdges
  val strategy: LoopStrategy = chosenStrategy


  def formatInitLoop(scc: List[Node]): InitializationInstruction = {
    val gets = graphBuilder.GetNodes.values.flatten.filter(o => scc.contains(o)).toList
    val edgesInGraph = getEdgesInSCC(InitEdge, scc)

    val reducedEdges = if (strategy == maximum)
    //Remove all connections between FMUs
      edgesInGraph.filter(e => e.srcNode.fmuName == e.trgNode.fmuName)
    else {
      //Remove all connections to a single FMU
      val reducedList = gets.groupBy(o => o.port.fmu).head._2
      edgesInGraph.filter(e => reducedList.contains(e.trgNode))
    }
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](reducedEdges)

    AlgebraicLoopInit(gets.map(o => o.port), tarjanGraph.topologicalSCC.flatten.flatMap(formatInitialInstruction))
  }

  override def onlyReactiveConnections(srcFMU: String, trgFMU: String): Boolean = {
    scenarioModel.connections.count(i => i.srcPort.fmu == srcFMU && i.trgPort.fmu == trgFMU) == graphBuilder.reactiveConnections.count(i => i.srcPort.fmu == srcFMU && i.trgPort.fmu == trgFMU)
  }

  def formatFeedthroughLoop(sccNodes: List[Node], edges: Set[Edge[Node]], isNested: Boolean): List[CosimStepInstruction] = {
    //Remove Artificial edges between DoStep-edges
    val edgesInSCC = getEdgesInSCC(edges, sccNodes)
    val FMUs = sccNodes.distinctBy(i => i.fmuName).map(i => i.fmuName)
    val gets = graphBuilder.GetNodes.values.flatten.filter(o => sccNodes.contains(o)).toList

    val reducedEdges = strategy match {
      case synthesizer.LoopStrategy.maximum => {
        //Remove all connections between FMUs in Loop
        edgesInSCC.filter(e => e.srcNode.fmuName == e.trgNode.fmuName)
      }
      case synthesizer.LoopStrategy.minimum => {
        //Remove enough edges connections to one FMU
        removeMinimumNumberOfEdges(FMUs, edgesInSCC)
      }
    }

    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](reducedEdges)
    assert(!tarjanGraph.hasCycle, "Graph has after edges have been removed cycles")
    val instructions = tarjanGraph.topologicalSCC.flatten.flatMap(formatStepInstruction(_, false)).filter(IsLoopInstruction)
    List(AlgebraicLoop(gets.map(i => i.port),instructions, List.empty))
  }

  def isTentative(action: Node, edges: Predef.Set[Edge[Node]]): Boolean = {
    if(action.isInstanceOf[GetNode]) return true
    return if(!action.isInstanceOf[SetNode])
      false
    else{
      edges.exists(e => e.trgNode == action && e.srcNode.isInstanceOf[GetNode])
    }
  }

  def formatReactiveLoop(scc: List[Node], edges: Predef.Set[Edge[Node]], isNested: Boolean): List[CosimStepInstruction] = {
    val gets = graphBuilder.GetNodes.values.flatten.filter(o => scc.contains(o)).toList
    val edgesInSCC = getEdgesInSCC(edges, scc)
    val FMUs = scc.filter(_.isInstanceOf[DoStepNode]).map(_.fmuName).toSet

    val reducedEdges = strategy match {
      case synthesizer.LoopStrategy.maximum => {
        //Remove all reactive connections
        edgesInSCC.filter(e => e.srcNode.fmuName == e.trgNode.fmuName)
      }
      case synthesizer.LoopStrategy.minimum => {
        //Remove enough edges connections to one FMU
        removeMinimumNumberOfEdges(FMUs.toList, edgesInSCC)
      }
    }

    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](reducedEdges)
    assert(!tarjanGraph.hasCycle, "Graph has after edges have been removed cycles")

    val instructions = tarjanGraph.topologicalSCC.flatten.flatMap(i => formatStepInstruction(i, isTentative(i, reducedEdges))).filter(IsLoopInstruction)

    val saves = if(isNested) List.empty[CosimStepInstruction] else createSaves(FMUs)
    val restores = createRestores(FMUs)
    saves.:+(AlgebraicLoop(gets.map(_.port), instructions, restores))
  }

}


