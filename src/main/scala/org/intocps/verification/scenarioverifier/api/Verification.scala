package org.intocps.verification.scenarioverifier.api

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files

import scala.reflect.io.Directory

import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier
import org.intocps.verification.scenarioverifier.cli.VerifyTA
import org.intocps.verification.scenarioverifier.cli.Z3.SMTEncoder
import org.intocps.verification.scenarioverifier.core._
import org.intocps.verification.scenarioverifier.core.masterModel._
import org.intocps.verification.scenarioverifier.core.FMI3.AdaptiveModel
import org.intocps.verification.scenarioverifier.core.FMI3.ConfigurationModel
import org.intocps.verification.scenarioverifier.traceanalyzer._
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala

object VerificationAPI extends Logging {

  /**
   * Verifies whether the algorithm is correct with respect to the scenario model.
   *
   * @param masterModel
   *   the algorithm and scenario to verify
   * @return
   *   true if the algorithm is correct, false otherwise
   */
  def verifyAlgorithm(masterModel: MasterModel): Boolean = {
    masterModel match {
      case masterModel: MasterModelFMI2 => verifyFMI2Algorithm(masterModel, ScenarioGenerator.generateUppaalFile)
      case masterModel: MasterModelFMI3 => verifyFMI3Algorithm(masterModel)
      case _ => throw new Exception("Unsupported master model")
    }
  }

  /**
   * Verifies whether the algorithm is correct with respect to the scenario model.
   *
   * @param masterModel
   *   the algorithm and scenario to verify
   * @return
   *   true if the algorithm is correct, false otherwise
   */
  def verifyFMI3Algorithm(masterModel: MasterModelFMI3): Boolean = {
    SMTEncoder.verifyAlgorithm(masterModel)
  }

  /**
   * Verifies whether the algorithm is correct with respect to the scenario model.
   *
   * @param masterModel
   *   the algorithm and scenario to verify
   * @param uppaalFileType
   *   the type of Uppaal file to generate - either a normal Uppaal file or a dynamic Uppaal file
   * @return
   *   true if the algorithm is correct, false otherwise
   */
  private def verifyFMI2Algorithm(
      masterModel: MasterModelFMI2,
      uppaalFileType: (String, ModelEncoding, Directory) => File,
      isOnlineMode: Boolean = false): Boolean = {
    if (!isOnlineMode) {
      require(VerifyTA.isInstalled, "Uppaal is not installed, please install it and add it to your PATH")
    }
    val uppaalFile = generateUppaalFile(masterModel, uppaalFileType)
    val verificationResult = VerifyTA.verify(uppaalFile, isOnlineMode)
    FileUtils.deleteQuietly(uppaalFile)
    checkVerificationResult(verificationResult)
  }

  /**
   * Verifies whether the next intended action is correct with respect to the scenario model and the previous actions. This method is used
   * for dynamic verification as it return a verdict with the enabled actions.
   *
   * @param scenarioModel
   *   the scenario
   * @param previous_actions
   *   the previous actions
   * @param next_action
   *   the next action
   * @return
   *   true if the algorithm is correct, false otherwise and the enabled actions
   */
  def dynamicVerification(
      scenarioModel: FMI2ScenarioModel,
      previous_actions: List[CosimStepInstruction],
      next_action: CosimStepInstruction): Verdict = {
    // Does the previous actions contain repeated actions?
    val connectionsSrc = scenarioModel.connections.groupBy(_.srcPort)
    val numberOfActionsInAlgorithm = connectionsSrc
      .map(_._2.size)
      .sum + connectionsSrc.size + scenarioModel.fmus.size
    val actionsToCheck = previous_actions.size % numberOfActionsInAlgorithm
    val firstAction = Math.max(previous_actions.size - actionsToCheck, 0)
    val filtered_previous_actions = previous_actions.drop(firstAction)

    /*
    val lastIndexOfFirstAction = if (previous_actions.nonEmpty)
      previous_actions.lastIndexOf(previous_actions.head)
    else 0
    val filtered_previous_actions = previous_actions.drop(lastIndexOfFirstAction)
     */

    val masterModel = MasterModelFMI2(
      "dynamic_verification",
      scenarioModel,
      List.empty[InstantiationInstruction],
      List.empty[InitializationInstruction],
      Map("conf1" -> (filtered_previous_actions ++ List(next_action))),
      List.empty[TerminationInstruction])

    val cachedResult = lookupInCache(masterModel)
    if (cachedResult.isDefined) {
      return Verdict(cachedResult.get, Predef.Set.empty)
    }

    val verdict =
      if (verifyFMI2Algorithm(masterModel, ScenarioGenerator.generateDynamicNoEnabledUppaalFile, isOnlineMode = true)) {
        // If the algorithm is correct, we don't need to generate a trace
        Verdict(correct = true, Predef.Set.empty)
      } else {
        logger.info("Trace generated")
        // TODO: Check if the trace is empty
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
   * Verifies whether the next intended action is correct with respect to the scenario model and the previous actions. This method is used
   * for dynamic verification as it return a verdict with the enabled actions.
   *
   * @param scenarioModel
   *   the scenario
   * @param previous_actions
   *   the previous actions
   * @param next_action
   *   the next action
   * @return
   *   true if the algorithm is correct, false otherwise and the enabled actions
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
   * @param name
   *   the name of the scenario
   * @param scenarioModel
   *   the scenario
   * @return
   *   true if the algorithm is correct, false otherwise
   */
  def synthesizeAndVerify(name: String, scenarioModel: ScenarioModel): Boolean = {
    val masterModel = GenerationAPI.synthesizeAlgorithm(name, scenarioModel)
    masterModel match {
      case masterModel: MasterModelFMI2 => verifyAlgorithm(masterModel)
      case masterModel: MasterModelFMI3 => verifyAlgorithm(masterModel)
      case _ => throw new Exception("Unsupported master model")
    }
  }

  def generateTrace(name: String, scenarioModel: ScenarioModel): TraceResult = {
    val masterModel = GenerationAPI.synthesizeAlgorithm(name, scenarioModel)
    masterModel match {
      case masterModel: MasterModelFMI2 => generateTraceVideo(masterModel)
      case _ => throw new Exception("Unsupported master model")
    }
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
      case AlgebraicLoopInit(untilConverged, iterate) =>
        scenarioverifier.core.AlgebraicLoopInit(untilConverged.map(i => sanitizePort(i)), iterate.map(i => sanitizeName(i)))
      case _ => act
    }
  }

  private def sanitizeAction(act: CosimStepInstruction): CosimStepInstruction = {
    act match {
      case Set(port) => scenarioverifier.core.Set(sanitizePort(port))
      case Get(port) => Get(sanitizePort(port))
      case GetTentative(port) =>
        scenarioverifier.core.GetTentative(sanitizePort(port))
      case SetTentative(port) =>
        scenarioverifier.core.SetTentative(sanitizePort(port))
      case AlgebraicLoop(untilConverged, iterate, ifRetryNeeded) =>
        AlgebraicLoop(untilConverged.map(sanitizePort), iterate.map(sanitizeAction), ifRetryNeeded)
      case StepLoop(untilStepAccept, iterate, ifRetryNeeded) =>
        StepLoop(untilStepAccept, iterate.map(sanitizeAction), ifRetryNeeded)
      case Step(fmu, by) => Step(fmu, by)
      case SaveState(fmu) => SaveState(fmu)
      case RestoreState(fmu) => RestoreState(fmu)
      case NoOP => NoOP
    }
  }

  private def generateUppaalFile(masterModel: MasterModelFMI2, uppaalFileType: (String, ModelEncoding, Directory) => File): File = {
    val sanitizedModel: MasterModelFMI2 = sanitizeMasterModel(masterModel)
    val encoding = new ModelEncoding(sanitizedModel)
    val currentFolder = new File(System.getProperty("user.dir"))
    uppaalFileType(sanitizedModel.name, encoding, Directory(currentFolder))
  }

  // Todo - check if this is needed
  private def sanitizeMasterModel(masterModel: MasterModelFMI2) = {
    val config =
      AdaptiveModel(
        masterModel.scenario.config.configurableInputs.map(sanitizePort),
        masterModel.scenario.config.configurations.map(i =>
          (i._1, ConfigurationModel(i._2.inputs.map(input => (sanitizePort(input._1), input._2)), i._2.cosimStep, i._2.connections))))

    val connectedPorts = masterModel.scenario.connections.flatMap(c => List(c.srcPort, c.trgPort))
    val fmus = masterModel.scenario.fmus.map(fmu => {
      // Remove all ports that are not connected to any other port
      val connectedOutputsPorts = fmu._2.outputs.filter(p => connectedPorts.contains(PortRef(fmu._1, p._1)))
      val connectedInputsPorts = fmu._2.inputs.filter(p => connectedPorts.contains(PortRef(fmu._1, p._1)))
      val connectedOutputsPortsWithConnectedFeedthroguh =
        connectedOutputsPorts.map(p =>
          p._1.replaceAll("\\W", "") -> FMI2OutputPortModel(
            p._2.dependenciesInit.filter(d => connectedInputsPorts.contains(d)).map(_.replaceAll("\\W", "")),
            p._2.dependencies.filter(d => connectedInputsPorts.contains(d)).map(_.replaceAll("\\W", ""))))
      fmu.copy(_2 = fmu._2.copy(
        outputs = connectedOutputsPortsWithConnectedFeedthroguh,
        inputs = connectedInputsPorts.map(p => p._1.replaceAll("\\W", "") -> p._2)))
    })

    val initActions = masterModel.initialization
      .filter(op => op.portName == "noPort" || connectedPorts.contains(PortRef(op.fmu, op.portName)))
      .map(sanitizeName)

    val actions = masterModel.cosimStep.map(act =>
      (
        act._1,
        act._2
          .filter(op => op.portName == "noPort" || connectedPorts.contains(PortRef(op.fmu, op.portName)))
          .map(sanitizeAction)))

    val scenario =
      masterModel.scenario.copy(fmus = fmus, config = config)

    masterModel.copy(initialization = initActions, cosimStep = actions, scenario = scenario)
  }

  private def generateTraceFromMasterModel(
      masterModel: MasterModelFMI2,
      uppaalFileType: (String, ModelEncoding, Directory) => File,
      isOnlineMode: Boolean = false): Option[UppaalTrace] = {
    if (verifyFMI2Algorithm(masterModel, uppaalFileType, isOnlineMode)) {
      // If the algorithm is correct, we don't need to generate a trace
      return None
    }

    val f = generateUppaalFile(masterModel, uppaalFileType)
    val encoding = new ModelEncoding(masterModel)
    val traceFile = Files.createTempFile("trace_", ".log").toFile
    VerifyTA.saveTraceToFile(f, traceFile)
    FileUtils.deleteQuietly(f)

    val source = scala.io.Source.fromFile(traceFile)
    val trace =
      try {
        val lines = source.getLines()
        TraceAnalyzer.createUppaalTrace(masterModel.name, lines, encoding)
      } finally source.close()

    FileUtils.deleteQuietly(traceFile)
    Some(trace)
  }

  def generateTraceVideo(masterModel: MasterModel): TraceResult = {
    masterModel match {
      case masterModel: MasterModelFMI2 => {
        val uppaalTrace = generateTraceFromMasterModel(masterModel, ScenarioGenerator.generateUppaalFile)
        if (uppaalTrace.isEmpty)
          return TraceResult(null, isGenerated = false)

        val traceResult =
          try {
            val outputDirectory = new File(System.getProperty("user.dir"))
            val videoFilePath =
              ScenarioPlotter.plot(uppaalTrace.get, outputDirectory.getPath)
            TraceResult(new File(videoFilePath), isGenerated = true)
          } catch {
            case _: Exception =>
              println("Failed to generate video trace")
              TraceResult(null, isGenerated = false)
          }
        traceResult
      }
      case _ => throw new Exception("Unsupported master model")
    }
  }

  private def checkVerificationResult(verificationResult: Int): Boolean = {
    verificationResult match {
      case 0 => true
      case 2 =>
        throw SyntaxException("The verification in Uppaal failed most likely due to a syntax error in the UPPAAL model.")
      case _ => false
    }
  }
}

final case class TraceResult(file: File, isGenerated: Boolean)

final case class SyntaxException(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
