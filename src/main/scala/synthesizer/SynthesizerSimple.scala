package synthesizer

import core._
import synthesizer.LoopStrategy.{LoopStrategy, maximum}
import scala.collection.immutable.HashSet
import scala.collection.mutable


class SynthesizerSimple(scenarioModel: ScenarioModel, strategy: LoopStrategy) extends SynthesizerBase {
  val graphBuilder: GraphBuilder = new GraphBuilder(scenarioModel)
  val FMUsStepped = new mutable.HashSet[String]()
  val FMUsSaved = new mutable.HashSet[String]()
  val FMUsMayRejectStepped: mutable.HashSet[String] = new mutable.HashSet[String]()
  val StepEdges = graphBuilder.stepEdges
  val InitEdge = graphBuilder.initialEdges

  def formatInitLoop(scc: List[Node]): InitializationInstruction = {
    val gets = graphBuilder.GetNodes.values.flatten.filter(o => scc.contains(o)).toList
    var edgesInGraph = getEdgesInSCC(InitEdge, scc)

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

  def formatAlgebraicLoop(scc: List[Node]): List[CosimStepInstruction] = {
    val steps = graphBuilder.stepNodes.filter(o => scc.contains(o))
    val gets = graphBuilder.GetNodes.values.flatten.filter(o => scc.contains(o)).toList
    val setsReactive = graphBuilder.SetNodesReactive.values.flatten.filter(o => scc.contains(o)).toList

    var edgesInSCC = getEdgesInSCC(StepEdges, scc)
    val FMUs = steps.map(o => o.name)

    val reactiveGets = gets.filter(o => (edgesInSCC.exists(edge => edge.srcNode == o && setsReactive.contains(edge.trgNode)))).toSet

    if (strategy == maximum)
    //Remove all connections between FMUs
      edgesInSCC = edgesInSCC.filterNot(e => setsReactive.contains(e.trgNode) && gets.contains(e.srcNode))
    else {
      //Remove all connections to a single FMU
      val reducedList = setsReactive.groupBy(o => o.port.fmu).toList.minBy(i => i._2.size)._2
      edgesInSCC = edgesInSCC.filterNot(e => reducedList.contains(e.trgNode) && gets.contains(e.srcNode))
    }

    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edgesInSCC)

    val instructions = tarjanGraph.topologicalSCC.flatten.flatMap(formatStepInstruction(_, true)).filter(IsLoopInstruction).toList
    val saves = createSaves(FMUs)
    val restores = createRestores(FMUs)
    saves.:+(AlgebraicLoop(reactiveGets.map(_.port).toList, instructions, restores))
  }
}


