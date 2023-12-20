package org.intocps.verification.scenarioverifier.cli.Z3

import org.intocps.verification.scenarioverifier
import org.intocps.verification.scenarioverifier.core.masterModel._
import org.intocps.verification.scenarioverifier.core.CosimStepInstruction
import org.intocps.verification.scenarioverifier.core.DefaultStepSize
import org.intocps.verification.scenarioverifier.core.EventInstruction
import org.intocps.verification.scenarioverifier.core.FMI3
import org.intocps.verification.scenarioverifier.core.FMI3.Get
import org.intocps.verification.scenarioverifier.core.FMI3.GetClock
import org.intocps.verification.scenarioverifier.core.FMI3.SetClock
import org.intocps.verification.scenarioverifier.core.InitGet
import org.intocps.verification.scenarioverifier.core.InitSet
import org.intocps.verification.scenarioverifier.core.InitializationInstruction
import org.intocps.verification.scenarioverifier.core.PortRef
import org.intocps.verification.scenarioverifier.core.Step

object Z3ModelParser {
  private def filterActions(z3Model: String): List[String] = {
    val actionRegex = """\(define-fun\s+(\S+)\s+\(\)\s+Int\s+(\d+)\)""".r
    val actions = actionRegex
      .findAllMatchIn(z3Model)
      .map(m => (m.group(1), m.group(2).toInt))
      .toList
      .filter(action => action._1 != "minAction" && action._1 != "maxAction")
    actions.sortBy(_._2).map(_._1)
  }

  private def parsePortRef(action: String): PortRef = {
    val fmuName = action.split("-").head
    val portName = action.split("-").last
    PortRef(fmuName, portName)
  }

  private def parseZ3InitAlgorithm(algorithm: String, masterModel: MasterModelFMI3): List[InitializationInstruction] = {
    val actions = filterActions(algorithm)
    val instructions = actions.map(action => {
      val portRef = parsePortRef(action)
      val fmuModel = masterModel.scenario.fmus(portRef.fmu)
      if (fmuModel.inputs.contains(portRef.port))
        InitSet(portRef)
      else if (fmuModel.outputs.contains(portRef.port))
        InitGet(portRef)
      else
        throw new Exception(s"The action $action is not a valid action in the given context")
    })
    instructions
  }
  private def parseZ3CoSimAlgorithm(algorithm: String, masterModel: MasterModelFMI3): List[CosimStepInstruction] = {
    val actions = filterActions(algorithm)
    val instructions = actions.map(action => {
      val fmuName = action.split("-").head
      val actionType = action.split("-").last
      if (actionType.equalsIgnoreCase("step"))
        Step(fmuName, DefaultStepSize())
      else {
        val portName = actionType
        val fmuModel = masterModel.scenario.fmus(fmuName)
        if (fmuModel.inputs.contains(portName))
          scenarioverifier.core.Set(PortRef(fmuName, portName))
        else if (fmuModel.outputs.contains(portName))
          scenarioverifier.core.Get(PortRef(fmuName, portName))
        else
          throw new Exception(s"The action $action is not a valid action in the given context")
      }
    })
    instructions
  }

  private def parseZ3EventAlgorithm(algorithm: String, masterModel: MasterModelFMI3): List[EventInstruction] = {
    val actions = filterActions(algorithm)
    val instructions = actions.map(action => {
      var portRef = parsePortRef(action)
      val fmuModel = masterModel.scenario.fmus(portRef.fmu)
      if (fmuModel.inputs.contains(portRef.port))
        FMI3.Set(portRef)
      else if (fmuModel.outputs.contains(portRef.port))
        Get(portRef)
      else if (fmuModel.inputClocks.contains(portRef.port))
        SetClock(portRef)
      else if (fmuModel.outputClocks.contains(portRef.port))
        GetClock(portRef)
      else
        throw new Exception(s"The action $action is not a valid action in the given context")
    })
    instructions
  }

  def parseZ3Model(model: String, masterModel: MasterModelFMI3): MasterModelFMI3 = {
    require(model.contains("sat"), "The model is not satisfiable")
    val algorithms = model.split("sat").filter(_.nonEmpty).map(_.trim)
    val initAlgorithm = parseZ3InitAlgorithm(algorithms.head, masterModel)
    val stepAlgorithm = Map("alg" -> parseZ3CoSimAlgorithm(algorithms(1), masterModel))
    val eventAlgorithms = masterModel.scenario.eventEntrances.indices
      .map(index => {
        val algorithm = algorithms(2 + index)
        val eventEntrance = masterModel.scenario.eventEntrances(index)
        s"Strategy ${index + 1}" -> EventStrategy(eventEntrance, parseZ3EventAlgorithm(algorithm, masterModel))
      })
      .toMap
    masterModel.copy(initialization = initAlgorithm, cosimStep = stepAlgorithm, eventStrategies = eventAlgorithms)
  }
}
