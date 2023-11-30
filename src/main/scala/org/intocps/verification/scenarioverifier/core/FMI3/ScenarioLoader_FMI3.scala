package org.intocps.verification.scenarioverifier.core.FMI3

import com.typesafe.config.ConfigFactory
import io.circe._
import io.circe.syntax._
import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier
import org.intocps.verification.scenarioverifier.core.{ConnectionModel, ConnectionParserSingleton, EnterInitMode, ExitInitMode, FMURefParserSingleton, InitGet, InitSet, InitializationInstruction, NestedInitStatement, NestedStepStatement, NoLoopInit, PortRef, Reactivity, RootStepStatement, ScenarioLoader}
import org.intocps.verification.scenarioverifier.prettyprint.PPrint
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.error.{ConfigReaderFailure, ConvertFailure, UnknownKey}
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._

import java.io.{File, InputStream, InputStreamReader}
import java.nio.file.{Files, Paths}

object ScenarioLoaderFMI3 extends Logging {
  def load(file: String): MasterModel3 = {
    if (!Files.exists(Paths.get(file))) {
      val msg = f"File not found: $file. Current working directory is: ${System.getProperty("user.dir")}."
      logger.error(msg)
      throw new IllegalArgumentException(msg)
    }
    val conf = ConfigFactory.parseFile(new File(file))
    val parsingResults = ConfigSource.fromConfig(conf).load[FMI3MasterConfig]
    val masterConfig = extractMasterConfig(parsingResults)
    parse(masterConfig)
  }

  def load(stream: InputStream): MasterModel3 = {
    val reader = new InputStreamReader(stream)
    try {
      // read from stream
      val conf = ConfigFactory.parseReader(reader)

      // This forces pure config to not tolerate unknown keys in the config file.
      // It gives errors when typos happen.
      // From https://pureconfig.github.io/docs/overriding-behavior-for-case-classes.html
      implicit val hintNestedStepStatement: ProductHint[NestedStepStatement] = ProductHint[NestedStepStatement](allowUnknownKeys = false)
      implicit val hintRootStepStatement: ProductHint[RootStepStatement] = ProductHint[RootStepStatement](allowUnknownKeys = false)
      implicit val hintNestedInitStatement: ProductHint[NestedInitStatement] = ProductHint[NestedInitStatement](allowUnknownKeys = false)
      implicit val hintRootInitStatement: ProductHint[RootEventStatement] = ProductHint[RootEventStatement](allowUnknownKeys = false)
      implicit val hintMasterConfig: ProductHint[FMI3MasterConfig] = ProductHint[FMI3MasterConfig](allowUnknownKeys = false)

      val parsingResults: Result[FMI3MasterConfig] = ConfigSource.fromConfig(conf).load[FMI3MasterConfig]
      val masterConfig = extractMasterConfig(parsingResults)
      parse(masterConfig)
    } finally {
      reader.close()
    }
  }

  private def prettyPrintError(e: ConfigReaderFailure): Unit = {
    e match {
      case ConvertFailure(UnknownKey(key), _, path) =>
        logger.error(f"Unknown key '$key' in element $path.")
      case other =>
        logger.error(other.description)
    }
  }

  private def extractMasterConfig(parsingResults: Result[FMI3MasterConfig]): FMI3MasterConfig = {
    parsingResults match {
      case Left(errors) =>
        logger.error("Errors during parsing.")
        for (e <- errors.toList) {
          prettyPrintError(e)
        }
        throw new IllegalArgumentException(errors.toString())
      case Right(master) =>
        logger.info(f"Successfully parsed master configuration.")
        PPrint.pprint(master, l = logger)
        master
    }
  }

  def parse(config: FMI3MasterConfig): MasterModel3 = {
    config match {
      case FMI3MasterConfig(name, scenario, initialization, cosimStep, eventStrategies) =>
        val scenarioModel = parse(scenario)
        val instantiationModel = ScenarioLoader.generateInstantiationInstructions(scenarioModel.scenarioModel).toList
        val initializationModel = initialization.map(instruction => parse(instruction, scenarioModel))
        val expandedInitModel = ScenarioLoader.generateEnterInitInstructions(scenarioModel.scenarioModel) ++ initializationModel ++ ScenarioLoader.generateExitInitInstructions(scenarioModel.scenarioModel)
        val cosimStepModel = cosimStep.map(instruction => ScenarioLoader.parse(instruction, scenarioModel.scenarioModel))
        val eventStrategyModel = eventStrategies.map(strategy => (strategy._1,
          EventStrategy(
            EventEntrance(strategy._2.clocks.map(clock =>
              parsePortRef(clock, "", scenarioModel.fmus)).toSet),
            strategy._2.iterate.map(instruction => parse(instruction, scenarioModel)))))
        val terminateModel = ScenarioLoader.generateTerminateInstructions(scenarioModel.scenarioModel).toList
        MasterModel3(name, scenarioModel, instantiationModel, expandedInitModel.toList, cosimStepModel, eventStrategyModel, terminateModel)
    }
  }

  private def parse(masterModel: MasterModel3): MasterModelDTO = {
    val filteredInitializationActions = masterModel.initialization.filterNot(i => i.isInstanceOf[EnterInitMode] || i.isInstanceOf[ExitInitMode])
    MasterModelDTO(masterModel.name, masterModel.scenario, filteredInitializationActions, masterModel.cosimStep, masterModel.eventStrategies)
  }

  private def enrichDTO(masterModelDTO: MasterModelDTO): MasterModel3 = {
    val instantiationModel = ScenarioLoader.generateInstantiationInstructions(masterModelDTO.scenario.scenarioModel).toList
    val expandedInitModel = (
      ScenarioLoader.generateEnterInitInstructions(masterModelDTO.scenario.scenarioModel)
        ++ masterModelDTO.initialization
        ++ ScenarioLoader.generateExitInitInstructions(masterModelDTO.scenario.scenarioModel)).toList
    val terminateModel = ScenarioLoader.generateTerminateInstructions(masterModelDTO.scenario.scenarioModel).toList
    MasterModel3(masterModelDTO.name, masterModelDTO.scenario, instantiationModel, expandedInitModel, masterModelDTO.cosimStep, masterModelDTO.eventStrategies, terminateModel)
  }

  private def parsePortRef(str: String, context: String, fmus: Map[String, Fmu3Model]): PortRef = {
    val pRes = FMURefParserSingleton.parse(scenarioverifier.core.FMURefParserSingleton.fmu_port_ref, str)
    assert(pRes.successful, s"Problem parsing fmu port reference $str in $context.")
    assert(fmus.contains(pRes.get.fmu), s"Unable to resolve fmu ${pRes.get.fmu} in $context.")
    pRes.get
  }

  def parse(instruction: InitializationStatement, scenarioModel: FMI3ScenarioModel): InitializationInstruction = {
    // Check uniqueness of instruction
    instruction match {
      case FMI3RootInitStatement(get, set, getShift, getInterval, loop) =>
        val baseOps = List(get, set, getShift, getInterval).count(b => b.nonEmpty && !b.isBlank)
        val nOps = baseOps + (if (loop == NoLoopInit) 0 else 1)
        assert(nOps == 1,
          s"Initialization instruction $instruction must be one of get, set or loop-init.")
    }
    if (!instruction.get.isBlank) {
      val pRef = parsePortRef(instruction.get, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).outputs.contains(pRef.port), s"Unable to resolve output port ${pRef.port} in fmu ${pRef.fmu}.")
      InitGet(pRef)
    } else if (!instruction.set.isBlank) {
      val pRef = parsePortRef(instruction.set, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).inputs.contains(pRef.port), s"Unable to resolve input port ${pRef.port} in fmu ${pRef.fmu}.")
      InitSet(pRef)
    } else if (!instruction.getShift.isBlank) {
      val pRef = parsePortRef(instruction.set, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).inputClocks.contains(pRef.port), s"Unable to resolve input port ${pRef.port} in fmu ${pRef.fmu}.")
      GetShift(pRef)
    } else if (!instruction.getInterval.isBlank) {
      val pRef = parsePortRef(instruction.set, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).inputClocks.contains(pRef.port), s"Unable to resolve input port ${pRef.port} in fmu ${pRef.fmu}.")
      GetInterval(pRef)
    } else {
      // Check subclasses
      instruction match {
        case FMI3RootInitStatement(_, _, _, _, loop) =>
          assert(loop != NoLoopInit, s"Invariant violated while parsing instruction $instruction")
          ScenarioLoader.parse(loop, scenarioModel.scenarioModel)
      }
    }
  }


  def parse(instruction: EventStatement, scenarioModel: FMI3ScenarioModel): EventInstruction = {
    // Check uniqueness of instruction
    instruction match {
      case RootEventStatement(get, set, setClock, getClock, step, next) =>
        val nOps = List(get, set, setClock, getClock, step, next).count(b => b.nonEmpty && !b.isBlank)
        assert(nOps == 1,
          s"Event instruction $instruction must be one of get, set, setClock, getClock, step or next.")
    }
    if (!instruction.get.isBlank) {
      val pRef = parsePortRef(instruction.get, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).outputs.contains(pRef.port), s"Unable to resolve output port ${pRef.port} in fmu ${pRef.fmu}.")
      Get(pRef)
    } else if (!instruction.set.isBlank) {
      val pRef = parsePortRef(instruction.set, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).inputs.contains(pRef.port), s"Unable to resolve input port ${pRef.port} in fmu ${pRef.fmu}.")
      Set(pRef)
    } else if (!instruction.getClock.isBlank) {
      val pRef = parsePortRef(instruction.getClock, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).inputClocks.contains(pRef.port), s"Unable to resolve input port ${pRef.port} in fmu ${pRef.fmu}.")
      GetClock(pRef)
    } else if (!instruction.setClock.isBlank) {
      val pRef = parsePortRef(instruction.setClock, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).outputClocks.contains(pRef.port), s"Unable to resolve output port ${pRef.port} in fmu ${pRef.fmu}.")
      SetClock(pRef)
    } else if (!instruction.next.isBlank) {
      val pRef = parsePortRef(instruction.next, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).inputClocks.contains(pRef.port), s"Unable to resolve input port ${pRef.port} in fmu ${pRef.fmu}.")
      NextClock(pRef)
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

  private def parseConnection(connectionConfig: String, fmus: Map[String, Fmu3Model]): ConnectionModel = {
    val results = ConnectionParserSingleton.parse(ConnectionParserSingleton.connection, connectionConfig)
    assert(results.successful, s"Problem parsing connection string $connectionConfig.")
    val connection = results.get
    assert(fmus.contains(connection.srcPort.fmu), s"Unable to resolve source fmu ${connection.srcPort.fmu} in connection $connectionConfig.")
    assert(fmus.contains(connection.trgPort.fmu), s"Unable to resolve source fmu ${connection.trgPort.fmu} in connection $connectionConfig.")
    connection
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

  def parse(config: InputPortConfig): InputPortModel = {
    config match {
      case InputPortConfig(reactivity, clocks) => InputPortModel(Reactivity.withName(reactivity), clocks)
    }
  }

  def parse(config: OutputPortConfig, inputsModel: Map[String, InputPortModel], outputPortId: String, fmuId: String): OutputPortModel = {
    config match {
      case OutputPortConfig(dependenciesInit, dependencies, clocks) =>
        val errorCheck = (inputPortRef: String) => assert(inputsModel.contains(inputPortRef),
          f"Unable to resolve input port reference $inputPortRef in the output port $outputPortId FMU $fmuId.")
        dependenciesInit.foreach(errorCheck)
        dependencies.foreach(errorCheck)
        OutputPortModel(dependenciesInit, dependencies, clocks)
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