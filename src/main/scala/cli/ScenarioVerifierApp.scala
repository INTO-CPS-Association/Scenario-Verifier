package cli

import api.GenerationAPI
import core.{ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.scala.Logging
import scopt.OParser
import trace_analyzer.TraceAnalyzer

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}
import java.nio.file.{Files, Paths}

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

  val builder = OParser.builder[CLIConfig]
  val parser = {
    import builder._
    OParser.sequence(
      programName("scenario_verifier"),
      head("Scenario Verifier", "0.0.1"),
      help("help").text("prints this usage text"),
      opt[String]('m', "master")
        .required()
        .action((x, c) => c.copy(master = x))
        .text("File containing the master configuration"),
      opt[String]('o', "output")
        .action((x, c) => c.copy(output = x))
        .text("Output file containing master algorithm in UPPAAL."),
      opt[Unit]("trace")
        .action((_, c) => c.copy(trace = true))
        .text("Create animation of the trace from the UPPAAL model"),
      opt[Unit]("verify")
        .action((_, c) => c.copy(verify = true))
        .text("Uses verifyta to check the resulting UPPAAL model"),
      opt[Unit]("generate")
        .action((_, c) => c.copy(generateAlgorithm = true))
        .text("Generate the master algorithm for the scenario"),
    )
  }

  // OParser.parse returns Option[Config]
  OParser.parse(parser, args, CLIConfig(), OParserSetup()) match {
    case Some(config) =>
      logger.info(f"Output file: ${config.output}.")
      if (Files.exists(Paths.get(config.output))) {
        logger.error(s"Output file already exists. Will not be overwritten. Delete it before running this app: ${config.output}")
        System.exit(1)
      }

      logger.info(f"Master description: ${config.master}")
      var masterModel = ScenarioLoader.load(config.master)

      if(config.generateAlgorithm){
        masterModel = GenerationAPI.synthesizeAlgorithm(masterModel.name, masterModel.scenario)
        FileUtils.deleteQuietly(new File(config.master))
        writeFile(config.master, masterModel.toConf().split("\n"))
      }

      logger.debug("Loaded model: ")
      logger.debug(masterModel)

      val queryModel = new ModelEncoding(masterModel)
      val result = ScenarioGenerator.generate(queryModel)
      new PrintWriter(config.output) {
        write(result)
        close()
      }

      if (config.verify) {
        logger.info(f"Verifying generated file.")
        val checkExitCode = VerifyTA.checkEnvironment()
        if (!checkExitCode) {
          System.exit(1)
        }
        val file = new File(config.output)
        if (config.trace) {
          val outputFolder = Paths.get(file.getParentFile.getName,"/video_trace")
          if(!Files.exists(outputFolder))
            Files.createDirectory(outputFolder)
          val traceFile = Files.createTempFile("trace_", ".log").toFile
          val result = VerifyTA.saveTraceToFile(file, traceFile)
          if (result == 1) {
            logger.info(s"Started generating the animation of trace ${masterModel.name} in folder: $outputFolder.")
            val source = scala.io.Source.fromFile(traceFile)
            try {
              val lines = source.getLines()
              TraceAnalyzer.AnalyseScenario(masterModel.name, lines, queryModel, outputFolder.toString)
            }
            finally source.close()
            FileUtils.deleteQuietly(traceFile)
          }
        } else {
          logger.info(s"Started verifying ${file.getName} in Uppaal.")
          VerifyTA.verify(file)
        }
      }

    case _ =>
      // arguments are bad, error message will have been displayed
      System.exit(1)
  }
  System.exit(0)
}
