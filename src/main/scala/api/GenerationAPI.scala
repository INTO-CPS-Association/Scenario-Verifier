package api

import java.io.{File, PrintWriter}
import java.nio.file.Files

import cli.VerifyTA
import core.ScenarioLoader.{generateEnterInitInstructions, generateExitInitInstructions, generateInstantiationInstructions, generateTerminateInstructions, parse}
import core.{MasterModel, ModelEncoding, ScenarioGenerator, ScenarioLoader, ScenarioModel}
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
      write(content); close()
    }
    file
  }

  def verifyAlgorithm(masterModel: MasterModel): Boolean = {
    val f = generateUppaalFile(masterModel)
    checkUppaalVersion()
    val verificationResult = VerifyTA.verify(f)
    FileUtils.deleteQuietly(f)

    checkVerificationResult(verificationResult)
  }

  def generateAndVerify(name: String, scenarioModel: ScenarioModel) = {
    val masterModel = GenerationAPI.generateAlgorithm(name, scenarioModel)
    verifyAlgorithm(masterModel)
  }

  def generateTrace(name: String, scenarioModel: ScenarioModel):TraceResult = {
    val masterModel = GenerationAPI.generateAlgorithm(name, scenarioModel)
    generateTraceFromMasterModel(masterModel)
  }

  private def generateUppaalFile(masterModel: MasterModel): File = {
    val encoding = new ModelEncoding(masterModel)
    val encodedUppaal = ScenarioGenerator.generate(encoding)
    writeToTempFile(encodedUppaal)
  }

  def generateTraceFromMasterModel(masterModel: MasterModel): TraceResult = {
    if(verifyAlgorithm(masterModel))
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
