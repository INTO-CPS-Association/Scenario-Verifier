package synthesizer

import core._
import synthesizer.LoopStrategy.{LoopStrategy, maximum}

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
    val gets = graphBuilder.GetNodes.values.flatten.filter(o => scc.contains(o))

    var edgesInGraph = graphBuilder.initialEdges.filter(e => scc.contains(e.srcNode) && scc.contains(e.trgNode))
    if (strategy == maximum)
    //Remove all connections between FMUs
      edgesInGraph = edgesInGraph.filterNot(e => gets.contains(e.trgNode))
    else {
      //Remove all connections to a single FMU
      val reducedList = gets.groupBy(o => o.port.fmu).head._2
      edgesInGraph = edgesInGraph.filter(e => reducedList.contains(e.trgNode))
    }
    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edgesInGraph)

    AlgebraicLoopInit(gets.map(o => o.port).toList, tarjanGraph.topologicalSCC.flatten.map(formatInitialInstruction).toList)
  }


  def formatAlgebraicLoop(scc: mutable.Buffer[Node]): List[CosimStepInstruction] = {
    val steps = graphBuilder.stepNodes.filter(o => scc.contains(o))
    val gets = graphBuilder.GetNodes.values.flatten.filter(o => scc.contains(o))
    val setsDelayed = graphBuilder.SetNodesDelayed.values.flatten.filter(o => scc.contains(o))
    val setsReactive = graphBuilder.SetNodesReactive.values.flatten.filter(o => scc.contains(o))

    var edgesInSCC = graphBuilder.stepEdges.filter(e => scc.contains(e.srcNode) && scc.contains(e.trgNode))
    val FMUs = steps.map(o => o.name)

    val reactiveGets = gets.filter(o => (edgesInSCC.exists(edge => edge.srcNode == o && setsReactive.contains(edge.trgNode)))).toSet

    //Add restore and save nodes:
    FMUs.foreach(fmu => {
      edgesInSCC += (Edge[Node](SaveNode(fmu), RestoreNode(fmu)))
      edgesInSCC += (Edge[Node](SaveNode(fmu), DoStepNode(fmu)))
      //val setsFMU = setsDelayed.filter(o => o.port.fmu == fmu) ++ setsReactive.filter(o => o.port.fmu == fmu)
      (setsDelayed ++ setsReactive).foreach(s => {
        edgesInSCC += (Edge[Node](SaveNode(fmu), s))
      })
      reactiveGets.foreach(g => {
        edgesInSCC += (Edge[Node](g, RestoreNode(fmu)))
      })
    })

    //Remove all connections between FMUs
    if (strategy == maximum)
    //Remove all connections between FMUs
      edgesInSCC = edgesInSCC.filterNot(e => setsReactive.toList.contains(e.trgNode) && gets.contains(e.srcNode))
    else {
      //Remove all connections to a single FMU
      val reducedList = setsReactive.groupBy(o => o.port.fmu).head._2
      edgesInSCC = edgesInSCC.filterNot(e => reducedList.contains(e.trgNode) && gets.contains(e.srcNode))
    }

    val tarjanGraph: TarjanGraph[Node] = new TarjanGraph[Node](edgesInSCC)

    val instructions = tarjanGraph.topologicalSCC.flatten.map(formatStepInstruction(_, true)).filter(o => o match {
      case RestoreState(fmu) => false
      case SaveState(fmu) => false
      case _ => true
    }).toList
    val saves = FMUs.map(o => formatStepInstruction(SaveNode(o), true)).toList
    val restores = FMUs.map(o => formatStepInstruction(RestoreNode(o), true)).toList
    saves.:+(AlgebraicLoop(reactiveGets.map(_.port).toList, instructions, restores))
  }

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
    var instructions : List[CosimStepInstruction] = List.empty[CosimStepInstruction]
    SCCs.foreach(scc => {
      if (scc.size == 1) {
        //Currently only trivial SCC are considered
        instructions = instructions.:+(formatStepInstruction(scc.head, false))
      } else {
        instructions = instructions ++ formatAlgebraicLoop(scc)
      }
    })
    instructions
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

