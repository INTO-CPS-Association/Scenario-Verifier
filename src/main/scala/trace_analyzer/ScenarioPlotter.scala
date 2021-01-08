package trace_analyzer

import java.io.File

import core.{FmuModel, InputPortModel, ModelEncoding, OutputPortModel}
import core.Reactivity.reactive
import guru.nidi.graphviz.attribute.{Color, Label, Shape, Style}
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.Factory.{graph, mutGraph, mutNode, port}
import guru.nidi.graphviz.model.{MutableGraph, MutableNode}
import org.apache.logging.log4j.scala.Logging
import org.jcodec.api.awt.AWTSequenceEncoder


object ScenarioPlotter extends Logging {

  def feedthroughConnections(outputPortModel: OutputPortModel, state: ModelState): List[String] =
    if (state.isInitState) outputPortModel.dependenciesInit else outputPortModel.dependenciesInit


  def isCurrent(action: SUAction, fmu: String, port: String): Boolean = action != null && action.FMU == fmu && action.Port == port

  //Todo Should be merged
  def createInputNode(fmu: String, portName: String, isReactive: Boolean, action: SUAction, state: ModelState): MutableNode = {
    val nodeName = fmu + "." + portName
    mutNode(nodeName)
      .add(if (isCurrent(action, fmu, portName)) Color.RED
      else if (isDefined(state, fmu, portName)) Color.BLACK else Color.WHITE,
        Shape.BOX,
        Label.lines(nodeName,
          state.portTime(fmu, portName, true),
          if (isReactive) "R" else "D"))
  }

  def createOutputNode(fmu: String, portName: String, action: SUAction, state: ModelState): MutableNode = {
    val nodeName = fmu + "." + portName
    mutNode(nodeName)
      .add(if (isCurrent(action, fmu, portName)) Color.RED
      else if (state.isDefinedOutputState(fmu, portName)) Color.BLACK else Color.WHITE,
        Shape.BOX,
        Label.lines(nodeName,
          state.portTime(fmu, portName, false)))
  }

  def createGraphOfState(state: ModelState, currentAction: SUAction, modelEncoding: ModelEncoding, name: String): MutableGraph = {
    val g = mutGraph(name).setDirected(true)
    modelEncoding.fmuModels.foreach(fmu => {
      val FMUName = fmu._1
      val cluster = graph.named(FMUName).cluster().graphAttr().`with`(Style.FILLED, Color.LIGHTGREY,
        Label.lines(FMUName.toUpperCase(),
          if (state.isInitState) "" else f"Timestamp: ${state.getTimeStamp(fmu._1)}",
          if (state.isInitState) "" else f"IsSaved: ${state.isSaved(fmu._1)}"
        )).toMutable
      fmu._2.inputs.foreach(input => cluster.add(createInputNode(FMUName, input._1, input._2.reactivity == reactive, currentAction, state)))

      fmu._2.outputs.foreach(output => {
        cluster.add(createOutputNode(FMUName, output._1, currentAction, state))
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
        if(currentAction!= null) f"Current Action: ${currentAction.action()}" else "",
        f"Next Action: ${state.action.action()}",
        if (state.isInitState) "" else s"Timestamp: ${state.timeStamp}"
      )))
  }

  private def isDefined(state: ModelState, fmu: String, port: String) = {
    state.isInitState && state.isDefinedInitInputState(fmu, port) || state.isSimulation && state.isDefinedInputState(fmu, port)
  }

  def plot(uppaalTrace: UppaalTrace): Unit = {
    val init_movie = new File(s"example/init_${uppaalTrace.scenarioName}.mp4")
    makeAnimation(init_movie, uppaalTrace.initStates, uppaalTrace.modelEncoding, uppaalTrace.scenarioName)

    val scenario_movie = new File(s"example/simulation_${uppaalTrace.scenarioName}.mp4")
    makeAnimation(scenario_movie, uppaalTrace.simulationStates, uppaalTrace.modelEncoding, uppaalTrace.scenarioName)
  }

  def makeAnimation(movie: File, states: Seq[ModelState], modelEncoding: ModelEncoding, scenarioName: String): Unit = {
    val encoder = AWTSequenceEncoder.createSequenceEncoder(movie, 1)
    var currentAction : SUAction = null

    states.indices.foreach(i => {
      val state = states(i)
      val g = createGraphOfState(state, currentAction, modelEncoding, scenarioName)
      encoder.encodeImage(Graphviz.fromGraph(g).height(512).width(512).render(Format.PNG).toImage)
      currentAction = state.action
    })
    encoder.finish()
  }

}
