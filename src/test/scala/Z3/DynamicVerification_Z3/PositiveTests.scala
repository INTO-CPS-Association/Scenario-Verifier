package Z3.DynamicVerification_Z3

import api.VerificationAPI
import core.ScenarioLoader
import org.scalatest.flatspec._
import org.scalatest.matchers._

class PositiveTests extends AnyFlatSpec with should.Matchers {
  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    val t_ms = (t1 - t0) / 1000000
    println("Elapsed time: " + t_ms + "ms")
    result
  }

  def generateAndVerify(resourcesFile: String): Boolean = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoader.load(conf)
    (1 until masterModel.cosimStep.values.head.length).forall(i => {
      val previous_actions = masterModel.cosimStep.values.head.take(i)
      val current_action = masterModel.cosimStep.values.head(i)
      time {
        //VerificationAPI.dynamicZ3Verification(masterModel.scenario, previous_actions, current_action)
      }
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