import api.VerificationAPI
import core.{ScenarioGenerator, ScenarioLoader}
import org.scalatest.Assertion
import org.scalatest.flatspec._
import org.scalatest.matchers._

class PositiveTests extends AnyFlatSpec with should.Matchers {
  def generateAndVerify(resourcesFile: String): Assertion = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoader.load(conf)
    assert(VerificationAPI.verifyAlgorithm(masterModel, ScenarioGenerator.generateUppaalFile))
  }

  it should "work for simple_master.conf" in {
    generateAndVerify("examples/simple_master.conf")
  }

  it should "work for feedthrough loops" in {
    generateAndVerify("examples/algebraic_loop_feedthrough.conf")
  }

  it should "work for simple_adaptive_master.conf" in {
    generateAndVerify("examples/simple_master_adaptive.conf")
  }

  it should "work for algebraic_loop_msd" in {
    generateAndVerify("examples/algebraic_loop_msd_gs.conf")
  }

  it should "work for algebraic_loop_msd_fail_converge" in {
    generateAndVerify("examples/algebraic_loop_msd_jac.conf")
  }

  it should "work for two_algebraic_loops" in {
    generateAndVerify("examples/two_algebraic_loops.conf")
  }

  it should "work for simple_master_step_sizes" in {
    generateAndVerify("examples/simple_master_step_sizes.conf")
  }

  it should "work for industrial" in {
    generateAndVerify("examples/industrial_casestudy.conf")
  }

  it should "work for industrial_case_alt" in {
    generateAndVerify("examples/industrial_casestudy_alt.conf")
  }

  it should "work for simple_master_can_reject_step.conf" in {
    generateAndVerify("examples/simple_master_can_reject_step.conf")
  }

  it should "work for step_finding_loop_msd_1.conf" in {
    generateAndVerify("examples/step_finding_loop_msd_1.conf")
  }

  it should "work for algebraic_loop_initialization.conf" in {
    generateAndVerify("examples/algebraic_loop_initialization.conf")
  }

  it should "work for loop_within_loop.conf" in {
    generateAndVerify("examples/loop_within_loop.conf")
  }
}
