package trace_analyzer

import java.io.File

import core.{FmuModel, InputPortModel, ModelEncoding, OutputPortModel}
import core.Reactivity.reactive
import guru.nidi.graphviz.attribute.{Color, Label, Shape, Style}
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.Factory.{graph, mutGraph, mutNode}
import guru.nidi.graphviz.model.MutableGraph
import org.jcodec.api.awt.AWTSequenceEncoder


object ScenarioPlotter {

  def feedthroughConnections(outputPortModel: OutputPortModel, state: ModelState): List[String] =
    if (state.isInitState) outputPortModel.dependenciesInit else outputPortModel.dependenciesInit


  def createGraphOfState(state: ModelState, modelEncoding: ModelEncoding, name: String): MutableGraph = {
    val g = mutGraph(name).setDirected(true)
    modelEncoding.fmuModels.foreach(fmu => {
      val cluster = graph.named(fmu._1).cluster().graphAttr().`with`(Style.FILLED, Color.LIGHTGREY,
        Label.lines(fmu._1.toUpperCase(),
          if (state.isInitState) "" else f"Timestamp: ${state.getTimeStamp(fmu._1)}",
          if (state.isInitState) "" else f"IsSaved: ${state.isSaved(fmu._1)}"
        )).toMutable
      fmu._2.inputs.foreach(input =>
        cluster.add(mutNode(fmu._1 + "." + input._1)
          .add(if (isDefined(state, fmu, input)) Color.BLACK else Color.WHITE, Shape.BOX, Label.lines(fmu._1 + "." + input._1, state.portTime(fmu._1, input._1, true), if (input._2.reactivity == reactive) "R" else "D")))
      )
      fmu._2.outputs.foreach(output => {
        cluster.add(mutNode(fmu._1 + "." + output._1)
          .add(if (state.isDefinedOutputState(fmu._1, output._1)) Color.BLACK else Color.WHITE, Shape.BOX, Label.lines(fmu._1 + "." + output._1, state.portTime(fmu._1, output._1, false))))

        feedthroughConnections(output._2, state).foreach(input => {
          cluster.add(mutNode(fmu._1 + "." + input).addLink(mutNode(fmu._1 + "." + output._1)))
        })
      })
      cluster.addTo(g)

      val connectionsFromFMU = modelEncoding.connections.filter(_.srcPort.fmu == fmu._1)
      connectionsFromFMU.foreach(connection =>
        g.add(mutNode(connection.srcPort.fmu + "." + connection.srcPort.port)
          .addLink(mutNode(connection.trgPort.fmu + "." + connection.trgPort.port)))
      )
    })

    g.add(mutNode(state.action.action())
      .add(Shape.DIAMOND, Label.lines(
        if (state.isInitState) "Initialization" else "Simulation",
        f"Action: ${state.action.action()}",
        if (state.isInitState) "" else s"Timestamp: ${state.timeStamp}"
      )))
  }

  private def isDefined(state: ModelState, i: (String, FmuModel), input: (String, InputPortModel)) = {
    state.isInitState && state.isDefinedInitInputState(i._1, input._1) || state.isSimulation && state.isDefinedInputState(i._1, input._1)
  }

  def plot(uppaalTrace: UppaalTrace): Unit = {
    val init_movie = new File(s"example/init_${uppaalTrace.scenarioName}.mp4")
    val encoder = AWTSequenceEncoder.createSequenceEncoder(init_movie, 1)
    uppaalTrace.initStates.indices.foreach(i => {
      val g = createGraphOfState(uppaalTrace.initStates(i), uppaalTrace.modelEncoding, uppaalTrace.scenarioName)
      encoder.encodeImage(Graphviz.fromGraph(g).height(512).width(512).render(Format.PNG).toImage)
    })
    encoder.finish()

    val scenario_movie = new File(s"example/simulation_${uppaalTrace.scenarioName}.mp4")
    val scenario_Encoder = AWTSequenceEncoder.createSequenceEncoder(scenario_movie, 1)

    uppaalTrace.simulationStates.indices.foreach(i => {
      val g = createGraphOfState(uppaalTrace.simulationStates(i), uppaalTrace.modelEncoding, uppaalTrace.scenarioName)
      scenario_Encoder.encodeImage(Graphviz.fromGraph(g).height(512).width(512).render(Format.PNG).toImage)
    })

    scenario_Encoder.finish()
  }

}
