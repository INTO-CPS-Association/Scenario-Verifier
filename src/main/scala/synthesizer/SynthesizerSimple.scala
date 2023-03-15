package synthesizer

import core.{AlgebraicLoop, AlgebraicLoopInit, FmuModel, InitializationInstruction, InputPortModel, PortRef, ScenarioModel}
import synthesizer.LoopStrategy._
import scala.collection.mutable

class SynthesizerSimple(scenarioModel: ScenarioModel, override val strategy: LoopStrategy.LoopStrategy = maximum) extends SynthesizerBase {
  private lazy val Configurations: Map[String, GraphBuilder] = {
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

  private val graphBuilder: GraphBuilder = Configurations.values.head
  val FMUsMayRejectStepped: mutable.HashSet[String] = new mutable.HashSet[String]()
  val StepEdges: Map[String, Set[Edge[StepInstructionNode]]] = Configurations.map(i => (i._1, i._2.stepEdges))
  val InitEdge: Set[Edge[InitializationInstructionNode]] = graphBuilder.initialEdges
  lazy val isAdaptive: Boolean = scenarioModel.config.configurableInputs.nonEmpty


  override def onlyReactiveConnections(srcFMU: String, trgFMU: String): Boolean = {
    scenarioModel.connections.count(i => i.srcPort.fmu == srcFMU && i.trgPort.fmu == trgFMU) == graphBuilder.reactiveConnections.count(i => i.srcPort.fmu == srcFMU && i.trgPort.fmu == trgFMU)
  }


  private def breakAlgebraicLoop[A <: Node](FMUsInLoop: List[String], edges: Set[Edge[A]]): TarjanGraph[A] = {
    val reducedEdges = strategy match {
      case synthesizer.LoopStrategy.maximum =>
        //Remove all connections between FMUs in Loop
        edges.filter(e => e.srcNode.fmuName == e.trgNode.fmuName)
      case synthesizer.LoopStrategy.minimum =>
        //Remove enough edges connections to one FMU
        removeMinimumNumberOfEdges(FMUsInLoop, edges)
      case _ =>
        logger.debug("No strategy chosen - using maximum")
        //Remove all connections between FMUs in Loop
        edges.filter(e => e.srcNode.fmuName == e.trgNode.fmuName)
    }
    new TarjanGraph[A](reducedEdges)
  } ensuring(tarjanGraph => !tarjanGraph.hasCycle, "The graph does still contain cycles")

  def formatInitLoop(scc: List[InitializationInstructionNode]): InitializationInstruction = {
    val gets = graphBuilder.GetNodes.values.flatten.filter(o => scc.contains(o)).toList
    val edgesInGraph = edgesInSCC(InitEdge, scc)
    val tarjanGraph = breakAlgebraicLoop(scc.distinctBy(i => i.fmuName).map(i => i.fmuName), edgesInGraph)
    AlgebraicLoopInit(gets.map(_.port), tarjanGraph.topologicalSCC.flatten.map(_.formatInitInstruction))
  }


  private def formatAlgebraicLoop(sccNodes: List[StepInstructionNode],
                                  edges: Set[Edge[StepInstructionNode]],
                                  algorithm: CoSimAlgorithm,
                                  isNested: Boolean): CoSimAlgorithm = {
    val outputPorts = graphBuilder.GetNodes.values.flatten.filter(o => sccNodes.contains(o)).toList
    val edgesInsideSCC = edgesInSCC(edges, sccNodes)
    val FMUsInLoop = sccNodes.filter(_.isInstanceOf[DoStepNode]).distinctBy(i => i.fmuName).map(i => i.fmuName)
    val tarjanGraph = breakAlgebraicLoop(FMUsInLoop, edgesInsideSCC)

    val initial_algorithm = algorithm.copy(instructions = List.empty)

    val algebraicLoopAlgorithm = tarjanGraph.topologicalSCC.flatten
      .foldLeft(initial_algorithm)((acc, n) => formatStepInstruction(n, acc, isTentative(n, edgesInsideSCC)))

    val algorithm_with_saves = if (isNested) algorithm else createSaves(FMUsInLoop.toSet, algorithm)
    val restores = createRestores(FMUsInLoop.toSet, initial_algorithm)
    val loopInstructions = algebraicLoopAlgorithm.instructions.filter(IsLoopInstruction)
    val algebraicLoop = AlgebraicLoop(outputPorts.map(_.port), loopInstructions, restores.instructions)
    algorithm_with_saves.copy(instructions = algorithm_with_saves.instructions.:+(algebraicLoop))
  }

  def formatFeedthroughLoop(sccNodes: List[StepInstructionNode],
                            edges: Set[Edge[StepInstructionNode]],
                            init_algorithm: CoSimAlgorithm, isNested: Boolean): CoSimAlgorithm = {
    formatAlgebraicLoop(sccNodes, edges, init_algorithm, isNested)
  }

  def formatReactiveLoop(sccNodes: List[StepInstructionNode], edges: Set[Edge[StepInstructionNode]],
                         init_algorithm: CoSimAlgorithm, isNested: Boolean): CoSimAlgorithm = {
    formatAlgebraicLoop(sccNodes, edges, init_algorithm, isNested)
  }

  def isTentative(action: StepInstructionNode, edges: Set[Edge[StepInstructionNode]]): Boolean = {
    if (action.isInstanceOf[GetNode] && edges.exists(e => e.srcNode == action && e.trgNode.isInstanceOf[SetTentativeNode])) return true
    if (!action.isInstanceOf[SetNode]) false else edges.exists(e => e.trgNode == action && e.srcNode.isInstanceOf[GetNode])
  }

}


