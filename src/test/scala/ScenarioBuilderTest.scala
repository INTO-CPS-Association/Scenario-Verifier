import java.io.{File, PrintWriter}
import java.nio.file.Files

import api.VerificationAPI
import cli.VerifyTA
import core.{MasterModel, ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.Ignore
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer.LoopStrategy.{LoopStrategy, maximum, minimum}
import synthesizer._

import scala.collection.immutable.HashSet
@Ignore
class ScenarioBuilderTest extends AnyFlatSpec with should.Matchers {
  def generateSynthesisAndVerify(scenarioName: String, nFMU: Int, nConnection: Int, feedthrough: Boolean = true, canRejectStep: Boolean = false) = {
    val scenario = ScenarioBuilder.generateScenario(nFMU, nConnection, feedthrough, canRejectStep)
    assert(VerificationAPI.generateAndVerify(scenarioName, scenario))
  }

  "ScenarioBuilderTest" should "create valid Simple example" in {
    generateSynthesisAndVerify("Test_Simple_Example", 2, 2)
  }

  "ScenarioBuilderTest" should "create valid Bigger example" in {
    generateSynthesisAndVerify("Big_Advanced_Example", 10, 20)
  }

  "ScenarioBuilderTest" should "create valid Big and Advanced example" in {
    generateSynthesisAndVerify("Big_Very_Advanced_Example", 5, 10, true, true)
  }

  "ScenarioBuilderTest" should "create valid Big and Advanced with Feedthrough example" in {
    generateSynthesisAndVerify("Big_Very_Advanced_Example", 10, 20, true, true)
  }

  "ScenarioBuilderTest" should "create valid Big example with no Feedthrough" ignore  {
    generateSynthesisAndVerify("Test_Simple_Big_Example", 20, 40, false, true)
  }

  "ScenarioBuilderTest" should "create very big simple example" ignore  {
    generateSynthesisAndVerify("Big_Simple_Example", 100, 200, false, false)
  }

  "ScenarioBuilderTest" should "create valid Connection example" in {
    val fmuNames = HashSet("fmu1", "fmu2")
    val connections = ScenarioBuilder.generateConnections(fmuNames, 2)
    assert(connections.exists(o => o.srcPort.fmu == fmuNames.toVector(0)) && connections.exists(o => o.srcPort.fmu == fmuNames.toVector(1)))
    assert(connections.exists(o => o.trgPort.fmu == fmuNames.toVector(0)) && connections.exists(o => o.trgPort.fmu == fmuNames.toVector(1)))
  }
}
