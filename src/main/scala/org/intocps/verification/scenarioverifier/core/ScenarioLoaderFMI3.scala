package org.intocps.verification.scenarioverifier.core

import scala.annotation.unused

import com.typesafe.config.Config
import org.intocps.verification.scenarioverifier.core.masterModel._
import org.intocps.verification.scenarioverifier.core.FMI3._
import pureconfig.generic.auto._
import pureconfig.generic.ProductHint
import pureconfig.ConfigSource

object ScenarioLoaderFMI3 extends ScenarioLoader[MasterModelFMI3, FMI3MasterConfig] {
  protected def parseConfig(conf: Config): MasterModelFMI3 = {
    // This forces pure config to not tolerate unknown keys in the config file.
    // It gives errors when typos happen.
    // From https://pureconfig.github.io/docs/overriding-behavior-for-case-classes.html
    @unused
    implicit val hintNestedStepStatement: ProductHint[NestedStepStatement] = ProductHint[NestedStepStatement](allowUnknownKeys = false)
    @unused
    implicit val hintRootStepStatement: ProductHint[RootStepStatement] = ProductHint[RootStepStatement](allowUnknownKeys = false)
    @unused
    implicit val hintNestedInitStatement: ProductHint[NestedInitStatement] = ProductHint[NestedInitStatement](allowUnknownKeys = false)
    @unused
    implicit val hintRootInitStatement: ProductHint[RootEventStatement] = ProductHint[RootEventStatement](allowUnknownKeys = false)
    @unused
    implicit val hintMasterConfig: ProductHint[FMI3MasterConfig] = ProductHint[FMI3MasterConfig](allowUnknownKeys = false)
    val parsingResults = ConfigSource.fromConfig(conf).load[FMI3MasterConfig]
    val masterConfig = extractMasterConfig(parsingResults)
    parse(masterConfig)
  }

  override def parse(config: FMI3MasterConfig): MasterModelFMI3 = {
    config match {
      case FMI3MasterConfig(name, scenario, initialization, cosimStep, eventStrategies) =>
        val scenarioModel = parse(scenario)
        val instantiationModel = generateInstantiationInstructions(scenarioModel.scenarioModel).toList
        val initializationModel = initialization.map(instruction => parse(instruction, scenarioModel))
        val expandedInitModel =
          generateEnterInitInstructions(scenarioModel.scenarioModel) ++ initializationModel ++ generateExitInitInstructions(
            scenarioModel.scenarioModel)
        val cosimStepModel =
          cosimStep.map(instructions =>
            (instructions._1, instructions._2.map(instruction => ScenarioLoaderFMI2.parse(instruction, scenarioModel.scenarioModel))))
        val eventStrategyModel = eventStrategies.map(strategy =>
          (
            strategy._1,
            EventStrategy(
              EventEntrance(strategy._2.clocks.map(clock => parsePortRefBase(clock, "", scenarioModel.fmus)).toSet),
              strategy._2.iterate.map(instruction => parse(instruction, scenarioModel)))))
        val terminateModel = generateTerminateInstructions(scenarioModel.scenarioModel).toList
        MasterModelFMI3(
          name,
          scenarioModel,
          instantiationModel,
          expandedInitModel.toList,
          cosimStepModel,
          eventStrategyModel,
          terminateModel)
    }
  }

  def parse(instruction: InitializationStatement, scenarioModel: FMI3ScenarioModel): InitializationInstruction = {
    // Check uniqueness of instruction
    instruction match {
      case FMI3RootInitStatement(get, set, getShift, getInterval, loop) =>
        val baseOps = List(get, set, getShift, getInterval).count(b => b.nonEmpty && !b.isBlank)
        val nOps = baseOps + (if (loop == NoLoopInit) 0 else 1)
        assert(nOps == 1, s"Initialization instruction $instruction must be one of get, set or loop-init.")
    }
    if (!instruction.get.isBlank) {
      parsePortAction[Fmu3Model](instruction.get, scenarioModel.fmus, InitGet, _.outputs)
    } else if (!instruction.set.isBlank) {
      parsePortAction[Fmu3Model](instruction.set, scenarioModel.fmus, InitSet, _.inputs)
    } else if (!instruction.getShift.isBlank) {
      parsePortAction[Fmu3Model](instruction.getShift, scenarioModel.fmus, GetShift, _.inputClocks)
    } else if (!instruction.getInterval.isBlank) {
      parsePortAction[Fmu3Model](instruction.getInterval, scenarioModel.fmus, GetInterval, _.inputClocks)
    } else {
      // Check subclasses
      instruction match {
        case FMI3RootInitStatement(_, _, _, _, loop) =>
          assert(loop != NoLoopInit, s"Invariant violated while parsing instruction $instruction")
          ScenarioLoaderFMI2.parse(loop, scenarioModel.scenarioModel)
      }
    }
  }

  def parse(instruction: EventStatement, scenarioModel: FMI3ScenarioModel): EventInstruction = {
    // Check uniqueness of instruction
    instruction match {
      case RootEventStatement(get, set, setClock, getClock, step, next) =>
        val nOps = List(get, set, setClock, getClock, step, next).count(b => b.nonEmpty && !b.isBlank)
        assert(nOps == 1, s"Event instruction $instruction must be one of get, set, setClock, getClock, step or next.")
    }
    if (!instruction.get.isBlank) {
      parsePortAction[Fmu3Model](instruction.get, scenarioModel.fmus, Get, _.outputs)
    } else if (!instruction.set.isBlank) {
      parsePortAction[Fmu3Model](instruction.set, scenarioModel.fmus, Set, _.inputs)
    } else if (!instruction.getClock.isBlank) {
      parsePortAction[Fmu3Model](instruction.getClock, scenarioModel.fmus, GetClock, _.outputClocks)
    } else if (!instruction.setClock.isBlank) {
      parsePortAction[Fmu3Model](instruction.setClock, scenarioModel.fmus, SetClock, _.inputClocks)
    } else if (!instruction.next.isBlank) {
      parsePortAction[Fmu3Model](instruction.next, scenarioModel.fmus, NextClock, _.inputClocks)
    } else if (!instruction.step.isBlank) {
      val fmuRef = instruction.step
      StepE(fmuRef)
    } else {
      // Check subclasses
      instruction match {
        case RootEventStatement(_, _, _, _, _, _) =>
          throw new RuntimeException(s"Invariant violated while parsing instruction $instruction")
      }
    }
  }

  private def parse(scenario: ScenarioConfig): FMI3ScenarioModel = {
    scenario match {
      case ScenarioConfig(fmus, connections, clockConnections, maxPossibleStepSize) =>
        assert(maxPossibleStepSize > 0, "Max possible step size has to be greater than 0 in scenario configuration.")
        val fmusModels = fmus.map(keyValPair => (keyValPair._1, parse(keyValPair._1, keyValPair._2)))
        val connectionsModel = connections.map(c => parseConnection(c, fmusModels))
        val clockConnectionModel = clockConnections.map(c => parseConnection(c, fmusModels))
        FMI3ScenarioModel(fmusModels, connectionsModel, clockConnectionModel, maxPossibleStepSize).enrich()
    }
  }

  private def parse(clockConfig: InputClockConfig): InputClockModel = {
    clockConfig match {
      case InputClockConfig(typeOfClock, interval) =>
        InputClockModel(ClockType.withName(typeOfClock), interval)
    }
  }

  private def parse(clockConfig: OutputClockConfig): OutputClockModel = {
    clockConfig match {
      case OutputClockConfig(typeOfClock, dependencies, dependenciesClocks) =>
        OutputClockModel(ClockType.withName(typeOfClock), dependencies, dependenciesClocks)
    }
  }

  def parse(config: InputPortConfig): FMI3InputPortModel = {
    config match {
      case InputPortConfig(reactivity, clocks) => FMI3InputPortModel(Reactivity.withName(reactivity), clocks)
    }
  }

  def parse(
      config: OutputPortConfig,
      inputsModel: Map[String, FMI3InputPortModel],
      outputPortId: String,
      fmuId: String): FMI3OutputPortModel = {
    config match {
      case OutputPortConfig(dependenciesInit, dependencies, clocks) =>
        val errorCheck = (inputPortRef: String) =>
          assert(
            inputsModel.contains(inputPortRef),
            f"Unable to resolve input port reference $inputPortRef in the output port $outputPortId FMU $fmuId.")
        dependenciesInit.foreach(errorCheck)
        dependencies.foreach(errorCheck)
        FMI3OutputPortModel(dependenciesInit, dependencies, clocks)
    }
  }

  private def parse(fmuId: String, fmu: FmuConfig): Fmu3Model = {
    fmu match {
      case FmuConfig(inputs, outputs, inputClocks, outputClocks, canRejectStep, path) =>
        val inputsModel = inputs.map(keyValPair => (keyValPair._1, parse(keyValPair._2)))
        val outputPortsModel = outputs.map(keyValPair => (keyValPair._1, parse(keyValPair._2, inputsModel, keyValPair._1, fmuId)))
        val inputClockModel = inputClocks.map(keyValPair => (keyValPair._1, parse(keyValPair._2)))
        val outputClockModel = outputClocks.map(keyValPair => (keyValPair._1, parse(keyValPair._2)))
        Fmu3Model(inputsModel, outputPortsModel, inputClockModel, outputClockModel, canRejectStep, path)
    }
  }
}
