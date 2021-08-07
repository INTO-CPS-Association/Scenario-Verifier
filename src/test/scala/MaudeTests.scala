import java.io.PrintWriter
import java.nio.file.Files

import cli.{MaudeRunner, VerifyTA}
import core.{MaudeModelEncoding, MaudeScenarioGenerator, ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.Ignore
import org.scalatest.flatspec._
import org.scalatest.matchers._

@Ignore
class MaudeTests extends AnyFlatSpec with should.Matchers {

  def writeToTempFile(content: String) = {
    val file = Files.createTempFile("maude_", ".maude").toFile
    new PrintWriter(file) { write(content); close() }
    file
  }

  def generateAndVerify(resourcesFile: String) = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val fullMaudeFile = getClass.getResource("full-maude.maude").getPath
    val encoding = new MaudeModelEncoding(ScenarioLoader.load(conf))
    val result = MaudeScenarioGenerator.generate(encoding)
    val f = writeToTempFile(result)
    assert(MaudeRunner.checkEnvironment())
    MaudeRunner.verify(f, fullMaudeFile) should be (0)
    FileUtils.deleteQuietly(f)
  }

  "ScenarioGenerator" should "work for simple_master.conf" in {
    generateAndVerify("examples/simple_master.conf")
  }
/*
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

  it should "work for loop_within_loop_alt.conf" in {
    generateAndVerify("examples/loop_within_loop_alt.conf")
  }

 */
}
