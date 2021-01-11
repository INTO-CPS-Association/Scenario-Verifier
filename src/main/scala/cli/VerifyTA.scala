package cli

import java.io.{BufferedWriter, File, FileWriter, IOException}

import org.apache.logging.log4j.scala.Logging

import scala.sys.process._

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
        logger.error(s"Generate the trace and use to correct the algorithm.")
        1
      } else {
        0
      }
    }
  }

  def saveTraceToFile(uppaalFile: File, traceFile: File): Int = {
    val fPath = uppaalFile.getAbsolutePath
    val cmd = s"""${VERIFY} -t 1 -Y -s '${fPath}'"""
    val pLog = new VerifyTaProcessLogger()
    val exitCode = Process(cmd).!(pLog)
    if (exitCode != 0) {
      logger.error(s"Command returned non zero exit code: ${exitCode}.")
      logger.error(s"This is probably a syntax error - no trace generated.")
      2
    } else {
      val output = pLog.output.toString
      if (output.contains("Formula is NOT satisfied.")) {
        val bw = new BufferedWriter(new FileWriter(traceFile))
        val trace = pLog.output.toString.replace("State", "\n").replace("Transitions", "\nTransitions")
          .split("\n")
          .drop(2).toList

        trace.drop(trace.indexWhere(_.contains("MasterA.Start"), 2))
          .filterNot(s => !(s.contains("isInit=1") || s.contains("isSimulation=1")))
          .map(_.replaceAll("\\([^()]*\\)", ""))
          .map(_.replaceAll("#depth=\\d+ *", ""))
          .foreach(s => {
            bw.write(s + "\n")
          })
        bw.close()
        1
      } else {
        logger.info("All Formulas are satisfied - no counter-example can be found.")
        0
      }
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
