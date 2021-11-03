package synthesizer

import core.{AlgebraicLoop, AlgebraicLoopInit, CosimStepInstruction, FmuModel, InitializationInstruction, InputPortModel, PortRef, ScenarioModel}
import synthesizer.LoopStrategy.{LoopStrategy, maximum}

import scala.collection.mutable


class SynthesizerSimple(scenarioModel: ScenarioModel, chosenStrategy: LoopStrategy = maximum) extends SynthesizerBase {
  lazy val Configurations: Map[String, GraphBuilder] = {
    if (isAdaptive)
      scenarioModel.config.configurations.values.map(configuration => {
        val affectedInputs = configuration.inputs.groupBy(f => f._1.fmu)
        val fmus =
          scenarioModel.fmus.map(fmu => {
            if (affectedInputs.keySet.contains(fmu._1)) {
              val fmuModel = fmu._2
              val fmuInputs = affectedInputs(fmu._1)
              val inputs: Map[String, InputPortModel] =
                fmuModel.inputs.filter(input => fmuInputs.keySet.contains(PortRef(fmu._1, input._1)))
                  .map(input => (input._1, fmuInputs(PortRef(fmu._1, input._1))))
                  .++(fmuModel.inputs.filterNot(input => fmuInputs.keySet.contains(PortRef(fmu._1, input._1))))

              (fmu._1, FmuModel(inputs, fmuModel.outputs, fmuModel.canRejectStep, fmuModel.path))
            } else
              fmu
          })
        val scenario = ScenarioModel(fmus, scenarioModel.config, configuration.connections, scenarioModel.maxPossibleStepSize)

        (configuration.cosimStep, new GraphBuilder(scenario))
      }).toMap
    else Map("conf1" -> new GraphBuilder(scenarioModel))
  }

  val graphBuilder: GraphBuilder = Configurations.values.head
  val FMUsStepped = new mutable.HashSet[String]()
  val FMUsSaved = new mutable.HashSet[String]()
  val FMUsMayRejectStepped: mutable.HashSet[String] = new mutable.HashSet[String]()
  val StepEdges = Configurations.map(i => (i._1, i._2.stepEdges))
  val InitEdge = graphBuilder.initialEdges
  val strategy: LoopStrategy = chosenStrategy
  lazy val isAdaptive: Boolean = scenarioModel.config.configurableInputs.nonEmpty


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

  def formatFeedthroughLoop(sccNodes: List[Node], edges: Set[Edge[Node]], coSimAlgorithm: CoSimAlgorithm, isNested: Boolean): CoSimAlgorithm = {
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
    assert(!tarjanGraph.hasCycle, "The graph does still contain cycles")

    var algorithm = CoSimAlgorithm(coSimAlgorithm.steppedFMUs, coSimAlgorithm.savedFMUs, List.empty)
    val nodes =  tarjanGraph.topologicalSCC.flatten
    nodes.foreach(n => {
      algorithm = formatStepInstruction(n, algorithm)
    })

    val instructions = algorithm.intructions.filter(IsLoopInstruction)
    CoSimAlgorithm(algorithm.steppedFMUs, algorithm.savedFMUs, coSimAlgorithm.intructions.:+(AlgebraicLoop(gets.map(_.port), instructions, List.empty)))
  }

  def isTentative(action: Node, edges: Predef.Set[Edge[Node]]): Boolean = {
    if (action.isInstanceOf[GetNode]) return true
    if (!action.isInstanceOf[SetNode]) false else edges.exists(e => e.trgNode == action && e.srcNode.isInstanceOf[GetNode])
  }

  def formatReactiveLoop(scc: List[Node], edges: Predef.Set[Edge[Node]], coSimAlgorithm: CoSimAlgorithm, isNested: Boolean): CoSimAlgorithm = {
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
    assert(!tarjanGraph.hasCycle, "The graph does still contain cycles")

    val defaultAlgorithm = CoSimAlgorithm(coSimAlgorithm.steppedFMUs, coSimAlgorithm.savedFMUs, List.empty)
    var algorithm = defaultAlgorithm
    val nodes =  tarjanGraph.topologicalSCC.flatten
      nodes.foreach(n => {
        algorithm = formatStepInstruction(n, algorithm, isTentative(n, reducedEdges))
      })

    val instructions = algorithm.intructions.filter(IsLoopInstruction)
    val saves = if (isNested) defaultAlgorithm else createSaves(FMUs, defaultAlgorithm)
    val restores = createRestores(FMUs,defaultAlgorithm)
    CoSimAlgorithm(saves.steppedFMUs, saves.savedFMUs, coSimAlgorithm.intructions ++ saves.intructions.:+(AlgebraicLoop(gets.map(_.port), instructions, restores.intructions)))
  }

}


