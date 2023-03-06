package api

import cli.VerifyTA
import core._
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.scala.Logging
import trace_analyzer.TraceAnalyzer
import java.io.{File, PrintWriter}
import java.nio.file.Files

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
    ensureUppaalInstalled()
    val verificationResult = VerifyTA.verify(f)
    //FileUtils.deleteQuietly(f)

    checkVerificationResult(verificationResult)
  }

  def synthesizeAndVerify(name: String, scenarioModel: ScenarioModel): Boolean = {
    val masterModel = GenerationAPI.synthesizeAlgorithm(name, scenarioModel)
    verifyAlgorithm(masterModel)
  }

  def generateTrace(name: String, scenarioModel: ScenarioModel): TraceResult = {
    val masterModel = GenerationAPI.synthesizeAlgorithm(name, scenarioModel)
    generateTraceFromMasterModel(masterModel)
  }

  private def sanitizePort(port: PortRef): PortRef = {
    PortRef(port.fmu, port.port.replaceAll("\\W", ""))
  }

  private def sanitizeName(act: InitializationInstruction): InitializationInstruction = {
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
        (fmu._1, fmu._2.copy(
          inputs = fmu._2.inputs.map(i => (i._1.replaceAll("\\W", ""), i._2)),
          outputs = fmu._2.outputs.map(i => (i._1.replaceAll("\\W", ""),
            OutputPortModel(
              i._2.dependenciesInit.map(o => o.replaceAll("\\W", "")),
              i._2.dependencies.map(o => o.replaceAll("\\W", ""))
            ))))))

    val scenario = ScenarioModel(fmus, config, connections, masterModel.scenario.maxPossibleStepSize)

    val initActions = masterModel.initialization.map(sanitizeName)
    val actions = masterModel.cosimStep.map(act => (act._1, act._2.map(sanitizeAction)))

    masterModel.copy(
      initialization = initActions,
      cosimStep = actions,
      scenario = scenario,
    )
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

    val videoFilePath = try {
      val lines = source.getLines()
      TraceAnalyzer.AnalyseScenario(masterModel.name, lines, encoding, videoFile.toString)
    }
    finally source.close()
    FileUtils.deleteQuietly(traceFile)
    val fileVideo = new File(videoFilePath)
    TraceResult(fileVideo, true)
  }

  private def ensureUppaalInstalled() = {
    if (!VerifyTA.checkEnvironment())
      throw UppaalException("UPPAAL v.4.1 is not in PATH - please install it and try again!")
  }

  private def checkVerificationResult(verificationResult: Int): Boolean = {
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
