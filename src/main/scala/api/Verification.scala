package api

import cli.VerifyTA
import core._
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.scala.Logging
import trace_analyzer.{ScenarioPlotter, TraceAnalyzer, UppaalTrace}

import java.io.File
import java.nio.file.Files
import scala.reflect.io.Directory

object VerificationAPI extends Logging {
  /**
   * Verifies whether the algorithm is correct with respect to the scenario model.
   *
   * @param MasterModel the algorithm and scenario to verify
   * @return true if the algorithm is correct, false otherwise
   */
  def verifyAlgorithm(masterModel: MasterModel, uppaalFileType: (String, ModelEncoding, Directory) => File): Boolean = {
    require(VerifyTA.isInstalled, "Uppaal is not installed, please install it and add it to your PATH")
    val uppaalFile = generateUppaalFile(masterModel, uppaalFileType)
    val verificationResult = VerifyTA.verify(uppaalFile)
    checkVerificationResult(verificationResult)
  }


  def dynamicVerification(scenarioModel: ScenarioModel, previous_actions: List[CosimStepInstruction], next_action: CosimStepInstruction): Verdict = {
    require(VerifyTA.isInstalled, "Uppaal is not installed, please install it and add it to your PATH")
    val masterModel = GenerationAPI.synthesizeAlgorithm("dynamic_verification", scenarioModel).copy(
      cosimStep = Map("conf1" -> (previous_actions ++ List(next_action)))
    )
    val trace = generateTraceFromMasterModel(masterModel, ScenarioGenerator.generateDynamicUppaalFile)
    // Trace could not be generated because the algorithm is correct
    trace match {
      case None => Verdict(correct = true, Predef.Set.empty)
      case Some(t) =>
        logger.info("Trace generated")
        val enabledActions = t.getLastEnabledActions
        Verdict(correct = false, enabledActions)
    }
  }

  /**
   * Synthesize an orchestration algorithm and verify it with respect to the scenario model.
   *
   * @param ScenarioModel the algorithm and scenario to verify
   * @return true if the algorithm is correct, false otherwise
   */
  def synthesizeAndVerify(name: String, scenarioModel: ScenarioModel): Boolean = {
    val masterModel = GenerationAPI.synthesizeAlgorithm(name, scenarioModel)
    verifyAlgorithm(masterModel, ScenarioGenerator.generateUppaalFile)
  }


  def generateTrace(name: String, scenarioModel: ScenarioModel): TraceResult = {
    val masterModel = GenerationAPI.synthesizeAlgorithm(name, scenarioModel)
    generateTraceVideo(masterModel)
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

  private def sanitizeAction(act: CosimStepInstruction): CosimStepInstruction = {
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

  private def generateUppaalFile(masterModel: MasterModel, uppaalFileType: (String, ModelEncoding, Directory) => File): File = {
    val sanitizedModel: MasterModel = sanitizeMasterModel(masterModel)
    val encoding = new ModelEncoding(sanitizedModel)
    val currentFolder = new File(System.getProperty("user.dir"))
    uppaalFileType(sanitizedModel.name, encoding, Directory(currentFolder))
  }


  //Todo - check if this is needed
  private def sanitizeMasterModel(masterModel: MasterModel) = {
    val config =
      AdaptiveModel(
        masterModel.scenario.config.configurableInputs.map(sanitizePort),
        masterModel.scenario.config.configurations.map(i => (i._1,
          ConfigurationModel(
            i._2.inputs.map(input => (sanitizePort(input._1), input._2)),
            i._2.cosimStep,
            i._2.connections
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

    val scenario = ScenarioModel(fmus, config, masterModel.scenario.connections, masterModel.scenario.maxPossibleStepSize)

    val initActions = masterModel.initialization.map(sanitizeName)
    val actions = masterModel.cosimStep.map(act => (act._1, act._2.map(sanitizeAction)))

    masterModel.copy(
      initialization = initActions,
      cosimStep = actions,
      scenario = scenario,
    )
  }

  private def generateTraceFromMasterModel(masterModel: MasterModel, uppaalFileType: (String, ModelEncoding, Directory) => File): Option[UppaalTrace] = {
    if (verifyAlgorithm(masterModel, uppaalFileType)) {
      // If the algorithm is correct, we don't need to generate a trace
      return None
    }

    val f = generateUppaalFile(masterModel, uppaalFileType)
    val encoding = new ModelEncoding(masterModel)
    val traceFile = Files.createTempFile("trace_", ".log").toFile
    VerifyTA.saveTraceToFile(f, traceFile)
    FileUtils.deleteQuietly(f)

    val source = scala.io.Source.fromFile(traceFile)
    val trace = try {
      val lines = source.getLines()
      TraceAnalyzer.createUppaalTrace(masterModel.name, lines, encoding)
    } finally source.close()

    FileUtils.deleteQuietly(traceFile)
    Some(trace)
  }

  def generateTraceVideo(masterModel: MasterModel): TraceResult = {
    val uppaalTrace = generateTraceFromMasterModel(masterModel, ScenarioGenerator.generateUppaalFile)
    if (uppaalTrace.isEmpty)
      return TraceResult(null, isGenerated = false)

    val traceResult = try {
      val outputDirectory = new File(System.getProperty("user.dir"))
      val videoFilePath = ScenarioPlotter.plot(uppaalTrace.get, outputDirectory.getPath)
      TraceResult(new File(videoFilePath), isGenerated = true)
    }
    catch {
      case e: Exception =>
        println("Failed to generate video trace")
        TraceResult(null, isGenerated = false)
    }
    traceResult
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
