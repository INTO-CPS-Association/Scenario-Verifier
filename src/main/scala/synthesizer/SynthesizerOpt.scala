package synthesizer

import core._
import synthesizer.LoopStrategy.{LoopStrategy, maximum}

import scala.collection.immutable.HashSet
import scala.collection.mutable


class SynthesizerOpt(scenarioModel: ScenarioModel, strategy: LoopStrategy) extends SynthesizerBase {
  val graphBuilder: GraphBuilder = new GraphBuilder(scenarioModel, true)
  val FMUsStepped: mutable.HashSet[String] = new mutable.HashSet[String]()
  val FMUsSaved: mutable.HashSet[String] = new mutable.HashSet[String]()
  val FMUsMayRejectStepped: mutable.HashSet[String] = new mutable.HashSet[String]()
  val StepEdges: Predef.Set[Edge[Node]] = graphBuilder.stepEdgesOptimized
  val InitEdge: Predef.Set[Edge[Node]] = graphBuilder.initialEdgesOptimized

  def formatInitLoop(scc: List[Node]): InitializationInstruction = {
    val gets = graphBuilder.GetOptimizedNodes.values.filter(o => scc.contains(o)).toList
    var edgesInGraph = getEdgesInSCC(graphBuilder.initialEdgesOptimized, scc)
    if (strategy == maximum)
    //Remove all connections between FMUs
      edgesInGraph = edgesInGraph.filterNot(e => gets.contains(e.trgNode))
    else {
      //Remove all connections to a single FMU
      edgesInGraph = edgesInGraph.filter(e => e.srcNode == gets.head)
    }
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edgesInGraph)
    AlgebraicLoopInit(gets.flatMap(_.ports), tarjanGraph.topologicalSCC.flatten.flatMap(formatInitialInstruction))
  }

  override def onlyReactiveConnections(srcFMU: String, trgFMU: String): Boolean = {
    scenarioModel.connections.count(i => i.srcPort.fmu == srcFMU && i.trgPort.fmu == trgFMU) == graphBuilder.reactiveConnections.count(i => i.srcPort.fmu == srcFMU && i.trgPort.fmu == trgFMU)
  }

  def formatAlgebraicLoop(scc: List[Node], edges: Predef.Set[Edge[Node]], isNested: Boolean): List[CosimStepInstruction] = {
    val steps = graphBuilder.stepNodes.filter(o => scc.contains(o))
    val gets = graphBuilder.GetOptimizedNodes.values.filter(o => scc.contains(o)).toList
    val setsDelayed = graphBuilder.SetOptimizedNodesDelayed.values.filter(o => scc.contains(o)).toList
    val setsReactive = graphBuilder.SetOptimizedNodesReactive.values.filter(o => scc.contains(o)).toList
    var edgesInSCC = getEdgesInSCC(edges, scc)
    val FMUs = steps.map(o => o.fmuName)

    val reactiveGets = gets.filter(o => edgesInSCC.exists(edge => edge.srcNode == o && setsReactive.contains(edge.trgNode))).toSet

    //Add restore and save nodes:
    //ExpandReactiveSCC(FMUs, setsDelayed, setsReactive, reactiveGets.toList)

    if (strategy == maximum)
    //Remove all connections between FMUs
      edgesInSCC = edgesInSCC.filterNot(e => setsReactive.contains(e.trgNode) && gets.contains(e.srcNode))
    else {
      //Remove all connections to a single FMU
      edgesInSCC = edgesInSCC.filterNot(e => setsReactive.head == e.trgNode && gets.contains(e.srcNode))
    }

    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edgesInSCC)

    val instructions = tarjanGraph.topologicalSCC.flatten.flatMap(formatStepInstruction(_, true)).filter(IsLoopInstruction)

    val saves = createSaves(FMUs)
    val restores = createRestores(FMUs)
    saves.:+(AlgebraicLoop(reactiveGets.flatMap(_.ports).toList, instructions, restores))
  }

  private def ExpandReactiveSCC(FMUs: scala.collection.Set[String], setsDelayed: List[SetOptimizedNode], setsReactive: List[SetOptimizedNode], reactiveGets: List[GetOptimizedNode]): HashSet[Edge[Node]] = {
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
}


