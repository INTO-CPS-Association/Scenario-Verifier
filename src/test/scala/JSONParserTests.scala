import cli.VerifyTA
import core.{ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.flatspec._
import org.scalatest.matchers._
import java.nio.file.Files
import scala.reflect.io.Directory

class JSONParserTests extends AnyFlatSpec with should.Matchers {
  def generateAndVerify(resourcesFile: String): Boolean = {
    require(VerifyTA.isInstalled, "Uppaal is not installed, please install it and add it to your PATH")
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoader.loadJson(conf)
    val encoding = new ModelEncoding(masterModel)
    val folder = Files.createTempDirectory("uppaal_").toFile
    val f = ScenarioGenerator.generateUppaalFile(masterModel.name, encoding, Directory(folder))
    VerifyTA.verify(f) should be(0)
    FileUtils.deleteQuietly(f)
    FileUtils.deleteQuietly(folder)
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
