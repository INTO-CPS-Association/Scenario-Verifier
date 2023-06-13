package api.FMI3

import cli.Z3.SMTEncoder
import core.FMI3.{FMI3ScenarioModel, MasterModel3}
import org.apache.logging.log4j.scala.Logging

object Verification extends Logging {

  /**
   * Verifies whether the algorithm is correct with respect to the scenario model.
   *
   * @param masterModel the algorithm and scenario to verify
   * @return true if the algorithm is correct, false otherwise
   */
  def verifyAlgorithm(masterModel: MasterModel3): Boolean = {
    SMTEncoder.verifyAlgorithm(masterModel)
  }

  /**
   * Synthesize an orchestration algorithm and verify it with respect to the scenario model.
   *
   * @param scenarioModel the scenario
   * @return the synthesized algorithm in a MasterModel
   */
  def synthesizeAlgorithm(scenarioModel: FMI3ScenarioModel): MasterModel3 = {
    val masterModel = MasterModel3("master", scenarioModel, List.empty, List.empty, List.empty, Map.empty, List.empty)
    val modelWithAlgorithm = SMTEncoder.synthesizeAlgorithm(masterModel)
    SMTEncoder.verifyAlgorithm(modelWithAlgorithm)
    modelWithAlgorithm
  }

  def synthesizeAlgorithmWithStatistics(scenarioModel: FMI3ScenarioModel): MasterModel3 = {
    val masterModel = MasterModel3("master", scenarioModel, List.empty, List.empty, List.empty, Map.empty, List.empty)
    logger.info(
      s"""
         |Synthesizing algorithm for scenario with ${scenarioModel.fmus.size} FMUs and ${scenarioModel.connections.size} connections
         |The scenario has ${scenarioModel.eventEntrances.size} event entrances.
         |""".stripMargin)
    logger.info("Synthesizing algorithm")
    time {
      val modelWithAlgorithm = SMTEncoder.synthesizeAlgorithm(masterModel)
    }
    val modelWithAlgorithm = SMTEncoder.synthesizeAlgorithm(masterModel)
    logger.info("Verifying algorithm")
    time {
      SMTEncoder.verifyAlgorithm(modelWithAlgorithm)
    }
    modelWithAlgorithm
  }

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) / 1000000.0 + "ms")
    result
  }

}

