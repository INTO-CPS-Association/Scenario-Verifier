package org.intocps.verification.scenarioverifier.api

import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier
import org.intocps.verification.scenarioverifier.cli.VerifyTA
import org.intocps.verification.scenarioverifier.cli.Z3.SMTEncoder
import org.intocps.verification.scenarioverifier.core.{AdaptiveModel, AlgebraicLoop, AlgebraicLoopInit, ConfigurationModel, CosimStepInstruction, EnterInitMode, ExitInitMode, Get, GetTentative, InitGet, InitSet, InitializationInstruction, InstantiationInstruction, MasterModel, ModelEncoding, NoOP, OutputPortModel, PortRef, RestoreState, SaveState, ScenarioGenerator, ScenarioModel, Set, SetTentative, Step, StepLoop, TerminationInstruction}
import org.intocps.verification.scenarioverifier.traceanalyzer.{ScenarioPlotter, TraceAnalyzer, UppaalTrace}
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import scala.reflect.io.Directory

object VerificationAPI extends Logging {

  /**
   * Verifies whether the algorithm is correct with respect to the scenario model.
   *
   * @param masterModel the algorithm and scenario to verify
   * @return true if the algorithm is correct, false otherwise
   */
  def verifyAlgorithm(masterModel: MasterModel): Boolean = {
    verifyAlgorithm(masterModel, ScenarioGenerator.generateUppaalFile)
  }

  /**
   * Verifies whether the algorithm is correct with respect to the scenario model.
   *
   * @param masterModel    the algorithm and scenario to verify
   * @param uppaalFileType the type of Uppaal file to generate - either a normal Uppaal file or a dynamic Uppaal file
   * @return true if the algorithm is correct, false otherwise
   */
  private def verifyAlgorithm(masterModel: MasterModel, uppaalFileType: (String, ModelEncoding, Directory) => File, isOnlineMode: Boolean = false): Boolean = {
    if (!isOnlineMode) {
      require(VerifyTA.isInstalled, "Uppaal is not installed, please install it and add it to your PATH")
    }
    val uppaalFile = generateUppaalFile(masterModel, uppaalFileType)
    val verificationResult = VerifyTA.verify(uppaalFile, isOnlineMode)
    //FileUtils.deleteQuietly(uppaalFile)
    checkVerificationResult(verificationResult)
  }

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    val t_ms = (t1 - t0) / 1000000
    println("Elapsed time: " + t_ms + "ms")
    result
  }

  /**
   * Verifies whether the next intended action is correct with respect to the scenario model and the previous actions.
   * This method is used for dynamic verification as it return a verdict with the enabled actions.
   *
   * @param scenarioModel    the scenario
   * @param previous_actions the previous actions
   * @param next_action      the next action
   * @return true if the algorithm is correct, false otherwise and the enabled actions
   */
  def dynamicVerification(scenarioModel: ScenarioModel,
                          previous_actions: List[CosimStepInstruction],
                          next_action: CosimStepInstruction): Verdict = {
    // Does the previous actions contain repeated actions?
    val connectionsSrc = scenarioModel.connections.groupBy(_.srcPort)
    val numberOfActionsInAlgorithm = connectionsSrc.map(_._2.size).sum + connectionsSrc.size + scenarioModel.fmus.size
    val actionsToCheck = previous_actions.size % numberOfActionsInAlgorithm
    val firstAction = Math.max(previous_actions.size - actionsToCheck, 0)
    val filtered_previous_actions = previous_actions.drop(firstAction)

    /*
    val lastIndexOfFirstAction = if (previous_actions.nonEmpty)
      previous_actions.lastIndexOf(previous_actions.head)
    else 0
    val filtered_previous_actions = previous_actions.drop(lastIndexOfFirstAction)
     */

    val masterModel = MasterModel("dynamic_verification",
      scenarioModel,
      List.empty[InstantiationInstruction],
      List.empty[InitializationInstruction],
      Map("conf1" -> (filtered_previous_actions ++ List(next_action))),
      List.empty[TerminationInstruction])

    val cachedResult = lookupInCache(masterModel)
    if (cachedResult.isDefined) {
      return Verdict(cachedResult.get, Predef.Set.empty)
    }

    val verdict = if (verifyAlgorithm(masterModel, ScenarioGenerator.generateDynamicNoEnabledUppaalFile, isOnlineMode = true)) {
      // If the algorithm is correct, we don't need to generate a trace
      Verdict(correct = true, Predef.Set.empty)
    } else {
      logger.info("Trace generated")
      //TODO: Check if the trace is empty
      Verdict(correct = false, Predef.Set.empty)
    }
    cacheScenarioVerification(masterModel, verdict)
    verdict
  }

  private def lookupInCache(masterModel: MasterModel): Option[Boolean] = {
    val cacheFile = new File(s"cache/cache.csv")
    if (!cacheFile.exists())
      return None
    val cache = FileUtils.readLines(cacheFile, Charset.defaultCharset())
    val hash = masterModel.hashCode()
    val cachedVerdict = cache.find(_.startsWith(hash.toString))
    cachedVerdict.map(_.split(",")(1).toBoolean)
  }

  private def cacheScenarioVerification(masterModel: MasterModel, verdict: Verdict): Unit = {
    val cacheDirectory = new Directory(new File("cache"))
    if (!cacheDirectory.exists) {
      cacheDirectory.createDirectory()
    }
    val cacheFile = new File(s"cache/cache.csv")
    if (!cacheFile.exists())
      FileUtils.writeStringToFile(cacheFile, "scenario,verdict\n", Charset.defaultCharset())
    FileUtils.writeStringToFile(cacheFile, s"${masterModel.hashCode()},${verdict.correct}\n", Charset.defaultCharset(), true)
  }

  /**
   * Verifies whether the next intended action is correct with respect to the scenario model and the previous actions.
   * This method is used for dynamic verification as it return a verdict with the enabled actions.
   *
   * @param scenarioModel    the scenario
   * @param previous_actions the previous actions
   * @param next_action      the next action
   * @return true if the algorithm is correct, false otherwise and the enabled actions
   */
    /*
  def dynamicZ3Verification(scenarioModel: ScenarioModel,
                            previous_actions: List[CosimStepInstruction],
                            next_action: CosimStepInstruction): Verdict = {
    val masterModel = MasterModel("dynamic_verification_z3",
      scenarioModel,
      List.empty[InstantiationInstruction],
      List.empty[InitializationInstruction],
      Map("conf1" -> (previous_actions ++ List(next_action))),
      List.empty[TerminationInstruction])

    val verified = SMTEncoder.dynamicVerifyAlgorithm(masterModel)
    // Trace could not be generated because the algorithm is correct
    Verdict(correct = verified, Predef.Set.empty)
  }

     */

  /**
   * Synthesize an orchestration algorithm and verify it with respect to the scenario model.
   *
   * @param name          the name of the scenario
   * @param scenarioModel the scenario
   * @return true if the algorithm is correct, false otherwise
   */
  def synthesizeAndVerify(name: String, scenarioModel: ScenarioModel): Boolean = {
    val masterModel = GenerationAPI.synthesizeAlgorithm(name, scenarioModel)
    verifyAlgorithm(masterModel)
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
      case InitSet(port) => scenarioverifier.core.InitSet(sanitizePort(port))
      case InitGet(port) => scenarioverifier.core.InitGet(sanitizePort(port))
      case EnterInitMode(fmu) => EnterInitMode(fmu)
      case ExitInitMode(fmu) => ExitInitMode(fmu)
      case AlgebraicLoopInit(untilConverged, iterate) => scenarioverifier.core.AlgebraicLoopInit(untilConverged.map(i => sanitizePort(i)), iterate.map(i => sanitizeName(i)))
    }
  }

  private def sanitizeAction(act: CosimStepInstruction): CosimStepInstruction = {
    act match {
      case Set(port) => scenarioverifier.core.Set(sanitizePort(port))
      case Get(port) => Get(sanitizePort(port))
      case GetTentative(port) => scenarioverifier.core.GetTentative(sanitizePort(port))
      case SetTentative(port) => scenarioverifier.core.SetTentative(sanitizePort(port))
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

  private def generateTraceFromMasterModel(masterModel: MasterModel,
                                           uppaalFileType: (String, ModelEncoding, Directory) => File,
                                           isOnlineMode: Boolean = false
                                          ): Option[UppaalTrace] = {
    if (verifyAlgorithm(masterModel, uppaalFileType, isOnlineMode)) {
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
