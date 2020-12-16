package synthesizer

import core.Reactivity.{Value, reactive}
import core._
import sun.util.resources.Bundles.Strategy
import synthesizer.LoopStrategy.{LoopStrategy, maximum}

import scala.:+
import scala.collection.convert.ImplicitConversions.`collection asJava`
import scala.collection.mutable

object LoopStrategy extends Enumeration {
  type LoopStrategy = Value
  val minimum, maximum = Value
}

class Synthesizer(scenarioModel: ScenarioModel, strategy: LoopStrategy) {
  val graphBuilder: GraphBuilder = new GraphBuilder(scenarioModel)
  var FMUsThatHaveStepped: mutable.HashSet[String] = new mutable.HashSet[String]()

  def formatInitLoop(scc: mutable.Buffer[Node]): InitializationInstruction = {
    val outputPorts = {
      graphBuilder.GetNodes.values.flatten.filter(o => scc.contains(o)).map { case GetNode(ref) => ref }.toList
    }

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
    val steps = graphBuilder.stepNodes.filter(o => scc.contains(o))
    val gets = graphBuilder.GetNodes.values.flatten.filter(o => scc.contains(o))
    val setsDelayed = graphBuilder.SetNodesDelayed.values.flatten.filter(o => scc.contains(o))
    val setsReactive = graphBuilder.SetNodesDelayed.values.flatten.filter(o => scc.contains(o))

    var edgesInGraph = graphBuilder.stepEdges.filter(e => scc.contains(e.srcNode) && scc.contains(e.trgNode))
    val fmus = steps.map(o => o.name)

    //Add restore and save nodes:
    fmus.foreach(fmu => {
      edgesInGraph+= (Edge[Node](SaveNode(fmu), RestoreNode(fmu)))
      edgesInGraph+= (Edge[Node](SaveNode(fmu), DoStepNode(fmu)))
      val setsFMU = setsDelayed.filter(o => o.port.fmu == fmu) ++ setsDelayed.filter(o => o.port.fmu == fmu)
      setsFMU.foreach(s => {
        edgesInGraph+= (Edge[Node](SaveNode(fmu), s))
      })
    })

    //Remove all connections between FMUs
    if(strategy == maximum)
    //Remove all connections between FMUs
      edgesInGraph = edgesInGraph.filterNot(e => setsReactive.toList.contains(e.trgNode))
    else{
      //Remove all connections to a single FMU
      val fmu = outputPorts.groupBy(o => o.fmu)
      val reducedList = outputPorts.filterNot(o => o.fmu == fmu.head._1)
      edgesInGraph = edgesInGraph.filter(e => reducedList.contains(e.trgNode))
    }

    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edgesInGraph)

    AlgebraicLoop(outputPorts.map { case GetNode(ref) => ref }, tarjanGraph.topologicalSCC.flatten.map().toList)
  }
*/

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
      case DoStepNode(name) => {
        FMUsThatHaveStepped.add(name)
        Step(name, DefaultStepSize())
      }
      case GetNode(port) => if (FMUsThatHaveStepped.contains(port.fmu) && isLoop) GetTentative(port) else Get(port)
      case SetNode(port) => if (FMUsThatHaveStepped.contains(port.fmu) && isLoop) SetTentative(port) else core.Set(port)
      case RestoreNode(name) => RestoreState(name)
      case SaveNode(name) => SaveState(name)
    }
  }
}

