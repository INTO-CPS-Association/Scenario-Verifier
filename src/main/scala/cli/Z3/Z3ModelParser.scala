package cli.Z3

import core.FMI3.{EventInstruction, EventStrategy, MasterModel3}
import core.{CosimStepInstruction, DefaultStepSize, InitializationInstruction, PortRef}

object Z3ModelParser {
  private def filterActions(z3Model: String): List[String] = {
    val actionRegex = """\(define-fun\s+(\w+)\s+\(\)\s+Int\s+(\d+)\)""".r
    val actions = actionRegex.findAllMatchIn(z3Model).map(m => (m.group(1), m.group(2).toInt)).toList.filter(action => action._1 != "minAction" && action._1 != "maxAction")
    actions.sortBy(_._2).map(_._1)
  }

  private def parseZ3InitAlgorithm(Z3algorithm: String,
                                   masterModel: MasterModel3): List[InitializationInstruction] = {
    val actions = filterActions(Z3algorithm)
    val instructions = actions.map(action => {
      val fmuName = action.split("-").head
      val portName = action.split("-").last
      val fmuModel = masterModel.scenario.fmus(fmuName)
      if (fmuModel.inputs.contains(portName))
        core.InitSet(PortRef(fmuName, portName))
      else if (fmuModel.outputs.contains(portName))
        core.InitGet(PortRef(fmuName, portName))
      else
        throw new Exception(s"The action $action is not a valid action in the given context")
    })
    instructions
  }

  private def parseZ3CoSimAlgorithm(Z3algorithm: String,
                                    masterModel: MasterModel3): List[CosimStepInstruction] = {
    val actions = filterActions(Z3algorithm)
    val instructions = actions.map(action => {
      val fmuName = action.split("-").head
      val actionType = action.split("-").last
      if (actionType.equalsIgnoreCase("step"))
        core.Step(fmuName, DefaultStepSize())
      else {
        val portName = actionType
        val fmuModel = masterModel.scenario.fmus(fmuName)
        if (fmuModel.inputs.contains(portName))
          core.Set(PortRef(fmuName, portName))
        else if (fmuModel.outputs.contains(portName))
          core.Get(PortRef(fmuName, portName))
        else
          throw new Exception(s"The action $action is not a valid action in the given context")
      }
    })
    instructions
  }

  private def parseZ3EventAlgorithm(algorithm: String,
                                    masterModel: MasterModel3): List[EventInstruction] = {
    val actions = filterActions(algorithm)
    val instructions = actions.map(action => {
      val fmuName = action.split("-").head
      val portName = action.split("-").last
      val fmuModel = masterModel.scenario.fmus(fmuName)
      if (fmuModel.inputs.contains(portName))
        core.FMI3.Set(PortRef(fmuName, portName))
      else if (fmuModel.outputs.contains(portName))
        core.FMI3.Get(PortRef(fmuName, portName))
      else if (fmuModel.inputClocks.contains(portName))
        core.FMI3.SetClock(PortRef(fmuName, portName))
      else if (fmuModel.outputClocks.contains(portName))
        core.FMI3.GetClock(PortRef(fmuName, portName))
      else
        throw new Exception(s"The action $action is not a valid action in the given context")
    })
    instructions
  }

  def parseZ3Model(model: String, masterModel: MasterModel3): MasterModel3 = {
    require(model.contains("sat"), "The model is not satisfiable")
    val algorithms = model.split("sat").filter(_.nonEmpty).map(_.trim)
    val initAlgorithm = parseZ3InitAlgorithm(algorithms.head, masterModel)
    val stepAlgorithm = parseZ3CoSimAlgorithm(algorithms(1), masterModel)
    val eventAlgorithms = masterModel.scenario.eventEntrances.indices.map(index => {
      val algorithm = algorithms(2 + index)
      val eventEntrance = masterModel.scenario.eventEntrances(index)
      s"Strategy ${index + 1}" -> EventStrategy(eventEntrance, parseZ3EventAlgorithm(algorithm, masterModel))
    }).toMap
    masterModel.copy(initialization = initAlgorithm, cosimStep = stepAlgorithm, eventStrategies = eventAlgorithms)
  }
}
