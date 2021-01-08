package trace_analyzer

import core.ModelEncoding
import core.Reactivity.reactive

import scala.collection.immutable


class TraceParser(modelEncoding: ModelEncoding) {
  def createPortVariable(portString: String, fmu: String, portName: String, isReactive: Boolean): PortVariableState = {
    val status = portString.split(",").find(_.contains("status")).get.split("=").last == "1"
    val time = portString.split(",").find(_.contains("time")).get.split("=").last.toInt
    PortVariableState(fmu, portName, time, status, isReactive)
  }

  def createFMUs(FMUStrings: immutable.Iterable[String]): Iterable[FMUState] = {
    modelEncoding.fmuEncoding.map(i => {
      val currentFMUStrings = FMUStrings.filter(_.contains(i._1))
      val isSaved = currentFMUStrings.find(_ contains "isSaved").get.split("=").last == "1"
      val savedTime = currentFMUStrings.find(_ contains "savedTime").get.split("=").last.toInt
      val timeStamp = currentFMUStrings.find(_ contains "cTime").get.split("=").last.toInt

      val inputVariables = currentFMUStrings.filter(_.contains("inputVariables"))
      val inputPorts = modelEncoding.fmuInputEncodingInverse(i._1).map(o => {
        createPortVariable(inputVariables.filter(input =>  variableById(input, o)).mkString(","), i._1, o._2, modelEncoding.fmuModels(i._1).inputs(o._2).reactivity == reactive)
      }).toList

      val outputVariables = currentFMUStrings.filter(_.contains("outputVariables"))
      val outputPorts = modelEncoding.fmuOutputEncodingInverse(i._1).map(o => {
        createPortVariable(outputVariables.filter(output =>  variableById(output, o)).mkString(","), i._1, o._2, false)
      }).toList

      new FMUState(isSaved, savedTime, timeStamp, i._1, inputPorts, outputPorts)
    })
  }

  private def variableById(in: String, o: (Int, String)): Boolean = {
    in.matches(f".*\\[${o._1}\\].*")
  }

  def parseState(stateStrings: List[String]): ModelState = {
    val stepVariables = stateStrings.filter(i => i startsWith "stepVariables")
    val FMUStrings = modelEncoding.fmuEncoding.flatMap(m => stateStrings.filter(i => i startsWith m._1))
    val FMUStates = createFMUs(FMUStrings)
    val isInit = stateStrings.find(i => i startsWith "isInit").get.split("=").last == "1"
    val isSimulation = stateStrings.find(i => i startsWith "isSimulation").get.split("=").last == "1"

    val reducedString = stateStrings.diff(stepVariables).diff(FMUStrings.toList)
    val loopActive = reducedString.find(i => i contains "loopActive").get.split("=").last == "1"
    val stepFinderActive = reducedString.find(i => i contains "stepFinderActive").get.split("=").last == "1"
    val time = reducedString.find(i => i startsWith "time=").get.split("=").last.toInt

    val nextAction = getAction(reducedString)

    new ModelState(stepFinderActive, loopActive, time, FMUStates.toList, nextAction, isInit, isSimulation)
  }

  def getPortName(fmuName: String, action: Int, variable: Int): String = {
    if (action == 0) modelEncoding.fmuOutputEncodingInverse(fmuName)(variable)
    else if (action == 1) modelEncoding.fmuInputEncodingInverse(fmuName)(variable)
    else ""
  }

  private def getAction(reducedString: List[String]): SUAction = {
    val action = reducedString.find(i => i startsWith "action=").get.split("=").last.toInt
    val stepSize = reducedString.find(i => i startsWith "stepsize=").get.split("=").last.toInt
    val relative_step_size = reducedString.find(i => i startsWith "relative_step_size=").get.split("=").last.toInt
    val activeFMU = reducedString.find(i => i startsWith "activeFMU=").get.split("=").last.toInt
    val commitment = reducedString.find(i => i startsWith "commitment=").get.split("=").last.toInt
    val variable = reducedString.find(i => i startsWith "var=").get.split("=").last.toInt
    val fmuName = modelEncoding.fmuEncodingInverse(activeFMU)
    val portName = getPortName(fmuName, action, variable)
    SUAction(fmuName, action, portName, stepSize, relative_step_size, commitment)
  }

  def parseScenarios(trace: Iterator[String]): Seq[ModelState] = {
    val filteredTrace = trace.flatMap(_.split(" ")).filterNot(notConstant)
    var states = Seq.empty[Seq[String]]
    var currentActions = Seq.empty[String]
    filteredTrace.filter(_.nonEmpty).foreach(i => {
      if (i.contains("State")) {
        states = states :+ (currentActions)
        currentActions = Seq.empty[String]
      } else {
        currentActions = currentActions :+ i
      }
    })
    //First is empty
    states = states.drop(1)
    states.map(o => o.toList).toList.map(parseState)
  }

  def notConstant(line: String): Boolean = line.startsWith("fmusUnloaded") || line.contains("savedInputVariables") || line.startsWith("connectionVariable") || line.contains("isConsistent=") || line.contains("_pc") || line.contains("isConsistent") || line.contains(".i=") || line.contains(".n=") || line.contains("#depth=") || line.contains("Transitions:") || line.contains("->")

}
