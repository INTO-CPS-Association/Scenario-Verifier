import api.{TraceResult, VerificationAPI}
import core.ScenarioLoader
import org.scalatest.flatspec._
import org.scalatest.matchers._
import org.scalatest.{Assertion, Ignore}

@Ignore
class TraceTester extends AnyFlatSpec with should.Matchers {

  def generateTrace(scenarioPath: String): TraceResult = {
    val conf = getClass.getResourceAsStream(scenarioPath)
    val masterModel = ScenarioLoader.load(conf)
    VerificationAPI.generateTraceFromMasterModel(masterModel)
  }

  def generateTraceExist(scenarioPath: String): Assertion = {
    val TraceResult(file, isGenerated) = generateTrace(scenarioPath)
    assert(file.exists())
    assert(isGenerated)
  }

  def generateTraceDoNotExist(scenarioPath: String): Assertion = {
    val TraceResult(_, isGenerated) = generateTrace(scenarioPath)
    assert(!isGenerated)
  }

  "TraceTester" should "work for simple master" in {
    generateTraceExist("common_mistakes/simple_master_forget_connection.conf")
  }
  "TraceTester" should "work for step finding" in {
    generateTraceExist("common_mistakes/step_finding_loop_msd_forget_connection.conf")
  }

  "TraceTester" should "work for simple" in {
    generateTraceExist("common_mistakes/simple_master_can_reject_step.conf")
  }
  "TraceTester" should "work for industrial" in {
    generateTraceExist("common_mistakes/industrial_missing_step.conf")
  }

  "TraceTester" should "work for loop" in {
    generateTraceExist("common_mistakes/algebraic_loop_msd_gs_forget_restore.conf")
  }

  "TraceTester" should "work for nested loop" in {
    generateTraceExist("common_mistakes/loop_within_loop_forgot_one_connection.conf")
  }

  "TraceTester" should "work for nested loop alt" in {
    generateTraceExist("examples/loop_within_loop_alt.conf")
  }

  it should "work for incubator.conf" in {
    generateTraceExist("common_mistakes/incubator.conf")
  }

  it should "work for incubator1.conf" in {
    generateTraceExist("common_mistakes/incubator1.conf")
  }

  it should "not generate a trace for simple_master.conf" in {
    generateTraceDoNotExist("examples/simple_master.conf")
  }

  it should "not generate a trace for loop_within_loop.conf" in {
    generateTraceDoNotExist("examples/loop_within_loop.conf")
  }
}
