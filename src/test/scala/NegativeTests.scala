import org.intocps.verification.scenarioverifier.api.VerificationAPI
import org.intocps.verification.scenarioverifier.core.ScenarioLoaderFMI2
import org.scalatest.flatspec._
import org.scalatest.matchers._
import org.scalatest.Assertion

class NegativeTests extends AnyFlatSpec with should.Matchers {
  def generateAndVerifyFail(resourcesFile: String): Assertion = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoaderFMI2.load(conf)
    assert(!VerificationAPI.verifyAlgorithm(masterModel))
  }

  it should "catch problem with simple_master_reactivity.conf" in {
    generateAndVerifyFail("common_mistakes/simple_master_reactivity.conf")
  }

  it should "catch problem for simple_master_can_reject_step.conf" in {
    generateAndVerifyFail("common_mistakes/simple_master_can_reject_step.conf")
  }

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
