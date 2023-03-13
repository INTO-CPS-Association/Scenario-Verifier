package trace_analyzer

import core.{AbsoluteStepSize, CosimStepInstruction, ModelEncoding, PortRef, StepSize}

final case class UPPAAL_Action(FMU: String = "", actionNumber: Int = -1, Port: String = "", stepSize: Int = -1, relative_step_size: Int = -1, commitment: Int = -1) {
  def format(): String = {
    actionNumber match {
      case 0 => f"Get $FMU.$Port"
      case 1 => f"Set $FMU.$Port"
      case 2 => f"Step $FMU ${formatStep()}"
      case 3 => f"Save $FMU"
      case 4 => f"Restore $FMU"
      case 9 => f"Solve Loop $FMU"
      case 10 => f"Step routine $FMU"
      case _ => "Done"
    }
  }

  def toCosimStepInstruction: CosimStepInstruction = {
    actionNumber match {
      case 0 => core.Get(PortRef(FMU, Port))
      case 1 => core.Set(PortRef(FMU, Port))
      case 2 => core.Step(FMU, AbsoluteStepSize(1))
      case 3 => core.SaveState(FMU)
      case 4 => core.RestoreState(FMU)
      case 9 => core.AlgebraicLoop(List.empty, List.empty, List.empty)
      case 10 => core.StepLoop(List.empty, List.empty, List.empty)
      case _ => throw new Exception("Invalid action number")
    }
  }

  private def formatStep(): String = {
    if (stepSize != -1) f"by $stepSize"
    else f"as same as $relative_step_size"
  }
}


final case class UppaalTrace(modelEncoding: ModelEncoding,
                             initStates: Seq[ModelState],
                             simulationStates: Seq[ModelState],
                             scenarioName: String) {
  def getLastEnabledActions: Set[CosimStepInstruction] = {
    val lastState = simulationStates.last
    val lastEnabledActions = lastState.possibleActions
    lastEnabledActions.map(_.toCosimStepInstruction).toSet
  }
}

final case class ModelState(checksDisabled: Boolean,
                            loopActive: Boolean,
                            timeStamp: Int,
                            FMUs: List[FMUState],
                            action: UPPAAL_Action,
                            possibleActions: List[UPPAAL_Action],
                            isInitState: Boolean,
                            isSimulation: Boolean,
                            previous: Option[UPPAAL_Action] = None) {

  def canStep(fmuName: String): Boolean = {
    val fmu = FMUs.find(_.name == fmuName).get
    fmu.inputPorts.forall(i => i.isReactive && i.time > fmu.timeStamp || !i.isReactive && i.time == fmu.timeStamp)
  }

  def portTime(fmu: String, portName: String, isInput: Boolean): String = {
    if (isInput)
      FMUs.flatMap(i => i.inputPorts).find(i => i.fmu == fmu && i.name == portName).get.time.toString
    else
      FMUs.flatMap(i => i.outputPorts).find(i => i.fmu == fmu && i.name == portName).get.time.toString
  }

  def isDefinedInitInputState(fmu: String, portName: String): Boolean = {
    definedInputs.exists(i => i.fmu == fmu && i.name == portName)
  }


  def isDefinedInputState(fmu: String, portName: String): Boolean =
    definedInputs.exists(i => i.fmu == fmu && i.name == portName && (if (i.isReactive) i.time > FMUs.find(_.name == fmu).get.timeStamp else i.time >= FMUs.find(_.name == fmu).get.timeStamp))

  def isDefinedOutputState(fmu: String, portName: String): Boolean =
    definedOutputs.exists(i => i.fmu == fmu && i.name == portName && i.time == FMUs.find(_.name == fmu).get.timeStamp)

  def getTimeStamp(fmuName: String): Int = {
    FMUs.find(i => i.name == fmuName).get.timeStamp
  }

  def isSaved(fmuName: String): Boolean = {
    FMUs.find(i => i.name == fmuName).get.isSaved
  }

  private val definedInputs: List[PortVariableState] = FMUs.flatMap(i => i.inputPorts.filter(o => o.defined))
  private val definedOutputs: List[PortVariableState] = FMUs.flatMap(i => i.outputPorts.filter(o => o.defined))

  def printState(): Unit = {
    println("------------------------------------------")
    println(f"ChecksDisabled = $checksDisabled")
    println(f"LoopActive = $loopActive")
    println(f"Time = $timeStamp")
    FMUs.foreach(_.printFMU())
    println("Next Action:")
    action.format()

  }
}

final case class FMUState(isSaved: Boolean, saveTime: Int, timeStamp: Int, name: String, inputPorts: List[PortVariableState], outputPorts: List[PortVariableState]) {
  def printFMU(): Unit = {
    println("****************************************")
    println(f"FMU = $name")
    println(f"isSaved = $isSaved")
    println(f"FMU-Time = $timeStamp")
    println("Input Ports:")
    inputPorts.foreach(_.printPort())
    println("Output Ports:")
    outputPorts.foreach(_.printPort())
  }
}

case class PortVariableState(fmu: String, name: String, time: Int, defined: Boolean, isReactive: Boolean) {
  def printPort(): Unit = {
    println(f"PortName = $name")
    println(f"time = $time")
    println(f"IsDefined = $defined")
  }
}


