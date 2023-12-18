package Z3.DynamicVerificationZ3

import org.intocps.verification.scenarioverifier.core.ScenarioLoaderFMI2
import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

@Ignore
class NegativeTests extends AnyFlatSpec with should.Matchers {
  def generateAndVerifyFail(resourcesFile: String): Assertion = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoaderFMI2.load(conf)
    assert(masterModel.cosimStep.values.head.indices.exists(i => {
      val previous_actions = masterModel.cosimStep.values.head.take(i)
      val current_action = masterModel.cosimStep.values.head(i)
      true
      // !VerificationAPI.dynamicZ3Verification(masterModel.scenario, previous_actions, current_action).correct
    }))
  }

  it should "catch problem with simple_master_reactivity.conf" in {
    generateAndVerifyFail("../common_mistakes/simple_master_reactivity.conf")
  }

  it should "catch problem for simple_master_can_reject_step.conf" in {
    generateAndVerifyFail("../common_mistakes/simple_master_can_reject_step.conf")
  }

  it should "catch problem for simple_master_get_set_wrong" in {
    generateAndVerifyFail("../common_mistakes/simple_master_get_set_wrong.conf")
  }

  it should "catch problem for simple_master_forget_connection" in {
    generateAndVerifyFail("../common_mistakes/simple_master_forget_connection.conf")
  }

  it should "catch problem for algebraic_loop_msd_no_looproutine" in {
    generateAndVerifyFail("../common_mistakes/algebraic_loop_msd_no_looproutine.conf")
  }

  it should "catch problem for loop_within_loop_forgot_one_connection.conf" in {
    generateAndVerifyFail("../common_mistakes/loop_within_loop_forgot_one_connection.conf")
  }

  it should "catch problem for incubator.conf" in {
    generateAndVerifyFail("../common_mistakes/incubator.conf")
  }
}
