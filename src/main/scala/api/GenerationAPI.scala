package api

import java.io.PrintWriter
import java.nio.file.Files

import cli.VerifyTA
import core.ScenarioLoader.{generateEnterInitInstructions, generateExitInitInstructions, generateInstantiationInstructions, generateTerminateInstructions, parse}
import core.{MasterModel, ModelEncoding, ScenarioGenerator, ScenarioLoader, ScenarioModel}
import org.apache.commons.io.FileUtils
import synthesizer.SynthesizerSimple
import trace_analyzer.TraceAnalyzer

object GenerationAPI {

  def generateInitialization(scenarioModel: ScenarioModel)= {
    val synthesizer = new SynthesizerSimple(scenarioModel)
    synthesizer.synthesizeInitialization()
  }

  def generatecoSimStep(scenarioModel: ScenarioModel)= {
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


object VerificationAPI {

  def writeToTempFile(content: String) = {
    val file = Files.createTempFile("uppaal_", ".xml").toFile
    new PrintWriter(file) { write(content); close() }
    file
  }

  def verifyAlgorithm(masterModel: MasterModel): Boolean = {
    val encoding = new ModelEncoding(masterModel)
    val result = ScenarioGenerator.generate(encoding)
    val f = writeToTempFile(result)
    assert(VerifyTA.checkEnvironment(), "UPPAAL v.4.1 is not in PATH - please install it and try again!")
    val verificationResult = VerifyTA.verify(f)
    FileUtils.deleteQuietly(f)

    if(verificationResult == 0) true
    else {
      if(verificationResult == 2)
        throw SyntaxException("The verification in Uppaal failed mostly likely due to a syntax error")
      else false
    }
  }

  def generateAndVerify(name: String, scenarioModel: ScenarioModel)= {
    val masterModel = GenerationAPI.generateAlgorithm(name, scenarioModel)
    verifyAlgorithm(masterModel)
  }

  def generateTrace(name: String, scenarioModel: ScenarioModel) = {
    val masterModel = GenerationAPI.generateAlgorithm(name, scenarioModel)
    generateTraceFromMasterModel(masterModel)
  }

  def generateTraceFromMasterModel(masterModel: MasterModel) = {
    val encoding = new ModelEncoding(masterModel)
    val result = ScenarioGenerator.generate(encoding)
    val f = writeToTempFile(result)
    val traceFile = Files.createTempFile("trace_", ".log").toFile
    val videoFile = Files.createTempFile("trace_", ".mp4").toFile

    assert(VerifyTA.checkEnvironment(), "UPPAAL v.4.1 is not in PATH - please install it and try again!")
    assert(VerifyTA.verify(f) == 1, "No trace can be generated.")
    VerifyTA.saveTraceToFile(f, traceFile)
    FileUtils.deleteQuietly(f)
    val source = scala.io.Source.fromFile(traceFile)
    try {
      val lines = source.getLines()
      TraceAnalyzer.AnalyseScenario(masterModel.name, lines, encoding, videoFile)
    }
    finally source.close()
    FileUtils.deleteQuietly(traceFile)
    videoFile
  }
}

final case class SyntaxException(private val message: String = "",
                                 private val cause: Throwable = None.orNull)
  extends Exception(message, cause)
