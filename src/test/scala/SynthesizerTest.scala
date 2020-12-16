import java.io.{File, PrintWriter}
import java.nio.file.Files

import cli.VerifyTA
import core.{MasterModel, ModelEncoding, PortRef, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.Ignore
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer._

class SynthesizerTest extends AnyFlatSpec with should.Matchers {

  def writeToTempFile(content: String) = {
    val file = Files.createTempFile("uppaal_", ".xml").toFile
    new PrintWriter(file) { write(content); close() }
    file
  }

  def synthesizeAndVerify(resourcesFile: String) = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val scenario = ScenarioLoader.load(conf)
    val synthesizer = new Synthesizer(scenario.scenario, LoopStrategy.maximum)
    val step = synthesizer.synthesizeStep()
    val init =  ScenarioLoader.generateEnterInitInstructions(scenario.scenario) ++ synthesizer.synthesizeInitialization() ++ ScenarioLoader.generateExitInitInstructions(scenario.scenario)
    val model = MasterModel(scenario.name, scenario.scenario, instantiation = scenario.instantiation, initialization = init.toList, cosimStep = step, terminate = scenario.terminate)
    val encoding = new ModelEncoding(model)
    val result = ScenarioGenerator.generate(encoding)
    val f = writeToTempFile(result)
    assert(VerifyTA.checkEnvironment())
    VerifyTA.verify(f) should be (0)
    FileUtils.deleteQuietly(f)
  }

  "Synthesizer" should "create valid Master Algorithm for Algebraic Initialization" in{
    synthesizeAndVerify("examples/algebraic_loop_initialization.conf")
  }

  "Synthesizer" should "create valid Master Algorithm for Simple Master" in {
    synthesizeAndVerify("examples/simple_master.conf")
  }

  "Synthesizer" should "create valid Master Algorithm for Industrial case study" in{
    synthesizeAndVerify("examples/industrial_casestudy.conf")
  }

  ignore should "create valid Step procedure for Two Algebraic Loops" in {
    synthesizeAndVerify("examples/two_algebraic_loops.conf")
  }
}
