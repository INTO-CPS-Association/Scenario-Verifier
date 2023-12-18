package org.intocps.verification.scenarioverifier.cli

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

import scala.reflect.io.Directory

import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier.api.GenerationAPI
import org.intocps.verification.scenarioverifier.core.masterModel.MasterModel
import org.intocps.verification.scenarioverifier.core.masterModel.MasterModelFMI2
import org.intocps.verification.scenarioverifier.core.ModelEncoding
import org.intocps.verification.scenarioverifier.core.ScenarioGenerator
import org.intocps.verification.scenarioverifier.core.ScenarioLoader
import org.intocps.verification.scenarioverifier.traceanalyzer.TraceAnalyzer
import scopt.OParser

object ScenarioVerifierApp extends App with Logging {
  logger.info("Logger started.")
  logger.debug("Debug logging enabled.")

  private def writeFile(filename: String, lines: Seq[String]): Unit = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))
    for (line <- lines) {
      bw.write(line)
    }
    bw.close()
  }

  private val builder = OParser.builder[CLIConfig]
  val parser: _root_.scopt.OParser[Unit, CLIConfig] = CLIParer

  private def CLIParer: OParser[Unit, CLIConfig] = {
    val parser = {
      import builder._
      OParser.sequence(
        programName("scenario_verifier"),
        head("Scenario Verifier", "0.0.2"),
        help("help").text("prints this usage text"),
        opt[Unit]("verify")
          .action((_, c) => c.copy(verify = true))
          .text("Use verifyta to check the resulting UPPAAL model")
          .children(
            opt[String]('m', "modelEncoding")
              .validate(x =>
                if (Files.exists(Paths.get(x)) && x.endsWith(".conf")) success
                else failure("File " + x + " does not exist."))
              .required()
              .action((x, c) => c.copy(master = x))
              .text("modelEncoding is a file containing the master configuration - a scenario model and an algorithm.")),
        opt[Unit]("trace")
          .action((_, c) => c.copy(verify = true))
          .text("Visualize the trace from the UPPAAL model")
          .children(
            opt[String]('m', "modelEncoding")
              .validate(x =>
                if (Files.exists(Paths.get(x)) && x.endsWith(".conf")) success
                else failure("File " + x + " does not exist."))
              .required()
              .action((x, c) => c.copy(master = x))
              .text("modelEncoding is a file containing the master configuration - a scenario model and an algorithm.")),
        opt[Unit]("generate")
          .action((_, c) => c.copy(generateAlgorithm = true))
          .text("Generate the master algorithm for the scenario")
          .children(
            opt[String]('m', "modelEncoding")
              .validate(x =>
                if (Files.exists(Paths.get(x)) && x.endsWith(".conf")) success
                else failure("File " + x + " does not exist."))
              .required()
              .action((x, c) => c.copy(master = x))
              .text("modelEncoding is a file containing the master configuration - a scenario model and an algorithm.")))
    }
    parser
  }

  OParser.parse(parser, args, CLIConfig(), OParserSetup()) match {
    case Some(config) =>
      logger.info("Starting scenario verifier.")
      require(VerifyTA.isInstalled, "VerifyTA/UPPAAL is not installed - please install it.")
      logger.info(f"Master description: ${config.master}")
      var masterModel: MasterModel = ScenarioLoader.load(config.master)
      if (config.generateAlgorithm) {
        logger.info(f"Generating algorithm for scenario: ${config.master}")
        masterModel = GenerationAPI.synthesizeAlgorithm(masterModel.name, masterModel.scenario)
        FileUtils.deleteQuietly(new File(config.master))
        writeFile(config.master, masterModel.toConf(0).split("\n").toIndexedSeq)
      }
      logger.debug(s"Loaded model: $masterModel")
      val queryModel = new ModelEncoding(masterModel.asInstanceOf[MasterModelFMI2])
      logger.debug(s"Generate UPPAAL model.")
      val folder = new File("uppaal")
      println("folder: " + folder.getAbsolutePath)
      val uppaalFile = ScenarioGenerator.generateUppaalFile(scenarioName = masterModel.name, queryModel, new Directory(folder))
      logger.info(f"Generated uppaal file: ${uppaalFile.getName}")

      if (config.verify) {
        logger.info(f"Verifying generated file.")
        if (config.trace) {
          val outputFolder = Paths.get(uppaalFile.getParentFile.getName, "/video_trace")
          if (!Files.exists(outputFolder))
            Files.createDirectory(outputFolder)
          val traceFile = Files.createTempFile("trace_", ".log").toFile
          val result = VerifyTA.saveTraceToFile(uppaalFile, traceFile)
          if (result == 1) {
            logger.info(s"Started generating the animation of trace ${masterModel.name} in folder: $outputFolder.")
            val source = scala.io.Source.fromFile(traceFile)
            try {
              val lines = source.getLines()
              TraceAnalyzer.AnalyseScenario(masterModel.name, lines, queryModel, outputFolder.toString)
            } finally source.close()
            FileUtils.deleteQuietly(traceFile)
          }
        } else {
          logger.info(s"Started verifying ${uppaalFile.getAbsolutePath} in Uppaal.")
          println(s"Started verifying ${uppaalFile.getAbsolutePath} in Uppaal.")
          VerifyTA.verify(uppaalFile)
        }
      }
    case _ =>
      // arguments are bad, error message will have been displayed
      println("Error parsing arguments.")
      System.exit(1)
  }
  logger.info("Finished scenario verifier.")
  println("Finished scenario verifier - exiting.")
  System.exit(0)
}
