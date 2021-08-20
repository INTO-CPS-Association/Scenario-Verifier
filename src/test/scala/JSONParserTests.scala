import java.io.{File, PrintWriter}
import java.nio.file.Files

import cli.VerifyTA
import core.{MasterModelDTO, ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.flatspec._
import org.scalatest.matchers._

class JSONParserTests extends AnyFlatSpec with should.Matchers {

  def writeToTempFile(content: String) = {
    val file = Files.createTempFile("uppaal_", ".xml").toFile
    new PrintWriter(file) { write(content); close() }
    file
  }


  def generateAndVerify(resourcesFile: String) = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoader.loadJson(conf)
    val encoding = new ModelEncoding(masterModel)
    val result = ScenarioGenerator.generate(encoding)
    val f = writeToTempFile(result)
    assert(VerifyTA.checkEnvironment())
    VerifyTA.verify(f) should be (0)
    FileUtils.deleteQuietly(f)
  }

  "JSONParserTests" should "work for simple_master.json" in {
    generateAndVerify("examples/JSON/simple_master_algorithm_where_an_FMU_can_reject_step_sizes.json")
  }

  "JSONParserTests" should "work for simple_master_algorithm.json" in {
    generateAndVerify("examples/JSON/simple_master_algorithm.json")
  }

  "JSONParserTests" should "work for step finding.json" in {
    generateAndVerify("examples/JSON/Master_step_finding_loop.json")
  }

  "JSONParserTests" should "work for nested_loop.json" in {
    generateAndVerify("examples/JSON/nested_loop.json")
  }

  "JSONParserTests" should "work for industrial_case_study.json" in {
    generateAndVerify("examples/JSON/Industrial_case_study.json")
  }

  "JSONParserTests" should "work for Master_algebraic_loop_in_initialization.json" in {
    generateAndVerify("examples/JSON/Master_algebraic_loop_in_initialization.json")
  }
}
