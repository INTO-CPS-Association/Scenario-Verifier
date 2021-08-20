import cli.VerifyTA
import core.{ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.flatspec._
import org.scalatest.matchers._
import java.io.PrintWriter
import java.nio.file.Files

import api.VerificationAPI

class NegativeTests extends AnyFlatSpec with should.Matchers {
  def generateAndVerifyFail(resourcesFile: String) = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoader.load(conf)
    assert(!VerificationAPI.verifyAlgorithm(masterModel))
  }

  "ScenarioGenerator" should "catch problem with simple_master_reactivity.conf" in {
    generateAndVerifyFail("common_mistakes/simple_master_reactivity.conf")
  }

  "ScenarioGenerator" should  "catch problem for simple_master_can_reject_step.conf" in {
    generateAndVerifyFail("common_mistakes/simple_master_can_reject_step.conf")
  }

  "ScenarioGenerator" should  "catch problem for simple_master_step_by_0.conf" in {
    generateAndVerifyFail("common_mistakes/simple_master_step_by_0.conf")
  }

  "ScenarioGenerator" should  "catch problem for simple_master_get_set_wrong" in {
    generateAndVerifyFail("common_mistakes/simple_master_get_set_wrong.conf")
  }

  "ScenarioGenerator" should  "catch problem for simple_master_forget_connection" in {
    generateAndVerifyFail("common_mistakes/simple_master_forget_connection.conf")
  }

  "ScenarioGenerator" should  "catch problem for algebraic_loop_msd_no_looproutine" in {
    generateAndVerifyFail("common_mistakes/algebraic_loop_msd_no_looproutine.conf")
  }

  "ScenarioGenerator" should  "catch problem for loop_within_loop_forgot_one_connection.conf" in {
    generateAndVerifyFail("common_mistakes/loop_within_loop_forgot_one_connection.conf")
  }

  "ScenarioGenerator" should  "catch problem for incubator.conf" in {
    generateAndVerifyFail("common_mistakes/incubator.conf")
  }

  "ScenarioGenerator" should  "catch problem in initialization for incubator1.conf" in {
    generateAndVerifyFail("common_mistakes/incubator1.conf")
  }

}
