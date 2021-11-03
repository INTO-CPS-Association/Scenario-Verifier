import java.io.PrintWriter
import java.nio.file.{Files, Paths}

import api.{TraceResult, VerificationAPI}
import cli.VerifyTA
import core.{ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.Ignore
import org.scalatest.flatspec._
import org.scalatest.matchers._
import trace_analyzer.TraceAnalyzer

@Ignore
class TraceTester extends AnyFlatSpec with should.Matchers {

  def generateTrace(scenarioPath: String, scenarioName: String) = {
    val conf = getClass.getResourceAsStream(scenarioPath)
    val masterModel = ScenarioLoader.load(conf)
    VerificationAPI.generateTraceFromMasterModel(masterModel)
  }

  def generateTraceExist(scenarioPath: String, scenarioName: String) = {
    val TraceResult(file, isGenerated) = generateTrace(scenarioPath, scenarioName)
    assert(file.exists())
    assert(isGenerated)
  }

  def generateTraceDoNotExist(scenarioPath: String, scenarioName: String) = {
    val TraceResult(file, isGenerated) = generateTrace(scenarioPath, scenarioName)
    assert(!isGenerated)
  }

  "TraceTester" should "work for simple master" in {
    generateTraceExist("common_mistakes/simple_master_forget_connection.conf", "simple_master_forget_connection")
  }
  "TraceTester" should "work for step finding" in {
    generateTraceExist("common_mistakes/step_finding_loop_msd_forget_connection.conf", "step_finding_loop_msd_forget_connection")
  }

  "TraceTester" should "work for simple" in {
    generateTraceExist("common_mistakes/simple_master_can_reject_step.conf", "simple_master_can_reject")
  }
  "TraceTester" should "work for industrial" in {
    generateTraceExist("common_mistakes/industrial_missing_step.conf", "industrial_missing_step")
  }

  "TraceTester" should "work for loop" in {
    generateTraceExist("common_mistakes/algebraic_loop_msd_gs_forget_restore.conf", "algebraic_loop_msd_gs_forget_restore")
  }

  "TraceTester" should "work for nested loop" in {
    generateTraceExist("common_mistakes/loop_within_loop_forgot_one_connection.conf", "Example master that has a loop within a loop")
  }

  "TraceTester" should "work for nested loop alt" in {
    generateTraceExist("examples/loop_within_loop_alt.conf", "Example master that has a loop within a loop")
  }

  it should "work for incubator.conf" in {
    generateTraceExist("common_mistakes/incubator.conf", "Incubator video")
  }

  it should "work for incubator1.conf" in {
    generateTraceExist("common_mistakes/incubator1.conf", "Incubator1 video")
  }

  it should "not generate a trace for simple_master.conf" in {
    generateTraceDoNotExist("examples/simple_master.conf", "simple_master video")
  }

  it should "not generate a trace for loop_within_loop.conf" in {
    generateTraceDoNotExist("examples/loop_within_loop.conf", "Loop within loop video")
  }
}
