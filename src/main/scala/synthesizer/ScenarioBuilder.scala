package synthesizer

import core.Reactivity.{Reactivity, delayed, reactive}
import core.{ConnectionModel, FmuModel, InputPortConfig, InputPortModel, OutputPortModel, PortRef, ScenarioModel}

import scala.annotation.tailrec
import scala.util.Random

object ScenarioBuilder {
  var srcFMUs : Set[String] = Set.empty
  var trgFMUs : Set[String] = Set.empty

  def isReactive(): Reactivity = if (Random.nextBoolean()) delayed else reactive

  def createFeedthrough(inputNames: List[String], supportFeedthrough:Boolean): List[String] = {
    val dependencies = if (supportFeedthrough) Random.between(0, inputNames.size) else 0
    inputNames.take(dependencies)
  }

  def generateScenario(nFMU: Int, nConnection: Int, supportFeedthrough: Boolean, fmusMayRejectStep: Boolean = false): ScenarioModel = {
    assert(nFMU > 0)
    assert(nFMU <= nConnection, "Scenario should have minimum the same number og edges as FMU")

    val fmuNames = (0 until nFMU).map(i => f"fmu_${i}").toSet
    val connections = generateConnections(fmuNames, nConnection)
    assert(connections.groupBy(i => i.trgPort).forall(_._2.size == 1))

    val outputPortsPerFMU = connections.groupBy(o => o.srcPort.fmu).map(o => (o._1, o._2.map(o => o.srcPort)))
    val inputPortsPerFMU = connections.groupBy(o => o.trgPort.fmu).map(o => (o._1, o._2.map(o => o.trgPort)))

    val FMUs = fmuNames.map(i => {
      val inputs = inputPortsPerFMU(i).map(n => (n.port, InputPortModel(isReactive()))).toMap
      assert(inputs.nonEmpty)
      val inputNames = inputs.keys.toList
      val outputs = outputPortsPerFMU(i).map(n => (n.port, OutputPortModel(createFeedthrough(inputNames, supportFeedthrough), createFeedthrough(inputNames, supportFeedthrough)))).toMap
      assert(outputs.nonEmpty)

      val canReject = if(fmusMayRejectStep) Random.nextBoolean() else false
      //All FMUs can accept a step of arbitrary size
      (i, FmuModel(inputs, outputs, canReject))
    }).toMap

    ScenarioModel(FMUs, connections, if(fmusMayRejectStep) 2 else 1)
  }

  def generateConnections(fmuNames:Set[String], nConnection: Int): List[ConnectionModel] =
    (0 until nConnection).map(_ => CreateConnection(fmuNames, generatePortName(3))).toList

  def CreateConnection(FMUs:Set[String], portName: String): ConnectionModel ={
    val srcFMU = random(FMUs, srcFMUs)
    var trgFMU = ""
    if((trgFMUs + srcFMU).size < FMUs.size)
      trgFMU = random(FMUs, (trgFMUs + srcFMU))
      else
      trgFMU = random(FMUs, Set(srcFMU))

    updateSets(FMUs.size, srcFMU, trgFMU)
    ConnectionModel(PortRef(srcFMU, portName), PortRef(trgFMU, portName))
  }

  private def updateSets(nFMU: Int, srcFMU: String, trgFMU: String):Unit = {
    srcFMUs += srcFMU
    trgFMUs += trgFMU
    if (srcFMUs.size == nFMU)
      srcFMUs = Set.empty
    if (trgFMUs.size == nFMU)
      trgFMUs = Set.empty
  }

  def random[T](s: Set[T], excludes: Set[T]): T = {
    val reduced = (s -- excludes)
    reduced.iterator.drop(Random.nextInt(reduced.size)).next
  }

  val alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

  def generatePortName(n: Int): String =
    (1 to n).map(_ => alpha(Random.nextInt(alpha.length))).mkString
}
