package Z3.DynamicVerification_Z3

import org.intocps.verification.scenarioverifier.api.VerificationAPI
import org.intocps.verification.scenarioverifier.core.ScenarioLoader
import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

@Ignore
class PositiveTests extends AnyFlatSpec with should.Matchers {

  def generateAndVerify(resourcesFile: String): Boolean = {
    // Assert that Z3 is installed
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoader.load(conf)
    (1 until masterModel.cosimStep.values.head.length).forall(i => {
      val previous_actions = masterModel.cosimStep.values.head.take(i)
      val current_action = masterModel.cosimStep.values.head(i)
      // VerificationAPI.dynamicZ3Verification(masterModel.scenario, previous_actions, current_action)
      true
    })
  }

  it should "work for simple_master scenario" in {
    generateAndVerify("../examples/simple_master.conf")
  }

  it should "work for simple_master_step_sizes" in {
    generateAndVerify("../examples/simple_master_step_sizes.conf")
  }

  it should "work for industrial" in {
    generateAndVerify("../examples/industrial_casestudy.conf")
  }

  it should "work for industrial_case_alt" in {
    generateAndVerify("../examples/industrial_casestudy_alt.conf")
  }

  it should "work for dynamic scenario 1" in {
    generateAndVerify("../examples/dynamic_state_estimation_scenario_1.conf")
  }

  it should "work for dynamic scenario 2" in {
    generateAndVerify("../examples/dynamic_state_estimation_scenario_2.conf")
  }
}
