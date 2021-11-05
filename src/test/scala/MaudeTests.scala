import java.io.{BufferedWriter, File, FileWriter, PrintWriter}
import java.nio.file.Files

import core.{MaudeModelEncoding, ScenarioLoader}
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

  def generateAndVerify(resourcesFile: String, name : String) = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val encoding = new MaudeModelEncoding(ScenarioLoader.load(conf))
    val encodingString = encoding.scenario

    val file = new File(name + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(encodingString)
    bw.close()
    ///val result = MaudeScenarioGenerator.generate(encoding)
    //assert(MaudeRunner.checkEnvironment())
    //MaudeRunner.verify(f, fullMaudeFile) should be (0)
    //FileUtils.deleteQuietly(f)
  }


  "ScenarioGenerator" should "work for water_tank.conf" in {
    generateAndVerify("examples/water_tank.conf", "water_tank")
  }

  "ScenarioGenerator" should "work for simple_master.conf" in {
    generateAndVerify("examples/simple_master.conf", "simple_master")
  }

  it should "work for algebraic_loop_msd" in {
    generateAndVerify("examples/algebraic_loop_msd_gs.conf", "algebraic_loop_msd_gs")
  }

  it should "work for algebraic_loop_msd_fail_converge" in {
    generateAndVerify("examples/algebraic_loop_msd_jac.conf", "algebraic_loop_msd_jac")
  }

  it should "work for two_algebraic_loops" in {
    generateAndVerify("examples/two_algebraic_loops.conf", "two_algebraic_loops")
  }

  it should "work for simple_master_step_sizes" in {
    generateAndVerify("examples/simple_master_step_sizes.conf", "simple_master_step_sizes")
  }


  it should "work for industrial" in {
    generateAndVerify("examples/industrial_casestudy.conf","industrial_casestudy")
  }

  it should "work for industrial_case_alt" in {
    generateAndVerify("examples/industrial_casestudy_alt.conf", "industrial_casestudy_alt")
  }

  it should "work for simple_master_can_reject_step.conf" in {
    generateAndVerify("examples/simple_master_can_reject_step.conf", "simple_master_can_reject_step")
  }

  it should "work for step_finding_loop_msd_1.conf" in {
    generateAndVerify("examples/step_finding_loop_msd_1.conf", "step_finding_loop_msd_1")
  }

  it should "work for algebraic_loop_initialization.conf" in {
    generateAndVerify("examples/algebraic_loop_initialization.conf", "algebraic_loop_initialization")
  }

  it should "work for loop_within_loop.conf" in {
    generateAndVerify("examples/loop_within_loop.conf", "loop_within_loop")
  }
}
