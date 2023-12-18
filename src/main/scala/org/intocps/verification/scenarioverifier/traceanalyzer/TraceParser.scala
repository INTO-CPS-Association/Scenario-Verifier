package org.intocps.verification.scenarioverifier.traceanalyzer

import scala.collection.immutable

import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier.core.masterModel.Reactivity.reactive
import org.intocps.verification.scenarioverifier.core.ModelEncoding

class TraceParser(modelEncoding: ModelEncoding) extends Logging {
  private def createPortVariable(portString: String, fmu: String, portName: String, isReactive: Boolean): PortVariableState = {
    val status = portString.split(",").find(_.contains("status")).get.split("=").last == "1"
    val time = portString.split(",").find(_.contains("time")).get.split("=").last.toInt
    PortVariableState(fmu, portName, time, status, isReactive)
  }

  private def createFMUs(FMUStrings: immutable.Iterable[String]): Iterable[FMUState] = {
    modelEncoding.fmuEncoding.map(i => {
      val currentFMUStrings = FMUStrings.filter(_.contains(i._1))
      val isSaved = currentFMUStrings.find(_ contains "isSaved").get.split("=").last == "1"
      val savedTime = currentFMUStrings.find(_ contains "savedTime").get.split("=").last.toInt
      val timeStamp = currentFMUStrings.find(_ contains "cTime").get.split("=").last.toInt

      val inputVariables = currentFMUStrings.filter(_.contains("inputVariables"))
      val inputPorts = modelEncoding
        .fmuInputEncodingInverse(i._1)
        .map(o => {
          createPortVariable(
            inputVariables.filter(input => variableById(input, o)).mkString(","),
            i._1,
            o._2,
            modelEncoding.fmuModels(i._1).inputs(o._2).reactivity == reactive)
        })
        .toList

      val outputVariables = currentFMUStrings.filter(_.contains("outputVariables"))
      val outputPorts = modelEncoding
        .fmuOutputEncodingInverse(i._1)
        .map(o => {
          createPortVariable(outputVariables.filter(output => variableById(output, o)).mkString(","), i._1, o._2, isReactive = false)
        })
        .toList

      FMUState(isSaved, savedTime, timeStamp, i._1, inputPorts, outputPorts)
    })
  }

  private def variableById(in: String, o: (Int, String)): Boolean = {
    in.matches(f".*\\[${o._1}\\].*")
  }

  def createAction(enabledAction: String): UPPAAL_Action = {
    // The last 4 characters _fmu should be removed
    val fmuName = enabledAction.split("\\.").head.dropRight(4)
    val actionType: Int = getActionType(enabledAction)

    val portName = getPortName(fmuName, actionType, getVariable(enabledAction, actionType))
    UPPAAL_Action(fmuName, actionType, portName, -1, -1, 0)
  }

  private def getActionType(enabledAction: String): Int = {
    if (enabledAction.contains("get")) 0
    else if (enabledAction.contains("set")) 1
    else 2
  }

  private def getVariable(enabledAction: String, actionType: Int): Int = {
    if (actionType != 2) {
      val index = "\\[\\d+\\]".r.findFirstMatchIn(enabledAction).get
      enabledAction.substring(index.start + 1, index.`end` - 1).toInt
    } else -1
  }

  def getPossibleActions(transitions: List[String], encoding: ModelEncoding): List[UPPAAL_Action] = {
    encoding.fmuNames.flatMap(i => transitions.filter(_.startsWith(i)).map(i => createAction(i))).toList
  }

  private def parseState(stateString: String): ModelState = {
    val stateStrings = stateString.split(" ").filterNot(notConstant).toList
    val stepVariables = stateStrings.filter(i => i.startsWith("stepVariables"))
    val FMUStrings = modelEncoding.fmuEncoding.flatMap(m => stateStrings.filter(i => i.startsWith(m._1)))
    val possibleActions = getPossibleActions(FMUStrings.toList.filter(i => i.contains("Enabled") && i.contains("=1")), modelEncoding)

    val FMUStates = createFMUs(FMUStrings)
    val isInit = stateStrings.find(_.startsWith("isInit")).get.split("=").last == "1"
    val isSimulation = stateStrings.find(_.startsWith("isSimulation")).get.split("=").last == "1"

    val reducedString = stateStrings.diff(stepVariables).diff(FMUStrings.toList)
    val loopActive = reducedString.find(_ contains "loopActive").get.split("=").last != "-1"
    // val stepFinderActive = reducedString.find(_ contains "stepFinderActive").get.split("=").last == "1"
    val time = reducedString.find(_.startsWith("time=")).get.split("=").last.toInt
    val checksDisabled = reducedString.find(_.startsWith("checksDisabled=")).get.split("=").last == "1"

    val nextAction = getAction(reducedString)

    ModelState(checksDisabled, loopActive, time, FMUStates.toList, nextAction, possibleActions, isInit, isSimulation)
  }

  private def getPortName(fmuName: String, action: Int, variable: Int): String = {
    if (action == 0) modelEncoding.fmuOutputEncodingInverse(fmuName)(variable)
    else if (action == 1) modelEncoding.fmuInputEncodingInverse(fmuName)(variable)
    else ""
  }

  private def getAction(reducedString: List[String]): UPPAAL_Action = {
    val action = reducedString.find(i => i.startsWith("action=")).get.split("=").last.toInt
    val stepSize = reducedString.find(i => i.startsWith("stepsize=")).get.split("=").last.toInt
    val relative_step_size = reducedString.find(i => i.startsWith("relative_step_size=")).get.split("=").last.toInt
    val activeFMU = reducedString.find(i => i.startsWith("activeFMU=")).get.split("=").last.toInt
    val commitment = reducedString.find(i => i.startsWith("commitment=")).get.split("=").last.toInt
    val variable = reducedString.find(i => i.startsWith("var=")).get.split("=").last.toInt

    val fmuName = if (action != 9 && action != 10) modelEncoding.fmuEncodingInverse(activeFMU) else ""
    val portName = getPortName(fmuName, action, variable)
    UPPAAL_Action(fmuName, action, portName, stepSize, relative_step_size, commitment)
  }

  def parseTrace(trace: Iterator[String]): Seq[ModelState] = {
    trace.toList.map(parseState)
  }

  private def notConstant(line: String): Boolean =
    line.startsWith("fmusUnloaded") || line.contains("savedInputVariables") || line.startsWith("connectionVariable") || line.contains(
      "isConsistent=") || line.contains("_pc") || line.contains("isConsistent") || line.contains(".i=") || line.contains(".n=") || line
      .contains("#depth=")

}
