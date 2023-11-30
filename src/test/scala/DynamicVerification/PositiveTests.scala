package DynamicVerification

import org.intocps.verification.scenarioverifier.api.VerificationAPI
import org.intocps.verification.scenarioverifier.core.ScenarioLoader
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
        VerificationAPI.dynamicVerification(masterModel.scenario, previous_actions, current_action)
      }
      true
    })
  }

  def dynamicVerifyLongAlgorithm(resourcesFile: String): Boolean = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoader.load(conf)
    val algorithm = masterModel.cosimStep.values.head
    val hundredThousandRepetitionsOfAlgorithm  = (1 to 10000).flatMap(_ => algorithm).toList
    (1 until algorithm.length).forall(i => {
      val previous_actions = hundredThousandRepetitionsOfAlgorithm ++ algorithm.take(i)
      val current_action = algorithm(i)
      time {
        VerificationAPI.dynamicVerification(masterModel.scenario, previous_actions, current_action)
      }
      true
    })
  }

  it should "work for simple_master_fmi3.conf" in {
    generateAndVerify("../examples/simple_master_fmi3.conf")
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

  it should "work for simple_master_can_reject_step.conf" in {
    generateAndVerify("../examples/simple_master_can_reject_step.conf")
  }

  it should "work for step_finding_loop_msd_1.conf" in {
    generateAndVerify("../examples/step_finding_loop_msd_1.conf")
  }

  it should "work for algebraic_loop_initialization.conf" in {
    generateAndVerify("../examples/algebraic_loop_initialization.conf")
  }

  it should "work for dynamic scenario 1 - long algorithm" in {
    dynamicVerifyLongAlgorithm("../examples/dynamic_state_estimation_scenario_1.conf")
  }

  it should "work for dynamic scenario 1" in {
    generateAndVerify("../examples/dynamic_state_estimation_scenario_1.conf")
  }

  it should "work for dynamic scenario 2" in {
    generateAndVerify("../examples/dynamic_state_estimation_scenario_2.conf")
  }

  it should "work for loop_within_loop.conf" in {
    generateAndVerify("../examples/loop_within_loop.conf")
  }
}
