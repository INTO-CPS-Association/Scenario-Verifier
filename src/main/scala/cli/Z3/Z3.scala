package cli.Z3

import cli.{CLITool, VerifyTaProcessLogger}
import core.FMI3.MasterModel3
import core._
import org.apache.logging.log4j.scala.Logging

import java.io.File

object Z3 extends CLITool {
  def name: String = "Z3"

  def command: String = "Z3"

  def runZ3(file: File): String = {
    require(file.exists(), "File does not exist")
    require(file.isFile, "File is not a file")
    require(file.canRead, "File is not readable")
    require(file.getName.endsWith(".smt2"), "File is not a .smt2 file")
    // Z3 must be installed and on the path
    val commandOptions = List("z3", "--smt2", "-file", file.getAbsolutePath)
    val processLogger = new VerifyTaProcessLogger()
    runCommand(commandOptions, processLogger)
    processLogger.output.toString
  }
}

object SMTEncoder extends Logging {
  private def encodeFile(masterModel: MasterModel3, algorithmTypes: List[AlgorithmType.Value], synthesize: Boolean): File = {
    val smtLib = masterModel.toSMTLib(algorithmTypes, synthesize, isParallel = false)
    // Write to file
    val smtFile = File.createTempFile("cosim", ".smt2")
    //smtFile.deleteOnExit()
    val bw = new java.io.BufferedWriter(new java.io.FileWriter(smtFile))
    bw.write(smtLib)
    bw.close()
    smtFile
  }

  def verifyAlgorithm(masterModel: MasterModel3): Boolean = {
    val smtFile = encodeFile(masterModel, List(AlgorithmType.init, AlgorithmType.step, AlgorithmType.event), synthesize = false)
    val result = Z3.runZ3(smtFile)
    if (result.contains("sat")) {
      logger.info("Algorithm is correct")
      true
    } else {
      logger.info("Algorithm is incorrect")
      false
    }
  }


  def synthesizeAlgorithm(masterModel: MasterModel3): MasterModel3 = {
    val smtFile = encodeFile(masterModel, List(AlgorithmType.init, AlgorithmType.step, AlgorithmType.event), synthesize = true)
    val model = Z3.runZ3(smtFile)
    if (model.contains("sat")) {
      logger.info("The scenario is realizable and the algorithm is synthesized")
      val updatedModel = Z3ModelParser.parseZ3Model(model, masterModel)
      updatedModel
    } else {
      throw new Exception("Z3 failed to synthesize algorithm - the scenario is not realizable")
    }
  }

  /*
  def dynamicVerifyAlgorithm(masterModel: MasterModel): Boolean = {
    val smtFile = encodeFile(masterModel, List(AlgorithmType.step))
    val result = Z3.runZ3(smtFile)
    result == 0
  }
   */
}
