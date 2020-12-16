package synthesizer

import core.Reactivity.{Value, reactive}
import core._
import sun.util.resources.Bundles.Strategy
import synthesizer.LoopStrategy.{LoopStrategy, maximum}

import scala.collection.mutable

object LoopStrategy extends Enumeration {
  type LoopStrategy = Value
  val minimum, maximum = Value
}

class Synthesizer(scenarioModel: ScenarioModel, strategy: LoopStrategy) {
  val graphBuilder: GraphBuilder = new GraphBuilder(scenarioModel)

  var FMUsThatHaveStepped: mutable.HashSet[String] = new mutable.HashSet[String]()

  def formatInitLoop(scc: mutable.Buffer[Node]): InitializationInstruction = {
    val outputPorts = scc.filter(i => i match {
      case GetNode(_) => true
      case _ => false
    }).map { case GetNode(ref) => ref }.toList

    var edgesInGraph = graphBuilder.initialEdges.filter(e => scc.contains(e.srcNode) && scc.contains(e.trgNode))
    if(strategy == maximum)
      //Remove all connections between FMUs
      edgesInGraph = edgesInGraph.filterNot(e => outputPorts.contains(e.trgNode))
    else{
      //Remove all connections to a single FMU
      val fmu = outputPorts.groupBy(o => o.fmu)
      val reducedList = outputPorts.filterNot(o => o.fmu == fmu.head._1)
      edgesInGraph = edgesInGraph.filter(e => reducedList.contains(e.trgNode))
    }


    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edgesInGraph)

    AlgebraicLoopInit(outputPorts, tarjanGraph.topologicalSCC.flatten.map(formatInitialInstruction).toList)
  }

/*
  def formatAlgebraicLoop(scc: mutable.Buffer[Node]): CosimStepInstruction = {
    val doStepsInSCC = scc.filter(i => i match {
      case DoStepNode(_,_) => true
      case _ => false
    }).toList

    val GetsInSCC = scc.filter(i => i match {
      case GetNode(_) => true
      case _ => false
    }).toList

    val SetsInSCC = scc.filter(i => i match {
      case SetNode(_) => true
      case _ => false
    }).toList

    val reactivePorts = scenarioModel.fmus.flatMap(fmu => fmu._2.inputs).filter(i => i._2.reactivity == reactive)
    val reactiveOutputPorts = scc.filter(i => i match {
      case GetNode(portRef) => true
      case _ => false
    }).toList

    val outputPorts = scc.filter(i => i match {
      case GetNode(_) => true
      case _ => false
    }).toList

    var edgesInGraph = graphBuilder.stepEdges.filter(e => scc.contains(e.srcNode) && scc.contains(e.trgNode))

    //Add restore Nodes:

    //Remove all connections between FMUs
    edgesInGraph = edgesInGraph.filterNot(e => outputPorts.contains(e.trgNode))

    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edgesInGraph)

    AlgebraicLoop(outputPorts.map { case GetNode(ref) => ref }, tarjanGraph.topologicalSCC.flatten.map().toList)
  }*/


  def synthesizeInitialization(): List[InitializationInstruction] = {
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](graphBuilder.initialEdges)
    val SCCs = tarjanGraph.topologicalSCC

    SCCs.map(scc => {
      if (scc.size == 1) {
        formatInitialInstruction(scc.head)
      } else {
        formatInitLoop(scc)
      }
    }).toList
  }


  def synthesizeStep(): List[CosimStepInstruction] = {
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](graphBuilder.stepEdges)
    val SCCs = tarjanGraph.topologicalSCC

    SCCs.map(scc => {
      if (scc.size == 1) {
        //Currently only trivial SCC are considered
        formatStepInstruction(scc.head, false)
      } else {
        throw new UnsupportedOperationException()
      }
    }).toList
  }

  def formatInitialInstruction(node: Node): InitializationInstruction = {
    node match {
      case GetNode(port) => InitGet(port)
      case SetNode(port) => InitSet(port)
      case _ => throw new UnsupportedOperationException()
    }
  }

  def formatStepInstruction(node: Node, isLoop: Boolean): CosimStepInstruction = {
    node match {
      case DoStepNode(name, _) => {
        FMUsThatHaveStepped.add(name)
        Step(name, DefaultStepSize())
      }
      case GetNode(port) => if (FMUsThatHaveStepped.contains(port.fmu) && isLoop) GetTentative(port) else Get(port)
      case SetNode(port) => if (FMUsThatHaveStepped.contains(port.fmu) && isLoop) SetTentative(port) else core.Set(port)
      case RestoreNode(name, _) => RestoreState(name)
      case SaveNode(name, _) => SaveState(name)
    }
  }
}

