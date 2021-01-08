package cli

import java.io.{BufferedWriter, File, FileWriter, IOException}

import org.apache.logging.log4j.scala.Logging

import scala.util.Properties
import sys.process._

object VerifyTA extends Logging {
  def verify(uppaalFile: File) = {
    /*
    Options:
    -t <0|1|2>
      Generate diagnostic information on stderr.
        0: Some trace
        1: Shortest trace (disables reuse)
        2: Fastest trace (disables reuse)
     -s  Do not display the progress indicator.
     -Y  Display traces symbolically (pre- and post-stable).
     */
    val fPath = uppaalFile.getAbsolutePath
    val cmd = s"""${VERIFY} -t 1 -Y -s '${fPath}'"""
    logger.info(s"Running command: ${cmd}")
    val pLog = new VerifyTaProcessLogger()
    val exitCode = Process(cmd).!(pLog)
    if (exitCode != 0) {
      logger.error(s"Command returned non zero exit code: ${exitCode}.")
      logger.error(s"This is probably a syntax error.")
      2
    } else {
      val output = pLog.output.toString
      if (output.contains("Formula is NOT satisfied.")) {
        logger.error(s"Model is not valid.")
        logger.error(s"To see the trace, run: ${cmd}")
        1
      } else {
        0
      }
    }
  }

  def saveTraceToFile(uppaalFile: File, traceFile: File) = {
    val fPath = uppaalFile.getAbsolutePath
    val cmd = s"""${VERIFY} -t 1 -Y -s '${fPath}'"""
    logger.info(s"Running command: ${cmd}")
    val pLog = new VerifyTaProcessLogger()
    val exitCode = Process(cmd).!(pLog)
    if (exitCode != 0) {
      logger.error(s"Command returned non zero exit code: ${exitCode}.")
      logger.error(s"This is probably a syntax error.")
    } else {
      val bw = new BufferedWriter(new FileWriter(traceFile))
      val trace = pLog.output.toString.replace("State", "\nState").replace("Transitions", "\nTransitions")
        .split("\n")
        .drop(2).toList

      trace.slice(0, trace.indexWhere(_.contains("MasterA.Start"), 2))
        .filterNot(_.contains("Transitions"))
        .map(_.replaceAll("\\([^()]*\\)", ""))
        .map(_.replaceAll("#depth=\\d+ *", ""))
        .foreach(s => {
          bw.write(s + "\n")
        })
      bw.close()
      logger.info(s"Trace saved toﬂ ${traceFile}")
    }
  }

  val VERIFY = "verifyta"

  def checkEnvironment(): Boolean = {
    logger.info("Checking for binary in system.")
    try {
      val pLog = new VerifyTaProcessLogger()
      val cmd = s"""${VERIFY} -v"""
      val exitCode = Process(cmd).!(pLog)
      if (exitCode != 0) {
        logger.error(s"Command failed: $cmd")
        false
      } else {
        val versionOut = pLog.output.toString
        logger.debug(s"${VERIFY} version:")
        logger.debug(versionOut)
        versionOut.contains("UPPAAL")
      }
    } catch {
      case e: IOException => {
        logger.error("Problem with verifyta binary. Make sure it is in the PATH.")
        logger.info("Current PATH:")
        logger.info(System.getenv("PATH"))
        logger.info("Underlying exception was:")
        logger.info(e)
        false
      }
    }
  }
}
