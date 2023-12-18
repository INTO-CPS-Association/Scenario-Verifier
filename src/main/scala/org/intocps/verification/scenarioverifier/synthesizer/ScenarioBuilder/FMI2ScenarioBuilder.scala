package org.intocps.verification.scenarioverifier.synthesizer.ScenarioBuilder

import scala.util.Random

import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier.core.masterModel._
import org.intocps.verification.scenarioverifier.core.masterModel.Reactivity._
import org.intocps.verification.scenarioverifier.core.FMI3.AdaptiveModel
import org.intocps.verification.scenarioverifier.core.PortRef

trait ScenarioBuilder {
  private var srcFMUs: Set[String] = Set.empty
  private var trgFMUs: Set[String] = Set.empty

  def generateFMUNames(nFMU: Int): Set[String] = (0 until nFMU).map(i => f"FMU$i").toSet
  private def updateSets(nFMU: Int, srcFMU: String, trgFMU: String): Unit = {
    srcFMUs += srcFMU
    trgFMUs += trgFMU
    if (srcFMUs.size == nFMU)
      srcFMUs = Set.empty
    if (trgFMUs.size == nFMU)
      trgFMUs = Set.empty
  }

  def isReactive: Reactivity.Value = if (Random.nextBoolean()) delayed else reactive

  private def createConnection(FMUs: Set[String]): ConnectionModel = {
    val srcFMU = random(FMUs, srcFMUs)
    var trgFMU = ""
    if ((trgFMUs + srcFMU).size < FMUs.size)
      trgFMU = random(FMUs, trgFMUs + srcFMU)
    else
      trgFMU = random(FMUs, Set(srcFMU))

    updateSets(FMUs.size, srcFMU, trgFMU)
    createConnectionModelBetweenFMUs(srcFMU, trgFMU)
  }

  def createConnectionModelBetweenFMUs(srcFMU: String, trgFMU: String): ConnectionModel = {
    val portName = randomName(3)
    ConnectionModel(PortRef(srcFMU, portName), PortRef(trgFMU, portName))
  }

  def generateConnections(fmuNames: Set[String], nConnection: Int): List[ConnectionModel] =
    (0 until nConnection).map(_ => createConnection(fmuNames)).toList

  def createFeedthrough(inputNames: List[String], supportFeedthrough: Boolean): List[String] = {
    val dependencies = if (supportFeedthrough && inputNames.nonEmpty) Random.between(0, inputNames.size) else 0
    inputNames.take(dependencies)
  }

  private def random[T](s: Set[T], excludes: Set[T]): T = {
    val reduced = s -- excludes
    reduced.iterator.drop(Random.nextInt(reduced.size)).next()
  }

  private val alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

  private def randomName(nameLength: Int): String =
    (1 to nameLength).map(_ => alpha(Random.nextInt(alpha.length))).mkString

  def generateScenario(nFMU: Int, nConnection: Int, supportFeedthrough: Boolean, supportStepRejection: Boolean = false): ScenarioModel
}

object FMI2ScenarioBuilder extends Logging with ScenarioBuilder {
  private def buildConfiguration(): AdaptiveModel = AdaptiveModel(List.empty, Map.empty)

  override def generateScenario(
      nFMU: Int,
      nConnection: Int,
      supportFeedthrough: Boolean,
      supportStepRejection: Boolean = false): FMI2ScenarioModel = {
    assert(nFMU > 0)
    assert(nFMU <= nConnection, "Scenario should have minimum the same number og edges as FMU")

    val fmuNames = generateFMUNames(nFMU)
    val connections = generateConnections(fmuNames, nConnection)
    assert(connections.groupBy(i => i.trgPort).forall(_._2.size == 1))

    val outputPortsPerFMU = connections.groupBy(o => o.srcPort.fmu).map(o => (o._1, o._2.map(o => o.srcPort)))
    val inputPortsPerFMU = connections.groupBy(o => o.trgPort.fmu).map(o => (o._1, o._2.map(o => o.trgPort)))

    val fmus = fmuNames
      .map(i => {
        val inputs = inputPortsPerFMU(i).map(n => (n.port, FMI2InputPortModel(isReactive))).toMap
        assert(inputs.nonEmpty)
        val inputNames = inputs.keys.toList
        val outputs = outputPortsPerFMU(i)
          .map(n =>
            (
              n.port,
              FMI2OutputPortModel(createFeedthrough(inputNames, supportFeedthrough), createFeedthrough(inputNames, supportFeedthrough))))
          .toMap
        assert(outputs.nonEmpty)

        val canReject = if (supportStepRejection) Random.nextBoolean() else false
        // All FMUs can accept a step of arbitrary size
        (i, Fmu2Model(inputs, outputs, canReject, ""))
      })
      .toMap

    FMI2ScenarioModel(fmus, buildConfiguration(), connections, if (supportStepRejection) 2 else 1)
  }
}
