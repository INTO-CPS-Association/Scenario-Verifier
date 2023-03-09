package cli

import org.apache.logging.log4j.scala.Logging

object MaudeRunner extends Logging {
  val MaudeCmd = "maude"
/*
  def verify(maudeFile: File, fullMaudePath : String) = {
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
    val fPath = maudeFile.getAbsolutePath

    val pLog = new VerifyTaProcessLogger()
    val startupCMD = s"$MaudeCmd $fullMaudePath"
    logger.info(s"Starting Full Maude: $startupCMD")
    logger.info(s"Loading Scenario: $fPath")
    val loadCMD = s"load $fPath"
    val cmd = "( frew setup . )"
    val versionOut = pLog.output.toString
    logger.info(versionOut)
  }


  def checkEnvironment(): Boolean = {
    logger.info("Checking if Maude is installed in the system.")
    try {
      val pLog = new VerifyTaProcessLogger()
      val cmd = s"""${MaudeCmd} --version"""
      val exitCode = Process(cmd).!(pLog)
      if (exitCode != 0) {
        logger.error(s"Command failed: $cmd")
        false
      } else {
        val versionOut = pLog.output.toString
        logger.info("Maude is installed in the system!")

        logger.debug(s"${MaudeCmd} version:")
        logger.debug(versionOut)
        versionOut.contains("3.1")
      }
    } catch {
      case e: IOException =>
        logger.error("Problem with Maude binary. Make sure it is in the PATH.")
        logger.info("Current PATH:")
        logger.info(System.getenv("PATH"))
        logger.info("Underlying exception was:")
        logger.info(e)
        false
    }
  }
 */
}
