package api.FMI3

import cli.Z3.SMTEncoder
import core.FMI3.{FMI3ScenarioModel, MasterModel3}
import org.apache.logging.log4j.scala.Logging

object VerificationAPI extends Logging {

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
}

