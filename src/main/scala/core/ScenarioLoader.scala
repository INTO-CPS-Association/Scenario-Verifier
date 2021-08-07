package core

import java.io.{File, InputStream, InputStreamReader}
import java.nio.file.{Files, Paths}
import com.typesafe.config.ConfigFactory
import org.apache.logging.log4j.scala.Logging
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pretty_print.PPrint
import pureconfig.error.{ConfigReaderFailure, ConfigReaderFailures, ConvertFailure, UnknownKey}
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._

import scala.collection.immutable

object ScenarioLoader extends Logging {

  def load(file: String): MasterModel = {
    if (!Files.exists(Paths.get(file))) {
      val msg = f"File not found: ${file}. Current working directory is: ${System.getProperty("user.dir")}."
      logger.error(msg)
      throw new IllegalArgumentException(msg)
    }
    val conf = ConfigFactory.parseFile(new File(file))
    val parsingResults = ConfigSource.fromConfig(conf).load[MasterConfig]
    val masterConfig = extractMasterConfig(parsingResults)
    parse(masterConfig)
  }

  def load(stream: InputStream): MasterModel = {
    val reader = new InputStreamReader(stream)
    try {
      // read from stream
      val conf = ConfigFactory.parseReader(reader)

      // This forces pure config to not tolerate unknown keys in the config file.
      // It gives errors when typos happen.
      // From https://pureconfig.github.io/docs/overriding-behavior-for-case-classes.html
      implicit val hintNestedStepStatement = ProductHint[NestedStepStatement](allowUnknownKeys = false)
      implicit val hintRootStepStatement = ProductHint[RootStepStatement](allowUnknownKeys = false)
      implicit val hintNestedInitStatement = ProductHint[NestedInitStatement](allowUnknownKeys = false)
      implicit val hintRootInitStatement = ProductHint[RootInitStatement](allowUnknownKeys = false)
      implicit val hintMasterConfig = ProductHint[MasterConfig](allowUnknownKeys = false)

      val parsingResults = ConfigSource.fromConfig(conf).load[MasterConfig]
      val masterConfig = extractMasterConfig(parsingResults)
      parse(masterConfig)
    } finally {
      reader.close()
    }
  }

  def prettyPrintError(e: ConfigReaderFailure): Unit = {
    e match {
      case ConvertFailure(UnknownKey(key), _, path) => {
        logger.error(f"Unknown key '${key}' in element ${path}.")
      }
      case other => {
        logger.error(other.description)
      }
    }

  }

  def extractMasterConfig(parsingResults: Result[MasterConfig]): MasterConfig = {
    parsingResults match {
      case Left(errors) => {
        logger.error("Errors during parsing.")
        for (e <- errors.toList) {
          prettyPrintError(e)
        }
        throw new IllegalArgumentException(errors.toString())
      }
      case Right(master) => {
        logger.info(f"Successfully parsed master configuration.")
        PPrint.pprint(master, l=logger)
        master
      }
    }
  }

  def parse(config: MasterConfig): MasterModel = {
    config match {
      case MasterConfig(name, scenario, initialization, cosimStep) => {
        val scenarioModel = parse(scenario)
        val instantiationModel = generateInstantiationInstructions(scenarioModel).toList
        val initializationModel = initialization.map(instruction => parse(instruction, scenarioModel))
        val expandedInitModel = generateEnterInitInstructions(scenarioModel) ++ initializationModel ++ generateExitInitInstructions(scenarioModel)
        val cosimStepModel = cosimStep.map(instructions => (instructions._1, instructions._2.map(instruction => parse(instruction, scenarioModel))))
        val terminateModel = generateTerminateInstructions(scenarioModel).toList
        MasterModel(name, scenarioModel, instantiationModel, expandedInitModel.toList, cosimStepModel, terminateModel)
      }
    }
  }

  def generateEnterInitInstructions(model: ScenarioModel): immutable.Iterable[InitializationInstruction] = model.fmus.map(f => EnterInitMode(f._1))

  def generateExitInitInstructions(model: ScenarioModel): immutable.Iterable[InitializationInstruction] = model.fmus.map(f => ExitInitMode(f._1))

  def generateInstantiationInstructions(model: ScenarioModel): immutable.Iterable[InstantiationInstruction] =
    model.fmus.map(f => Instantiate(f._1)) ++ model.fmus.map(f => SetupExperiment(f._1))

  def generateTerminateInstructions(model: ScenarioModel): immutable.Iterable[TerminationInstruction] =
    model.fmus.map(f => Terminate(f._1)) ++
      model.fmus.map(f => FreeInstance(f._1)) ++
      model.fmus.map(f => Unload(f._1))


  def parsePortRef(str: String, context: String, fmus: Map[String, FmuModel]): PortRef = {
    val pRes = FMURefParserSingleton.parse(FMURefParserSingleton.fmu_port_ref, str)
    assert(pRes.successful, s"Problem parsing fmu port reference $str in $context.")
    assert(fmus.contains(pRes.get.fmu), s"Unable to resolve fmu ${pRes.get.fmu} in $context.")
    pRes.get
  }

  def parse(instruction: InitializationStatement, scenarioModel: ScenarioModel): InitializationInstruction = {
    // Check uniqueness of instruction
    instruction match {
      case NestedInitStatement(get, set) => {
        val nOps = List(get, set).count(b => !b.isBlank)
        assert(nOps == 1,
          s"Initialization instruction $instruction must be one of get, set")
      }
      case RootInitStatement(get, set, loop) => {
        val baseOps = List(get, set).count(b => !b.isBlank)
        val nOps = baseOps + (if (loop == NoLoopInit) 0 else 1)
        assert(nOps == 1,
          s"Initialization instruction $instruction must be one of get, set or loop-init.")
      }
    }
    if (!instruction.get.isBlank) {
      val pRef = parsePortRef(instruction.get, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).outputs.contains(pRef.port), s"Unable to resolve output port ${pRef.port} in fmu ${pRef.fmu}.")
      InitGet(pRef)
    } else if (!instruction.set.isBlank) {
      val pRef = parsePortRef(instruction.set, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).inputs.contains(pRef.port), s"Unable to resolve input port ${pRef.port} in fmu ${pRef.fmu}.")
      InitSet(pRef)
    } else {
      // Check subclasses
      instruction match {
        case NestedInitStatement(_, _) => {
          throw new RuntimeException("Invariant violated while parsing instruction $instruction")
        }

        case RootInitStatement(_, _, loop) => {
          assert(loop != NoLoopInit, s"Invariant violated while parsing instruction $instruction")
          parse(loop, scenarioModel)
        }
      }
    }
  }


    def parse(instruction: StepStatement, scenarioModel: ScenarioModel): CosimStepInstruction = {
    // Check uniqueness of instruction
    instruction match {
      case NestedStepStatement(get, set, step, saveState, restoreState, loop, getTentative, setTentative, by, bySameAs) => {
        val baseOps = List(get, set, step, saveState, restoreState, getTentative, setTentative).count(b => !b.isBlank)
        val nOps = baseOps + (if (loop == NoLoop) 0 else 1)
        assert(nOps == 1,
          s"Cosim step instruction $instruction must be one of get, set, step, save-state, restore-state, loop, or get-tentative.")
      }
      case RootStepStatement(get, set, step, saveState, restoreState, loop, by, bySameAs) => {
        val baseOps = List(get, set, step, saveState, restoreState).count(b => !b.isBlank)
        val nOps = baseOps + (if (loop == NoLoop) 0 else 1)
        assert(nOps == 1,
          s"Cosim step instruction $instruction must be one of get, set, step, save-state, restore-state, or loop.")
      }
    }

    if (!instruction.get.isBlank) {
      val pRef = parsePortRef(instruction.get, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).outputs.contains(pRef.port), s"Unable to resolve output port ${pRef.port} in fmu ${pRef.fmu}.")
      Get(pRef)
    } else if (!instruction.set.isBlank) {
      val pRef = parsePortRef(instruction.set, s"instruction $instruction", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).inputs.contains(pRef.port), s"Unable to resolve input port ${pRef.port} in fmu ${pRef.fmu}.")
      Set(pRef)
    } else if (!instruction.step.isBlank) {
      val fmuRef = instruction.step
      assert(scenarioModel.fmus.contains(fmuRef), s"Unable to resolve fmu ${fmuRef}.")
      if (instruction.by >= 0) {
        assert(instruction.bySameAs.isBlank, "Only one of by or by-same-as is allowed.")
        assert(instruction.by <= scenarioModel.maxPossibleStepSize,
          s"Cannot take step (${instruction.by}) larger than the specified max-possible-step-size (${scenarioModel.maxPossibleStepSize}).")
        Step(fmuRef, AbsoluteStepSize(instruction.by))
      } else if (!instruction.bySameAs.isBlank) {
        assert(instruction.by < 0, "Only one of by or by-same-as is allowed.")
        val fmuStepRef = instruction.bySameAs
        assert(scenarioModel.fmus.contains(fmuRef), s"Unable to resolve fmu ${fmuStepRef}.")
        Step(fmuRef, RelativeStepSize(fmuStepRef))
      } else {
        Step(fmuRef, DefaultStepSize())
      }
    } else if (!instruction.saveState.isBlank) {
      val fmuRef = instruction.saveState
      assert(scenarioModel.fmus.contains(fmuRef), s"Unable to resolve fmu ${fmuRef}.")
      SaveState(fmuRef)
    } else if (!instruction.restoreState.isBlank) {
      val fmuRef = instruction.restoreState
      assert(scenarioModel.fmus.contains(fmuRef), s"Unable to resolve fmu ${fmuRef}.")
      RestoreState(fmuRef)
    } else {
      // Check subclasses
      instruction match {
        case NestedStepStatement(_, _, _, _, _, loop, getTentative, setTentative, by, bySameAs) => {
          if (!getTentative.isBlank) {
            val pRef = parsePortRef(getTentative, s"instruction $instruction", scenarioModel.fmus)
            assert(scenarioModel.fmus(pRef.fmu).outputs.contains(pRef.port), s"Unable to resolve output port ${pRef.port} in fmu ${pRef.fmu}.")
            GetTentative(pRef)
          } else if (!setTentative.isBlank) {
            val pRef = parsePortRef(setTentative, s"instruction $instruction", scenarioModel.fmus)
            assert(scenarioModel.fmus(pRef.fmu).inputs.contains(pRef.port), s"Unable to resolve input port ${pRef.port} in fmu ${pRef.fmu}.")
            SetTentative(pRef)
          } else if (loop != NoLoop) {
            parse(loop, scenarioModel)
          } else {
            throw new RuntimeException(f"Invariant violated while parsing instruction $instruction")
          }
        }
        case RootStepStatement(_, _, _, _, _, loop, by, bySameAs) => {
          assert(loop != NoLoop, s"Invariant violated while parsing instruction $instruction")
          parse(loop, scenarioModel)
        }
      }
    }
  }

  def parse(loop: LoopConfig, scenarioModel: ScenarioModel): CosimStepInstruction = {
    assert((!loop.untilConverged.isEmpty) || (!loop.untilStepAccept.isEmpty), "Loop has to have either until-converged or until-step-accept.")
    assert(loop.untilConverged.isEmpty || loop.untilStepAccept.isEmpty, "Loop has to have either until-converged or until-step-accept, but not both.")
    if (! loop.untilConverged.isEmpty){
      val untilConverged = loop.untilConverged.map(p => {
        val pRef = parsePortRef(p, s"instruction $loop", scenarioModel.fmus)
          assert(scenarioModel.fmus(pRef.fmu).outputs.contains(pRef.port), s"Unable to resolve output port ${pRef.port} in fmu ${pRef.fmu}.")
          pRef
        })
      val iterate = loop.iterate.map(s => parse(s, scenarioModel))
      val ifRetryNeeded = loop.ifRetryNeeded.map(s => parse(s, scenarioModel))
      AlgebraicLoop(untilConverged, iterate, ifRetryNeeded)
    } else {
      val untilStepAccept = loop.untilStepAccept
      val iterate = loop.iterate.map(s => parse(s, scenarioModel))
      val ifRetryNeeded = loop.ifRetryNeeded.map(s => parse(s, scenarioModel))
      StepLoop(untilStepAccept, iterate, ifRetryNeeded)
    }
  }

  def parse(loop: LoopInitConfig, scenarioModel: ScenarioModel): InitializationInstruction = {
    val untilConverged = loop.untilConverged.map(p => {
      val pRef = parsePortRef(p, s"instruction $loop", scenarioModel.fmus)
      assert(scenarioModel.fmus(pRef.fmu).outputs.contains(pRef.port), s"Unable to resolve output port ${pRef.port} in fmu ${pRef.fmu}.")
      pRef
    })
    val iterate = loop.iterate.map(s => parse(s, scenarioModel))
    AlgebraicLoopInit(untilConverged, iterate)
  }

  def parseAdaptiveConfig(configuration: AdaptiveConfig, fmus: Map[String, FmuModel]): AdaptiveModel = {
    val configurableInputs = configuration.configurableInputs.map(p => {
      val pRef = parsePortRef(p, s"instruction ", fmus)
      assert(fmus(pRef.fmu).inputs.contains(pRef.port), s"Unable to resolve input port ${pRef.port} in fmu ${pRef.fmu}.")
      pRef
    })
    val configurationModels = configuration.configurations.map(keyValues => {
      val inputs = keyValues._2.inputs.map(p => {
        val pRef = configurableInputs.filter(_.port == p._1).head
        (pRef, parse(p._2))
      })
      (keyValues._1, ConfigurationModel(inputs, keyValues._2.cosimStep))
    })

    AdaptiveModel(configurableInputs = configurableInputs, configurations = configurationModels)
  }

  def parse(scenario: ScenarioConfig): ScenarioModel = {
    scenario match {
      case ScenarioConfig(fmus, configuration, connections, maxPossibleStepSize) => {
        assert(maxPossibleStepSize > 0, "Max possible step size has to be greater than 0 in scenario configuration.")
        val fmusModel = fmus.map(keyValPair => (keyValPair._1, parse(keyValPair._1, keyValPair._2)))
        val connectionsModel = connections.map((c) => parseConnection(c, fmusModel))
        val configurationModel =
          if (configuration.isDefined)
          parseAdaptiveConfig(configuration.get, fmusModel)
          else
            parseAdaptiveConfig(AdaptiveConfig(Nil, Map.empty), fmusModel)

        core.ScenarioModel(fmusModel, configurationModel, connectionsModel, maxPossibleStepSize)
      }
    }
  }

  def parse(fmuId: String, fmu: FmuConfig): FmuModel = {
    fmu match {
      case FmuConfig(inputs, outputs, canRejectStep) => {
        val inputsModel = inputs.map(keyValPair => (keyValPair._1, parse(keyValPair._2)))
        val outputPortsModel = outputs.map(keyValPair => (keyValPair._1, parse(keyValPair._2, inputsModel, keyValPair._1, fmuId)))
        FmuModel(inputsModel, outputPortsModel, canRejectStep)
      }
    }
  }

  def parse(config: InputPortConfig): InputPortModel = {
    config match {
      case InputPortConfig(reactivity) => InputPortModel(Reactivity.withName(reactivity))
    }
  }

  def parse(config: OutputPortConfig, inputsModel: Map[String, InputPortModel], outputPortId: String, fmuId: String): OutputPortModel = {
    config match {
      case OutputPortConfig(dependenciesInit, dependencies) => {
        val errorCheck = (inputPortRef: String) => assert(inputsModel.contains(inputPortRef),
          f"Unable to resolve input port reference $inputPortRef in the output port $outputPortId FMU $fmuId.")
        dependenciesInit.foreach(errorCheck)
        dependencies.foreach(errorCheck)

        OutputPortModel(dependenciesInit, dependencies)
      }
    }
  }

  def parseConnection(connectionConfig: String, fmus: Map[String, FmuModel]): ConnectionModel = {
    val results = ConnectionParserSingleton.parse(ConnectionParserSingleton.connection, connectionConfig)
    assert(results.successful, s"Problem parsing connection string $connectionConfig.")
    val connection = results.get
    assert(fmus.contains(connection.srcPort.fmu), s"Unable to resolve source fmu ${connection.srcPort.fmu} in connection $connectionConfig.")
    assert(fmus.contains(connection.trgPort.fmu), s"Unable to resolve source fmu ${connection.trgPort.fmu} in connection $connectionConfig.")

    val srcFmu = fmus(connection.srcPort.fmu)
    val trgFmu = fmus(connection.trgPort.fmu)

    assert(srcFmu.outputs.contains(connection.srcPort.port),
      s"Unable to resolve source port ${connection.srcPort.port} of fmu ${connection.srcPort.fmu} in connection $connectionConfig.")
    assert(trgFmu.inputs.contains(connection.trgPort.port),
      s"Unable to resolve source port ${connection.trgPort.port} of fmu ${connection.trgPort.fmu} in connection $connectionConfig.")
    connection
  }

  def assert(condition: Boolean, msg: String) = {
    if (!condition) {
      logger.error(msg)
      throw new IllegalArgumentException(msg)
    }
  }
}
