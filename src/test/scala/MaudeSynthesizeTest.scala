import core.ScenarioLoader
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer.ConfParser.MaudeScenarioGenerator

class MaudeSynthesizeTest extends AnyFlatSpec with should.Matchers {

  def generateScenario(scenarioPath: String, scenarioName: String) = {
    val conf = getClass.getResourceAsStream(scenarioPath)
    val masterModel = ScenarioLoader.load(conf)

    MaudeScenarioGenerator.generateScenario(masterModel.scenario, scenarioName)
  }

  "MaudeSynthesizeTest" should "work for simple master" in {
    generateScenario("common_mistakes/simple_master_forget_connection.conf", "simple_master_forget_connection")
  }

  "MaudeSynthesizeTest" should "work for step finding" in {
    generateScenario("common_mistakes/step_finding_loop_msd_forget_connection.conf", "step_finding_loop_msd_forget_connection")
  }

  "MaudeSynthesizeTest" should "work for simple" in {
    generateScenario("common_mistakes/simple_master_can_reject_step.conf", "simple_master_can_reject")
  }
}
