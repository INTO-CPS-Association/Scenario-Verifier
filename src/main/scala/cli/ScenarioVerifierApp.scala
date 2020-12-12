package cli

import java.io.PrintWriter
import java.nio.file.{Files, Paths}

import core.{ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.logging.log4j.scala.Logging
import scopt.OParser

object ScenarioVerifierApp extends App with Logging {

  logger.info("Logger started.")
  logger.debug("Debug logging enabled.")

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
      opt[Unit]("verify")
        .action((_, c) => c.copy(verify = true))
        .text("Uses verifyta to check the resulting UPPAAL model"),
    )
  }

  // OParser.parse returns Option[Config]
  OParser.parse(parser, args, CLIConfig(), OParserSetup()) match {
    case Some(config) =>

      logger.info(f"Output file: ${config.output}.")
      if (Files.exists(Paths.get(config.output))){
        logger.error(s"Output file already exists. Will not be overwritten. Delete it before running this app: ${config.output}")
        System.exit(1)
      }

      logger.info(f"Master description: ${config.master}")
      val masterModel = ScenarioLoader.load(config.master)
      logger.debug("Loaded model: ")
      logger.debug(masterModel)

      val queryModel = new ModelEncoding(masterModel)
      val result = ScenarioGenerator.generate(queryModel)
      new PrintWriter(config.output) { write(result); close() }

      if (config.verify) {
        logger.info(f"Verifying generated file.")
        val checkExitCode = VerifyTA.checkEnvironment()
        if (! checkExitCode){
          System.exit(1)
        }
      }

    case _ =>
      // arguments are bad, error message will have been displayed
      System.exit(1)
  }
  System.exit(0)
}
