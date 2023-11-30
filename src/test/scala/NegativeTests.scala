import org.intocps.verification.scenarioverifier.api.VerificationAPI
import org.intocps.verification.scenarioverifier.core.{ScenarioGenerator, ScenarioLoader}
import org.scalatest.Assertion
import org.scalatest.flatspec._
import org.scalatest.matchers._

class NegativeTests extends AnyFlatSpec with should.Matchers {
  def generateAndVerifyFail(resourcesFile: String): Assertion = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoader.load(conf)
    assert(!VerificationAPI.verifyAlgorithm(masterModel))
  }

  it should "catch problem with simple_master_reactivity.conf" in {
    generateAndVerifyFail("common_mistakes/simple_master_reactivity.conf")
  }

  it should "catch problem for simple_master_can_reject_step.conf" in {
    generateAndVerifyFail("common_mistakes/simple_master_can_reject_step.conf")
  }

  /*
  it should "catch problem for simple_master_step_by_0.conf" in {
    val conf = getClass.getResourceAsStream("common_mistakes/simple_master_step_by_0.conf")
    val masterModel = ScenarioLoader.load(conf)
    assertThrows(VerificationAPI.verifyAlgorithm(masterModel, ScenarioGenerator.generateUppaalFile))
  }
   */

  it should "catch problem for simple_master_get_set_wrong" in {
    generateAndVerifyFail("common_mistakes/simple_master_get_set_wrong.conf")
  }

  it should "catch problem for simple_master_forget_connection" in {
    generateAndVerifyFail("common_mistakes/simple_master_forget_connection.conf")
  }

  it should "catch problem for algebraic_loop_msd_no_looproutine" in {
    generateAndVerifyFail("common_mistakes/algebraic_loop_msd_no_looproutine.conf")
  }

  it should "catch problem for loop_within_loop_forgot_one_connection.conf" in {
    generateAndVerifyFail("common_mistakes/loop_within_loop_forgot_one_connection.conf")
  }

  it should "catch problem for incubator.conf" in {
    generateAndVerifyFail("common_mistakes/incubator.conf")
  }

  it should "catch problem in initialization for incubator1.conf" in {
    generateAndVerifyFail("common_mistakes/incubator1.conf")
  }
}
