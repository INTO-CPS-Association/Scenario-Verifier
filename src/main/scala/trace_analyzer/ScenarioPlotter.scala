package trace_analyzer

import java.io.File

import core.ModelEncoding
import core.Reactivity.reactive
import guru.nidi.graphviz.attribute.{Color, Label, Shape, Style}
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.Factory.{graph, mutGraph, mutNode}
import guru.nidi.graphviz.model.MutableGraph


object ScenarioPlotter {
  def createGraphOfState(state: ModelState, modelEncoding: ModelEncoding, name: String): MutableGraph = {
    val g = mutGraph(name).setDirected(true)
    modelEncoding.fmuModels.foreach(i => {
      val cluster = graph.named(i._1).cluster().graphAttr().`with`(Style.FILLED, Color.LIGHTGREY,
        Label.lines(i._1.toUpperCase(),
          if (state.isInitState) "" else f"Timestamp: ${state.getTimeStamp(i._1)}",
          if (state.isInitState) "" else f"IsSaved: ${state.isSaved(i._1)}"
        )).toMutable
      i._2.inputs.foreach(input => {
        if (state.isInitState && state.isDefinedInitInputState(i._1, input._1) || state.isSimulation && state.isDefinedInputState(i._1, input._1))
          cluster.add(mutNode(i._1 + "." + input._1)
            .add(Color.BLACK, Shape.BOX, Label.lines(i._1 + "." + input._1, state.portTime(i._1, input._1, true), if(input._2.reactivity == reactive) "R" else "D")))
        else
          cluster.add(mutNode(i._1 + "." + input._1)
            .add(Color.WHITE, Shape.BOX, Label.lines(i._1 + "." + input._1, state.portTime(i._1, input._1, true), if(input._2.reactivity == reactive) "R" else "D")))
      })
      i._2.outputs.foreach(v => {
        if (state.isDefinedOutputState(i._1, v._1))
          cluster.add(mutNode(i._1 + "." + v._1)
            .add(Color.BLACK, Shape.BOX, Label.lines(i._1 + "." + v._1, state.portTime(i._1, v._1, false))))
        else
          cluster.add(mutNode(i._1 + "." + v._1)
            .add(Color.WHITE, Shape.BOX, Label.lines(i._1 + "." + v._1, state.portTime(i._1, v._1, false))))

        if (state.isInitState) {
          v._2.dependenciesInit.foreach(input => {
            cluster.add(mutNode(i._1 + "." + input).addLink(mutNode(i._1 + "." + v._1)))
          })
        } else {
          v._2.dependencies.foreach(input => {
            cluster.add(mutNode(i._1 + "." + input).addLink(mutNode(i._1 + "." + v._1)))
          })
        }
        cluster.addTo(g)
      })
      val connectionsFromFMU = modelEncoding.connections.filter(_.srcPort.fmu == i._1)
      connectionsFromFMU.foreach(v => {
        g.add(
          mutNode(v.srcPort.fmu + "." + v.srcPort.port)
            .addLink(mutNode(v.trgPort.fmu + "." + v.trgPort.port)))
      })
    })


    g.add(mutNode(state.action.action())
      .add(Shape.DIAMOND, Label.lines(
        if (state.isInitState) "Initialization" else "Simulation",
        f"Action: ${state.action.action()}",
        if (state.isInitState) "" else s"Timestamp: ${state.timeStamp}"
      )))
  }

  def plot(uppaalTrace: UppaalTrace): Unit = {
    uppaalTrace.initStates.indices.foreach(i => {
      val g = createGraphOfState(uppaalTrace.initStates(i), uppaalTrace.modelEncoding, uppaalTrace.scenarioName)
      Graphviz.fromGraph(g).render(Format.PNG).toFile(new File(String.format("example/%s/init_test%s.png", g.name().toString, i)))
    })

    uppaalTrace.simulationStates.indices.foreach(i => {
      val g = createGraphOfState(uppaalTrace.simulationStates(i), uppaalTrace.modelEncoding, uppaalTrace.scenarioName)
      Graphviz.fromGraph(g).render(Format.PNG).toFile(new File(String.format("example/%s/simulation_test%s.png", g.name().toString, i)))
    })
  }

}
