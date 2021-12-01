package api

import java.io.{File, PrintWriter}
import java.nio.file.Files

import cli.VerifyTA
import core.ScenarioLoader.{generateEnterInitInstructions, generateExitInitInstructions, generateInstantiationInstructions, generateTerminateInstructions, parse}
import core.{AdaptiveModel, AlgebraicLoop, AlgebraicLoopInit, ConfigurationModel, ConnectionModel, CosimStepInstruction, EnterInitMode, ExitInitMode, FmuModel, Get, GetTentative, InitGet, InitSet, InitializationInstruction, MasterModel, ModelEncoding, NoOP, OutputPortModel, PortRef, RestoreState, SaveState, ScenarioGenerator, ScenarioLoader, ScenarioModel, SetTentative, Step, StepLoop}
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.scala.Logging
import synthesizer.SynthesizerSimple
import trace_analyzer.TraceAnalyzer

object GenerationAPI {

  def generateInitialization(scenarioModel: ScenarioModel) = {
    val synthesizer = new SynthesizerSimple(scenarioModel)
    synthesizer.synthesizeInitialization()
  }

  def generatecoSimStep(scenarioModel: ScenarioModel) = {
    val synthesizer = new SynthesizerSimple(scenarioModel)
    synthesizer.synthesizeStep()
  }

  def generateAlgorithm(name: String, scenarioModel: ScenarioModel): MasterModel = {
    val instantiationModel = generateInstantiationInstructions(scenarioModel).toList
    val expandedInitModel = generateEnterInitInstructions(scenarioModel) ++ generateInitialization(scenarioModel) ++ generateExitInitInstructions(scenarioModel)
    val cosimStepModel = generatecoSimStep(scenarioModel)
    val terminateModel = generateTerminateInstructions(scenarioModel).toList
    MasterModel(name, scenarioModel, instantiationModel, expandedInitModel.toList, cosimStepModel, terminateModel)
  }
}


object VerificationAPI extends Logging {
  private def writeToTempFile(content: String) = {
    val file = Files.createTempFile("uppaal_", ".xml").toFile
    new PrintWriter(file) {
      write(content);
      close()
    }
    file
  }

  def verifyAlgorithm(masterModel: MasterModel): Boolean = {
    val f = generateUppaalFile(masterModel)
    checkUppaalVersion()
    val verificationResult = VerifyTA.verify(f)
    //FileUtils.deleteQuietly(f)

    checkVerificationResult(verificationResult)
  }

  def generateAndVerify(name: String, scenarioModel: ScenarioModel) = {
    val masterModel = GenerationAPI.generateAlgorithm(name, scenarioModel)
    verifyAlgorithm(masterModel)
  }

  def generateTrace(name: String, scenarioModel: ScenarioModel): TraceResult = {
    val masterModel = GenerationAPI.generateAlgorithm(name, scenarioModel)
    generateTraceFromMasterModel(masterModel)
  }

  def sanitizePort(port: PortRef): PortRef = {
    PortRef(port.fmu, port.port.replaceAll("\\W", ""))
  }

  def sanitizeName(act: InitializationInstruction): InitializationInstruction = {
    act match {
      case InitSet(port) => InitSet(sanitizePort(port))
      case InitGet(port) => InitGet(sanitizePort(port))
      case EnterInitMode(fmu) => EnterInitMode(fmu)
      case ExitInitMode(fmu) => ExitInitMode(fmu)
      case AlgebraicLoopInit(untilConverged, iterate) => AlgebraicLoopInit(untilConverged.map(i => sanitizePort(i)), iterate.map(i => sanitizeName(i)))
    }
  }


  def sanitizeConnection(c: ConnectionModel): ConnectionModel = ConnectionModel(sanitizePort(c.srcPort), sanitizePort(c.trgPort))

  def sanitizeAction(act: CosimStepInstruction): CosimStepInstruction = {
    act match {
      case core.Set(port) => core.Set(sanitizePort(port))
      case Get(port) => core.Get(sanitizePort(port))
      case GetTentative(port) => GetTentative(sanitizePort(port))
      case SetTentative(port) => SetTentative(sanitizePort(port))
      case AlgebraicLoop(untilConverged, iterate, ifRetryNeeded) => AlgebraicLoop(untilConverged.map(sanitizePort), iterate.map(sanitizeAction), ifRetryNeeded)
      case StepLoop(untilStepAccept, iterate, ifRetryNeeded) => StepLoop(untilStepAccept, iterate.map(sanitizeAction), ifRetryNeeded)
      case Step(fmu, by) => Step(fmu, by)
      case SaveState(fmu) => SaveState(fmu)
      case RestoreState(fmu) => RestoreState(fmu)
      case NoOP => NoOP
    }
  }

  private def generateUppaalFile(masterModel: MasterModel): File = {
    val sanitizedModel: MasterModel = sanitizeMasterModel(masterModel)
    val encoding = new ModelEncoding(sanitizedModel)
    val encodedUppaal = ScenarioGenerator.generate(encoding)

    writeToTempFile(encodedUppaal)
  }

  private def sanitizeMasterModel(masterModel: MasterModel) = {
    val connections = masterModel.scenario.connections.map(sanitizeConnection)
    val config =
      AdaptiveModel(
        masterModel.scenario.config.configurableInputs.map(sanitizePort),
        masterModel.scenario.config.configurations.map(i => (i._1,
          ConfigurationModel(
            i._2.inputs.map(input => (sanitizePort(input._1), input._2)),
            i._2.cosimStep,
            i._2.connections.map(sanitizeConnection)
          )
        )))
    val fmus = masterModel.scenario.fmus.map(
      fmu =>
        (fmu._1, FmuModel(
          fmu._2.inputs.map(i => (i._1.replaceAll("\\W", ""), i._2)),
          fmu._2.outputs.map(i => (i._1.replaceAll("\\W", ""),
            OutputPortModel(
              i._2.dependenciesInit.map(o => o.replaceAll("\\W", "")),
              i._2.dependencies.map(o => o.replaceAll("\\W", ""))
            )
          )),
          fmu._2.canRejectStep,
          fmu._2.path
        )))

    val scenario = ScenarioModel(fmus, config, connections, masterModel.scenario.maxPossibleStepSize)

    val initActions = masterModel.initialization.map(sanitizeName)
    val actions = masterModel.cosimStep.map(act => (act._1, act._2.map(sanitizeAction)))

    val sanitizedModel = MasterModel(masterModel.name, scenario, masterModel.instantiation, initActions, actions, masterModel.terminate)
    sanitizedModel
  }

  def generateTraceFromMasterModel(masterModel: MasterModel): TraceResult = {
    if (verifyAlgorithm(masterModel))
      return TraceResult(null, false)

    val f = generateUppaalFile(masterModel)
    val encoding = new ModelEncoding(masterModel)
    val traceFile = Files.createTempFile("trace_", ".log").toFile
    val videoFile = Files.createTempFile("trace_", ".mp4").toFile

    VerifyTA.saveTraceToFile(f, traceFile)
    FileUtils.deleteQuietly(f)
    val source = scala.io.Source.fromFile(traceFile)
    try {
      val lines = source.getLines()
      TraceAnalyzer.AnalyseScenario(masterModel.name, lines, encoding, videoFile)
    }
    finally source.close()
    FileUtils.deleteQuietly(traceFile)
    TraceResult(videoFile, true)
  }

  def checkUppaalVersion() = {
    if (!VerifyTA.checkEnvironment())
      throw UppaalException("UPPAAL v.4.1 is not in PATH - please install it and try again!")
  }

  def checkVerificationResult(verificationResult: Int) = {
    verificationResult match {
      case 0 => true
      case 2 => throw SyntaxException("The verification in Uppaal failed most likely due to a syntax error in the UPPAAL model.")
      case _ => false
    }
  }
}

final case class TraceResult(file: File, isGenerated: Boolean)

final case class SyntaxException(private val message: String = "",
                                 private val cause: Throwable = None.orNull)
  extends Exception(message, cause)

final case class UppaalException(private val message: String = "",
                                 private val cause: Throwable = None.orNull)
  extends Exception(message, cause)
