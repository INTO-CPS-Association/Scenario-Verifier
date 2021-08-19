package trace_analyzer

import java.awt.Dimension
import java.io.File

import core.Reactivity.reactive
import core.{ModelEncoding, OutputPortModel}
import guru.nidi.graphviz.attribute.{Color, Label, Shape, Style}
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.Factory.{graph, mutGraph, mutNode}
import guru.nidi.graphviz.model.{MutableGraph, MutableNode}
import org.apache.logging.log4j.scala.Logging
import org.jcodec.api.awt.AWTSequenceEncoder

import scala.collection.mutable.ListBuffer
import scala.math.{ceil, log10, pow}

object ScenarioPlotter extends Logging {

  def feedthroughConnections(outputPortModel: OutputPortModel, state: ModelState): List[String] =
    if (state.isInitState) outputPortModel.dependenciesInit else outputPortModel.dependenciesInit


  def isCurrent(action: SUAction, fmu: String, port: String): Boolean = action != null && action.FMU == fmu && action.Port == port

  //Todo Should be merged
  def createInputNode(fmu: String, portName: String, isReactive: Boolean, action: SUAction, state: ModelState): MutableNode = {
    val nodeName = fmu + "." + portName
    mutNode(nodeName)
      .add(if (isCurrent(action, fmu, portName)) Color.RED
      else if (isDefined(state, fmu, portName)) Color.BLACK
      else Color.WHITE,
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


  def createGraphOfState(state: ModelState, currentAction: SUAction, possibleActions: List[SUAction], modelEncoding: ModelEncoding, name: String): MutableGraph = {
    val g = mutGraph(name).setDirected(true)
    modelEncoding.fmuModels.foreach(fmu => {
      val FMUName = fmu._1
      val cluster = graph.named(FMUName).cluster().graphAttr().`with`(Style.FILLED, Color.LIGHTGREY,
        Label.lines(FMUName.toUpperCase(),
          if (state.isInitState) "" else f"Timestamp: ${state.getTimeStamp(fmu._1)}",
          if (state.isInitState) "" else f"IsSaved: ${state.isSaved(fmu._1)}",
          if (state.isInitState) "" else f"Can Step: ${state.canStep(fmu._1)}",
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

    g.add(actionNode(state, currentAction))
    g.add(possibleAction(state, possibleActions))

    //g.add(createLegend())
  }

  def createLegend(): MutableNode = {
    mutNode("Legend")
      .add(
        Shape.SQUARE,
        Label.htmlLines("<b>Legend:</b>",
          "Black: defined",
          "White: undefined",
          "Red: current"
        ))
  }

  private def actionNode(state: ModelState, currentAction: SUAction): MutableNode = {
    mutNode(state.action.action())
      .add(Shape.DIAMOND, Label.lines(
        if (state.isInitState) "Initialization" else "Simulation",
        if (currentAction != null) f"Current Action: ${currentAction.action()}" else "",
        f"Next Action: ${state.action.action()}",
        if (state.isInitState) "" else s"Timestamp: ${state.timeStamp}",
        if (state.loopActive) "Solving Algebraic loop" else ""
      ))
  }

  private def possibleAction(state: ModelState, actions: List[SUAction]): MutableNode = {
    mutNode(s"Possible Next Actions :")
      .add(Shape.BOX,
        Label.html(
          f"<b>Possible Next Actions ${if (state.checksDisabled) "(Checks disabled)"}:</b><br/>" +
            actions.sortBy(i => i.FMU).map(i => i.action()).mkString("<br/>")
        ))
  }

  private def isDefined(state: ModelState, fmu: String, port: String) = {
    state.isInitState && state.isDefinedInitInputState(fmu, port) || state.isSimulation && state.isDefinedInputState(fmu, port)
  }

  def plot(uppaalTrace: UppaalTrace, outputDirectory: String): Unit = {
    val init_movie = new File(s"${outputDirectory}/init_${uppaalTrace.scenarioName}.mp4")
    makeAnimation(init_movie, uppaalTrace.initStates, uppaalTrace.modelEncoding, uppaalTrace.scenarioName)

    if(uppaalTrace.simulationStates.nonEmpty){
      val scenario_movie = new File(s"${outputDirectory}/simulation_${uppaalTrace.scenarioName}.mp4")
      makeAnimation(scenario_movie, uppaalTrace.simulationStates, uppaalTrace.modelEncoding, uppaalTrace.scenarioName)
    }
  }

  def getDimensions(state: ModelState, modelEncoding: ModelEncoding, scenarioName: String): dimensions = {
    val g = createGraphOfState(state, null, List.empty, modelEncoding, scenarioName)
    val image = Graphviz.fromGraph(g).render(Format.PNG).toImage
    val log2 = (x: Double) => log10(x) / log10(2.0)
    dimensions(pow(2, ceil(log2(image.getHeight))).toInt, pow(2, ceil(log2(image.getWidth))).toInt)
  }

  def makeAnimation(movie: File, states: Seq[ModelState], modelEncoding: ModelEncoding, scenarioName: String): Unit = {
    val encoder = AWTSequenceEncoder.createSequenceEncoder(movie, 1)
    var currentAction: SUAction = null
    val performedActions = ListBuffer[SUAction]()
    val dimensions: dimensions = getDimensions(states.head, modelEncoding, scenarioName)

    states.indices.foreach(i => {
      val state = states(i)
      val g = createGraphOfState(state, currentAction, state.possibleActions
        .filterNot(act => if (state.checksDisabled) false else performedActions.exists(per => per.actionNumber == act.actionNumber && per.FMU == act.FMU && act.Port == per.Port)), modelEncoding, scenarioName)
      encoder.encodeImage(Graphviz.fromGraph(g).height(dimensions.height).width(dimensions.width).render(Format.PNG).toImage)
      currentAction = state.action
      performedActions += currentAction
      if (state == states.last)
        Graphviz.fromGraph(g).render(Format.SVG).toFile(new File(String.format("example/%s.svg", scenarioName)))
    })
    encoder.finish()
  }

  case class dimensions(val height: Int, val width: Int)

}
