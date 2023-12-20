package org.intocps.verification.scenarioverifier.core

import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths

import scala.annotation.unused
import scala.collection.immutable

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier.core.masterModel._
import org.intocps.verification.scenarioverifier.core.FMI3.AdaptiveModel
import org.intocps.verification.scenarioverifier.core.FMI3.ConfigurationModel
import org.intocps.verification.scenarioverifier.prettyprint.PPrint
import pureconfig.error.ConfigReaderFailure
import pureconfig.error.ConvertFailure
import pureconfig.error.UnknownKey
import pureconfig.generic.auto._
import pureconfig.generic.ProductHint
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.generic.auto._

trait ScenarioLoader[A <: MasterModel, B] extends Logging {
  def parse(config: B): A

  private def extractMasterConfig(parsingResults: Result[B]): B = {
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

  private def parseConfig(conf: Config): A = {
    val parsingResults = ConfigSource.fromConfig(conf).load[B]
    val masterConfig = extractMasterConfig(parsingResults)
    configHints()
    parse(masterConfig)
  }

  def load(file: String): A = {
    if (!Files.exists(Paths.get(file))) {
      val msg = f"File not found: $file. Current working directory is: ${System.getProperty("user.dir")}."
      logger.error(msg)
      throw new IllegalArgumentException(msg)
    }
    val conf = ConfigFactory.parseFile(new File(file))
    parseConfig(conf)
  }

  def load(stream: InputStream): A = {
    val reader = new InputStreamReader(stream)
    try {
      // read from stream
      val conf = ConfigFactory.parseReader(reader)
      parseConfig(conf)
    } finally {
      reader.close()
    }
  }

  protected def prettyPrintError(e: ConfigReaderFailure): Unit = {
    e match {
      case ConvertFailure(UnknownKey(key), _, path) =>
        logger.error(f"Unknown key '$key' in element $path.")
      case other =>
        logger.error(other.description)
    }
  }

  protected def parsePortRefBase(str: String, context: String, fmus: Map[String, FmuModel]): PortRef = {
    val pRes = FMURefParserSingleton.parse(FMURefParserSingleton.fmu_port_ref, str)
    assert(pRes.successful, s"Problem parsing fmu port reference $str in $context.")
    assert(fmus.contains(pRes.get.fmu), s"Unable to resolve fmu ${pRes.get.fmu} in $context.")
    val pRef = pRes.get
    pRef
  }

  protected def parsePortRef[F <: FmuModel](
      str: String,
      context: String,
      fmus: Map[String, F],
      portProjection: F => Map[String, PortModel]): PortRef = {
    val pRef = parsePortRefBase(str, context, fmus)
    assert(portProjection(fmus(pRef.fmu)).contains(pRef.port), s"Unable to resolve port ${pRef.port} in fmu ${pRef.fmu}.")
    pRef
  }

  protected def parsePortAction[F <: FmuModel](
      instruction: String,
      fmus: Map[String, F],
      actionConstructor: PortRef => PortAction,
      portProjection: F => Map[String, PortModel]): PortAction = {
    val pRef = parsePortRef(instruction, s"instruction $instruction", fmus, portProjection)
    actionConstructor(pRef)
  }

  /*
  protected def parseFMUAction(
      instruction: String,
      fmus: Map[String, FmuModel],
      actionConstructor: String => FMUAction): FMUAction = {

    val fmuRef = instruction
    assert(fmus.contains(fmuRef), s"Unable to resolve fmu $fmuRef.")
    actionConstructor(fmuRef)
  }
   */

  protected def parseConnection(connectionConfig: String, fmus: Map[String, FmuModel]): ConnectionModel = {
    val results = ConnectionParserSingleton.parse(ConnectionParserSingleton.connection, connectionConfig)
    assert(results.successful, s"Problem parsing connection string $connectionConfig.")
    val connection = results.get
    assert(
      fmus.contains(connection.srcPort.fmu),
      s"Unable to resolve source fmu ${connection.srcPort.fmu} in connection $connectionConfig.")
    assert(
      fmus.contains(connection.trgPort.fmu),
      s"Unable to resolve source fmu ${connection.trgPort.fmu} in connection $connectionConfig.")
    connection
  }

  def generateEnterInitInstructions(model: ScenarioModel): immutable.Iterable[InitializationInstruction] =
    model.fmus.map(f => EnterInitMode(f._1))

  def generateExitInitInstructions(model: ScenarioModel): immutable.Iterable[InitializationInstruction] =
    model.fmus.map(f => ExitInitMode(f._1))

  def generateInstantiationInstructions(model: ScenarioModel): immutable.Iterable[InstantiationInstruction] =
    model.fmus.map(f => Instantiate(f._1)) ++ model.fmus.map(f => SetupExperiment(f._1))

  def generateTerminateInstructions(model: ScenarioModel): immutable.Iterable[TerminationInstruction] =
    model.fmus.map(f => Terminate(f._1)) ++
      model.fmus.map(f => FreeInstance(f._1)) ++
      model.fmus.map(f => Unload(f._1))

  protected def configHints(): Unit
}

object ScenarioLoaderFMI2 extends ScenarioLoader[MasterModelFMI2, MasterConfig] {

  protected override def configHints(): Unit = {
    @unused
    implicit val hintNestedStepStatement: ProductHint[NestedStepStatement] = ProductHint[NestedStepStatement](allowUnknownKeys = false)
    @unused
    implicit val hintRootStepStatement: ProductHint[RootStepStatement] = ProductHint[RootStepStatement](allowUnknownKeys = false)
    @unused
    implicit val hintNestedInitStatement: ProductHint[NestedInitStatement] = ProductHint[NestedInitStatement](allowUnknownKeys = false)
    @unused
    implicit val hintRootInitStatement: ProductHint[RootInitStatement] = ProductHint[RootInitStatement](allowUnknownKeys = false)
    @unused
    implicit val hintMasterConfig: ProductHint[MasterConfig] = ProductHint[MasterConfig](allowUnknownKeys = false)
  }

  /*
   * Remove all ports that are not connected to any other port.
   * This is done to simplify the orchestration algorithm.
   */
  def simplifyScenario(model: FMI2ScenarioModel): FMI2ScenarioModel = {
    val connectedPorts = model.connections.flatMap(c => List(c.srcPort, c.trgPort))
    val fmus = model.fmus.map(fmu => {
      // Remove all ports that are not connected to any other port
      val connectedOutputsPorts = fmu._2.outputs.filter(p => connectedPorts.contains(PortRef(fmu._1, p._1)))
      val connectedInputsPorts = fmu._2.inputs.filter(p => connectedPorts.contains(PortRef(fmu._1, p._1)))
      val connectedOutputsPortsWithConnectedFeedthroguh = connectedOutputsPorts.map(p =>
        p._1 -> FMI2OutputPortModel(
          p._2.dependenciesInit.filter(d => connectedInputsPorts.contains(d)),
          p._2.dependencies.filter(d => connectedInputsPorts.contains(d))))
      fmu.copy(_2 = fmu._2.copy(outputs = connectedOutputsPortsWithConnectedFeedthroguh, inputs = connectedInputsPorts))
    })
    model.copy(fmus = fmus)
  }

  def parse(config: MasterConfig): MasterModelFMI2 = {
    config match {
      case MasterConfig(name, scenario, initialization, cosimStep) =>
        val scenarioModel = parse(scenario)
        val instantiationModel = generateInstantiationInstructions(scenarioModel).toList
        val initializationModel = initialization.map(instruction => parse(instruction, scenarioModel))
        val expandedInitModel =
          generateEnterInitInstructions(scenarioModel) ++ initializationModel ++ generateExitInitInstructions(scenarioModel)
        val cosimStepModel =
          cosimStep.map(instructions => (instructions._1, instructions._2.map(instruction => parse(instruction, scenarioModel))))
        val terminateModel = generateTerminateInstructions(scenarioModel).toList
        MasterModelFMI2(name, scenarioModel, instantiationModel, expandedInitModel.toList, cosimStepModel, terminateModel)
    }
  }

  def parse(instruction: InitializationStatement, scenarioModel: FMI2ScenarioModel): InitializationInstruction = {
    // Check uniqueness of instruction
    instruction match {
      case NestedInitStatement(get, set) =>
        val nOps = List(get, set).count(b => !b.isBlank)
        assert(nOps == 1, s"Initialization instruction $instruction must be one of get, set")
      case RootInitStatement(get, set, loop) =>
        val baseOps = List(get, set).count(b => b.nonEmpty && !b.isBlank)
        val nOps = baseOps + (if (loop == NoLoopInit) 0 else 1)
        assert(nOps == 1, s"Initialization instruction $instruction must be one of get, set or loop-init.")
    }
    if (!instruction.get.isBlank) {
      parsePortAction[Fmu2Model](instruction.get, scenarioModel.fmus, InitGet, _.outputs)
    } else if (!instruction.set.isBlank) {
      parsePortAction[Fmu2Model](instruction.set, scenarioModel.fmus, InitSet, _.inputs)
    } else {
      // Check subclasses
      instruction match {
        case NestedInitStatement(_, _) =>
          throw new RuntimeException(s"Invariant violated while parsing instruction $instruction")
        case RootInitStatement(_, _, loop) =>
          assert(loop != NoLoopInit, s"Invariant violated while parsing instruction $instruction")
          parse(loop, scenarioModel)
      }
    }
  }

  def parse(instruction: StepStatement, scenarioModel: FMI2ScenarioModel): CosimStepInstruction = {
    // Check uniqueness of instruction
    instruction match {
      case NestedStepStatement(get, set, step, saveState, restoreState, loop, getTentative, setTentative, _, _) =>
        val baseOps = List(get, set, step, saveState, restoreState, getTentative, setTentative).count(b => !b.isBlank)
        val nOps = baseOps + (if (loop == NoLoop) 0 else 1)
        assert(
          nOps == 1,
          s"Cosim step instruction $instruction must be one of get, set, step, save-state, restore-state, loop, or get-tentative.")
      case RootStepStatement(get, set, step, saveState, restoreState, loop, _, _) =>
        val baseOps = List(get, set, step, saveState, restoreState).count(b => !b.isBlank)
        val nOps = baseOps + (if (loop == NoLoop) 0 else 1)
        assert(nOps == 1, s"Cosim step instruction $instruction must be one of get, set, step, save-state, restore-state, or loop.")
    }

    if (!instruction.get.isBlank) {
      parsePortAction[Fmu2Model](instruction.get, scenarioModel.fmus, Get, _.outputs)
    } else if (!instruction.set.isBlank) {
      parsePortAction[Fmu2Model](instruction.set, scenarioModel.fmus, Set, _.inputs)
    } else if (!instruction.step.isBlank) {
      val fmuRef = instruction.step
      assert(scenarioModel.fmus.contains(fmuRef), s"Unable to resolve fmu $fmuRef.")
      if (instruction.by >= 0) {
        assert(instruction.bySameAs.isBlank, "Only one of by or by-same-as is allowed.")
        assert(
          instruction.by <= scenarioModel.maxPossibleStepSize,
          s"Cannot take step (${instruction.by}) larger than the specified max-possible-step-size (${scenarioModel.maxPossibleStepSize}).")
        Step(fmuRef, AbsoluteStepSize(instruction.by))
      } else if (!instruction.bySameAs.isBlank) {
        assert(instruction.by < 0, "Only one of by or by-same-as is allowed.")
        val fmuStepRef = instruction.bySameAs
        assert(scenarioModel.fmus.contains(fmuRef), s"Unable to resolve fmu $fmuStepRef.")
        Step(fmuRef, RelativeStepSize(fmuStepRef))
      } else {
        Step(fmuRef, DefaultStepSize())
      }
    } else if (!instruction.saveState.isBlank) {
      val fmuRef = instruction.saveState
      assert(scenarioModel.fmus.contains(fmuRef), s"Unable to resolve fmu $fmuRef.")
      SaveState(fmuRef)
    } else if (!instruction.restoreState.isBlank) {
      val fmuRef = instruction.restoreState
      assert(scenarioModel.fmus.contains(fmuRef), s"Unable to resolve fmu $fmuRef.")
      RestoreState(fmuRef)
    } else {
      // Check subclasses
      instruction match {
        case NestedStepStatement(_, _, _, _, _, loop, getTentative, setTentative, _, _) =>
          if (!getTentative.isBlank) {
            parsePortAction[Fmu2Model](getTentative, scenarioModel.fmus, GetTentative, _.outputs)
          } else if (!setTentative.isBlank) {
            parsePortAction[Fmu2Model](setTentative, scenarioModel.fmus, SetTentative, _.inputs)
          } else if (loop != NoLoop) {
            parse(loop, scenarioModel)
          } else {
            throw new RuntimeException(f"Invariant violated while parsing instruction $instruction")
          }
        case RootStepStatement(_, _, _, _, _, loop, _, _) =>
          assert(loop != NoLoop, s"Invariant violated while parsing instruction $instruction")
          parse(loop, scenarioModel)
      }
    }
  }

  def parse(loop: LoopConfigStep, scenarioModel: FMI2ScenarioModel): CosimStepInstruction = {
    assert(loop.untilConverged.nonEmpty || loop.untilStepAccept.nonEmpty, "Loop has to have either until-converged or until-step-accept.")
    assert(
      loop.untilConverged.isEmpty || loop.untilStepAccept.isEmpty,
      "Loop has to have either until-converged or until-step-accept, but not both.")
    val iterate = loop.iterate.map(s => parse(s, scenarioModel))
    val ifRetryNeeded = loop.ifRetryNeeded.map(s => parse(s, scenarioModel))
    if (loop.untilConverged.nonEmpty) {
      val untilConverged = loop.untilConverged.map(p => {
        parsePortRef[Fmu2Model](p, s"instruction $loop", scenarioModel.fmus, _.outputs)
      })
      AlgebraicLoop(untilConverged, iterate, ifRetryNeeded)
    } else {
      val untilStepAccept = loop.untilStepAccept
      StepLoop(untilStepAccept, iterate, ifRetryNeeded)
    }
  }

  def parse(loop: LoopConfigInit, scenarioModel: FMI2ScenarioModel): InitializationInstruction = {
    val untilConverged = loop.untilConverged.map(p => {
      parsePortRef[Fmu2Model](p, s"instruction $loop", scenarioModel.fmus, _.outputs)
    })
    val iterate = loop.iterate.map(s => parse(s, scenarioModel))
    AlgebraicLoopInit(untilConverged, iterate)
  }

  private def parseAdaptiveConfig(configuration: AdaptiveConfig, fmus: Map[String, Fmu2Model]): AdaptiveModel = {
    val configurableInputs = configuration.configurableInputs.map(p => {
      parsePortRef[Fmu2Model](p, s"instruction ", fmus, _.inputs)
    })
    val configurationModels = configuration.configurations.map(keyValues => {
      val inputs = keyValues._2.inputs.map(p => {
        val pRef = configurableInputs.filter(_.port == p._1).head
        (pRef, parse(p._2))
      })
      val connections = keyValues._2.connections.map(c => parseConnection(c, fmus))
      (keyValues._1, ConfigurationModel(inputs, keyValues._2.cosimStep, connections))
    })

    AdaptiveModel(configurableInputs = configurableInputs, configurations = configurationModels)
  }

  def parse(scenario: ScenarioConfig): FMI2ScenarioModel = {
    scenario match {
      case ScenarioConfig(fmus, configuration, connections, maxPossibleStepSize) =>
        assert(maxPossibleStepSize > 0, "Max possible step size has to be greater than 0 in scenario configuration.")
        val fmusModels: Map[String, Fmu2Model] = fmus.map(keyValPair => (keyValPair._1, parse(keyValPair._1, keyValPair._2)))
        val connectionsModel: List[ConnectionModel] = connections.map(c => parseConnection(c, fmusModels))
        val configurationModel: AdaptiveModel =
          if (configuration.isDefined)
            parseAdaptiveConfig(configuration.get, fmusModels)
          else
            parseAdaptiveConfig(AdaptiveConfig(Nil, Map.empty), fmusModels)
        FMI2ScenarioModel(fmusModels, configurationModel, connectionsModel, maxPossibleStepSize).enrich()
      case _ => throw new RuntimeException("Only FMI2 is supported.")
    }
  }

  def parse(fmuId: String, fmu: FmuConfig): Fmu2Model = {
    fmu match {
      case FmuConfig(inputs, outputs, canRejectStep, path) =>
        val inputsModel = inputs.map(keyValPair => (keyValPair._1, parse(keyValPair._2)))
        val outputPortsModel = outputs.map(keyValPair => (keyValPair._1, parse(keyValPair._2, inputsModel, keyValPair._1, fmuId)))
        Fmu2Model(inputsModel, outputPortsModel, canRejectStep, path)
    }
  }

  def parse(config: InputPortConfig): FMI2InputPortModel = {
    config match {
      case InputPortConfig(reactivity) => FMI2InputPortModel(Reactivity.withName(reactivity))
    }
  }

  def parse(
      config: org.intocps.verification.scenarioverifier.core.OutputPortConfig,
      inputsModel: Map[String, FMI2InputPortModel],
      outputPortId: String,
      fmuId: String): FMI2OutputPortModel = {
    config match {
      case org.intocps.verification.scenarioverifier.core.OutputPortConfig(dependenciesInit, dependencies) =>
        val errorCheck = (inputPortRef: String) =>
          assert(
            inputsModel.contains(inputPortRef),
            f"Unable to resolve input port reference $inputPortRef in the output port $outputPortId FMU $fmuId.")
        dependenciesInit.foreach(errorCheck)
        dependencies.foreach(errorCheck)
        FMI2OutputPortModel(dependenciesInit, dependencies)
    }
  }

}
