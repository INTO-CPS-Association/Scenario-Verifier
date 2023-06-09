package synthesizer.ScenarioBuilder

import core.FMI3.{ClockType, FMI3ScenarioModel, Fmu3Model, InputClockModel, InputPortModel, OutputClockModel, OutputPortModel}
import core.Reactivity._
import core.{ConnectionModel, PortRef, Reactivity}

import scala.util.Random

object ScenarioBuilderFMI3 {
  private var srcFMUs: Set[String] = Set.empty
  private var trgFMUs: Set[String] = Set.empty

  private def isReactive: Reactivity.Value = if (Random.nextBoolean()) delayed else reactive

  private def createFeedthrough(inputNames: List[String], supportFeedthrough: Boolean): List[String] = {
    val dependencies = if (supportFeedthrough && inputNames.nonEmpty) Random.between(0, inputNames.size) else 0
    inputNames.take(dependencies)
  }

  def generateScenario(nFMU: Int, nConnection: Int, nTimeBasedClocks: Int,
                       nClockConnections: Int, supportFeedthrough: Boolean): FMI3ScenarioModel = {
    assert(nFMU > 0, "Scenario should have at least one FMU")
    assert(nFMU <= nConnection, "Scenario should have minimum the same number of edges as FMU")

    val fmuNames = (0 until nFMU).map(i => f"fmu-$i").toSet
    val connections = generateConnections(fmuNames, nConnection)
    assert(connections.groupBy(i => i.trgPort).forall(_._2.size == 1), "Each input port should have exactly one connection")
    val discreteConnections = connections.take(nConnection / 2)
    val continuousConnections = connections.drop(nConnection / 2)
    val clockConnections = generateConnections(fmuNames, nClockConnections)
    assert(clockConnections.groupBy(i => i.trgPort).forall(_._2.size == 1), "Each input clock should have exactly one connection")

    def portTypePerFMU(connectionModels: List[ConnectionModel], extractor: ConnectionModel => PortRef): Map[String, List[PortRef]] =
      connectionModels.groupBy(extractor(_).fmu).map(o => (o._1, o._2.map(extractor)))

    val outputPortsPerFMU = portTypePerFMU(connections, _.srcPort)
    val inputPortsPerFMU = portTypePerFMU(connections, _.trgPort)
    val inputClocksPerFMU = portTypePerFMU(clockConnections, _.trgPort)
    val outputClocksPerFMU = portTypePerFMU(clockConnections, _.srcPort)

    val FMUs = fmuNames.map(i => {
      val inputs =
        if (inputPortsPerFMU.contains(i))
          inputPortsPerFMU(i).map(n => (n.port, InputPortModel(isReactive, clocks = List.empty))).toMap
        else
          Map.empty[String, InputPortModel]
      //assert(inputs.nonEmpty)
      val inputNames = inputs.keys.toList
      val outputs = if (outputPortsPerFMU.contains(i))
        outputPortsPerFMU(i).map(n => (n.port,
          OutputPortModel(
            createFeedthrough(inputNames, supportFeedthrough),
            createFeedthrough(inputNames, supportFeedthrough),
            clocks = List.empty
          ))).toMap
      else
        Map.empty[String, OutputPortModel]
      //assert(outputs.nonEmpty)
      val outputNames = outputs.keys.toList
      val inputClocks =
        if (inputClocksPerFMU.contains(i))
          inputClocksPerFMU(i).map(n => (n.port, InputClockModel(ClockType.triggered, interval = 1))).toMap
        else
          Map.empty[String, InputClockModel]
      val outputClocks =
        if (outputClocksPerFMU.contains(i))
          outputClocksPerFMU(i).map(n => (n.port, OutputClockModel(ClockType.triggered, List.empty, List.empty))).toMap
        else
          Map.empty[String, OutputClockModel]

      //All FMUs can accept a step of arbitrary size
      (i, Fmu3Model(inputs, outputs, inputClocks, outputClocks, canRejectStep = false))
    }).toMap

    FMI3ScenarioModel(FMUs, connections, clockConnections, 1)
  }

  private def generateConnections(fmuNames: Set[String], nConnection: Int): List[ConnectionModel] =
    (0 until nConnection).map(_ => CreateConnection(fmuNames, randomName(3))).toList

  private def CreateConnection(FMUs: Set[String], portName: String): ConnectionModel = {
    val srcFMU = random(FMUs, srcFMUs)
    var trgFMU = ""
    if ((trgFMUs + srcFMU).size < FMUs.size)
      trgFMU = random(FMUs, trgFMUs + srcFMU)
    else
      trgFMU = random(FMUs, Set(srcFMU))

    updateSets(FMUs.size, srcFMU, trgFMU)
    ConnectionModel(PortRef(srcFMU, portName), PortRef(trgFMU, portName))
  }

  private def updateSets(nFMU: Int, srcFMU: String, trgFMU: String): Unit = {
    srcFMUs += srcFMU
    trgFMUs += trgFMU
    if (srcFMUs.size == nFMU)
      srcFMUs = Set.empty
    if (trgFMUs.size == nFMU)
      trgFMUs = Set.empty
  }

  private def random[T](s: Set[T], excludes: Set[T]): T = {
    val reduced = s -- excludes
    reduced.iterator.drop(Random.nextInt(reduced.size)).next()
  }

  private val alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

  private def randomName(nameLength: Int): String =
    (1 to nameLength).map(_ => alpha(Random.nextInt(alpha.length))).mkString
}
