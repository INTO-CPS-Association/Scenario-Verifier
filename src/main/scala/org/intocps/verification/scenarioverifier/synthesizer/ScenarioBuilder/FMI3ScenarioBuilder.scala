package org.intocps.verification.scenarioverifier.synthesizer.ScenarioBuilder

import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier.core.masterModel._
import org.intocps.verification.scenarioverifier.core.PortRef

object FMI3ScenarioBuilder extends Logging with ScenarioBuilder {
  private def generateClockConnections(discreteConnections: List[ConnectionModel]): List[ConnectionModel] = {
    discreteConnections.map(connection => createConnectionModelBetweenFMUs(connection.srcPort.fmu, connection.trgPort.fmu))
  }

  def generateScenario(
      nFMU: Int,
      nConnection: Int,
      nTimeBasedClocks: Int,
      nClockConnections: Int,
      supportFeedthrough: Boolean): FMI3ScenarioModel = {
    assert(nFMU > 0, "Scenario should have at least one FMU")
    assert(nFMU <= nConnection, "Scenario should have minimum the same number of edges as FMU")

    val fmuNames = generateFMUNames(nFMU)

    val continuousConnections = generateConnections(fmuNames, nConnection)
    assert(continuousConnections.groupBy(i => i.trgPort).forall(_._2.size == 1), "Each input port should have exactly one connection")
    // Half of the connections are discrete and half are continuous
    val discreteConnections = generateConnections(fmuNames, nClockConnections)
    val clockConnections = generateClockConnections(discreteConnections)
    assert(clockConnections.groupBy(i => i.trgPort).forall(_._2.size == 1), "Each input clock should have exactly one connection")

    val timedBasedClocks = (0 until nTimeBasedClocks).map(i => f"clock-$i" -> InputClockModel(ClockType.timed, interval = i + 1)).toList

    def portTypePerFMU(connectionModels: List[ConnectionModel], extractor: ConnectionModel => PortRef): Map[String, List[PortRef]] =
      connectionModels.groupBy(extractor(_).fmu).map(o => (o._1, o._2.map(extractor)))

    val outputPortsPerFMU = portTypePerFMU(continuousConnections, _.srcPort)
    val inputPortsPerFMU = portTypePerFMU(continuousConnections, _.trgPort)
    val inputClocksPerFMU = portTypePerFMU(clockConnections, _.trgPort)
    val outputClocksPerFMU = portTypePerFMU(clockConnections, _.srcPort)

    val fmus = fmuNames
      .map(i => {
        val inputs =
          if (inputPortsPerFMU.contains(i))
            inputPortsPerFMU(i).map(n => (n.port, FMI3InputPortModel(isReactive, clocks = List.empty))).toMap
          else
            Map.empty[String, FMI3InputPortModel]
        // assert(inputs.nonEmpty)
        val inputNames = inputs.keys.toList
        val outputs =
          if (outputPortsPerFMU.contains(i))
            outputPortsPerFMU(i)
              .map(n =>
                (
                  n.port,
                  FMI3OutputPortModel(
                    createFeedthrough(inputNames, supportFeedthrough),
                    createFeedthrough(inputNames, supportFeedthrough),
                    clocks = List.empty)))
              .toMap
          else
            Map.empty[String, FMI3OutputPortModel]
        // assert(outputs.nonEmpty)
        val outputNames = outputs.keys.toList
        val inputClocks =
          if (inputClocksPerFMU.contains(i))
            inputClocksPerFMU(i).map(n => (n.port, InputClockModel(ClockType.triggered, interval = 0))).toMap
          else
            Map.empty[String, InputClockModel]
        val outputClocks =
          if (outputClocksPerFMU.contains(i))
            outputClocksPerFMU(i).map(n => (n.port, OutputClockModel(ClockType.triggered, List.empty, List.empty))).toMap
          else
            Map.empty[String, OutputClockModel]

        // All FMUs can accept a step of arbitrary size
        (i, Fmu3Model(inputs, outputs, inputClocks, outputClocks, canRejectStep = false))
      })
      .toMap

    FMI3ScenarioModel(fmus, continuousConnections ++ discreteConnections, clockConnections, 1)
  }

  override def generateScenario(nFMU: Int, nConnection: Int, supportFeedthrough: Boolean, supportStepRejection: Boolean): ScenarioModel =
    throw new NotImplementedError("FMI3ScenarioBuilder does not support the generation of ScenarioModels for FMI2")
}
