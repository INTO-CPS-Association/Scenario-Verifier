package org.intocps.verification.scenarioverifier.cli

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

import scala.sys.process._

import org.apache.logging.log4j.scala.Logging

trait CLITool extends Logging {
  def name: String

  def command: String

  def runCommand(options: List[String], processLogger: VerifyTaProcessLogger = new VerifyTaProcessLogger()): Int = {
    val cmd = s"""$command ${options.mkString(" ")}"""
    logger.debug(s"Running command: $cmd")
    logger.info(s"Running $name with options: ${options.mkString(" ")}")
    val exitCode = Process(cmd).!(processLogger)
    if (exitCode != 0) {
      logger.error(s"Command returned non zero exit code: $exitCode.")
      logger.error(s"This is probably a syntax error.")
    }
    exitCode
  }

  def isInstalled: Boolean = {
    logger.info(s"Checking if $name is installed in the system.")
    try {
      val exitCode = runCommand(List("-v"))
      exitCode == 0
    } catch {
      case _: IOException =>
        logger.error(s"Problem with $name binary. Make sure it is in the PATH.")
        logger.info("Current PATH:")
        logger.info(System.getenv("PATH"))
        false
    }
  }
}

object VerifyTA extends CLITool {
  def name: String = "UPPAAL"

  def command: String = "verifyta"

  def runUppaal(uppaalFile: File, notSatisfiedHandler: (File, VerifyTaProcessLogger) => Unit, onlineMode: Boolean = false): Int = {
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
    require(uppaalFile.exists(), s"File ${uppaalFile.getAbsolutePath} does not exist.")
    if (!onlineMode) {
      require(isInstalled, s"UPPAAL is not installed in the system. Make sure it is in the PATH.")
    }
    val fPath = uppaalFile.getAbsolutePath
    val options =
      if (onlineMode)
        List(s"'$fPath'")
      else
        List("-t", "1", "-Y", "-s", s"'$fPath'")
    val pLog = new VerifyTaProcessLogger()
    val exitCode = runCommand(options, pLog)
    if (exitCode != 0) {
      logger.error(s"Command returned non zero exit code: $exitCode.")
      logger.error(s"This is probably a syntax error.")
      2
    } else {
      val output = pLog.output.toString
      if (output.contains("Formula is NOT satisfied.")) {
        logger.error(s"Model is not valid.")
        logger.error(s"Generate the trace and use to correct the algorithm.")
        notSatisfiedHandler(uppaalFile, pLog)
        1
      } else {
        logger.info("All Formulas are satisfied")
        0
      }
    }
  }

  def verify(uppaalFile: File, isOnlineMode: Boolean = false): Int = {
    runUppaal(uppaalFile, (_, _) => (), isOnlineMode)
  }

  def saveTraceToFile(uppaalFile: File, traceFile: File): Int = {
    val fPath = uppaalFile.getAbsolutePath
    val options = List("-t", "0", "-C", "-A", "-Y", "-s", s"'${fPath}'")
    val pLog = new VerifyTaProcessLogger()
    val exitCode = runCommand(options, pLog)
    if (exitCode != 0) {
      logger.error(s"Command returned non zero exit code: $exitCode.")
      logger.error(s"This is probably a syntax error - no trace generated.")
      2
    } else {
      val output = pLog.output.toString
      if (output.contains("Formula is NOT satisfied.")) {
        saveTrace(traceFile, pLog)
        1
      } else {
        logger.info("All Formulas are satisfied - no counter-example can be found.")
        0
      }
    }
  }

  private def saveTrace(traceFile: File, pLog: VerifyTaProcessLogger): Unit = {
    val bw = new BufferedWriter(new FileWriter(traceFile))
    val trace = pLog.output.toString
      .replace("State", "\n")
      .replace("Transitions", "\nTransitions")
      .split("\n")
      .drop(2)
      .toList

    trace
      .drop(trace.indexWhere(_.contains("MasterA.Start"), 2))
      .filterNot(s => !(s.contains("isInit=1") || s.contains("isSimulation=1")))
      .map(_.replaceAll("\\([^()]*\\)", ""))
      .map(_.replaceAll("#depth=\\d+ *", ""))
      .foreach(s => {
        bw.write(s + "\n")
      })
    bw.close()
  }
}