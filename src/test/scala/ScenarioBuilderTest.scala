import api.VerificationAPI
import org.scalatest.{Assertion, Ignore}
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer._

import scala.collection.immutable.HashSet
@Ignore
class ScenarioBuilderTest extends AnyFlatSpec with should.Matchers {
  def generateSynthesisAndVerify(scenarioName: String, nFMU: Int, nConnection: Int, feedthrough: Boolean = true, canRejectStep: Boolean = false): Assertion = {
    val scenario = ScenarioBuilder.generateScenario(nFMU, nConnection, feedthrough, canRejectStep)
    assert(VerificationAPI.synthesizeAndVerify(scenarioName, scenario))
  }

  it should "create valid Simple example" in {
    generateSynthesisAndVerify("Test_Simple_Example", 2, 2)
  }

  it should "create valid Bigger example" in {
    generateSynthesisAndVerify("Big_Advanced_Example", 10, 20)
  }

  it should "create valid Big and Advanced example" in {
    generateSynthesisAndVerify("Big_Very_Advanced_Example", 5, 10, canRejectStep = true)
  }

  it should "create valid Big and Advanced with Feedthrough example" in {
    generateSynthesisAndVerify("Big_Very_Advanced_Example", 10, 20, canRejectStep = true)
  }

  it should "create valid Big example with no Feedthrough" ignore  {
    generateSynthesisAndVerify("Test_Simple_Big_Example", 20, 40, feedthrough = false, canRejectStep = true)
  }

  it should "create very big simple example" ignore  {
    generateSynthesisAndVerify("Big_Simple_Example", 100, 200, feedthrough = false)
  }

  it should "create valid Connection example" in {
    val fmuNames = HashSet("fmu1", "fmu2")
    val connections = ScenarioBuilder.generateConnections(fmuNames, 2)
    assert(connections.exists(o => o.srcPort.fmu == fmuNames.toVector(0)) && connections.exists(o => o.srcPort.fmu == fmuNames.toVector(1)))
    assert(connections.exists(o => o.trgPort.fmu == fmuNames.toVector(0)) && connections.exists(o => o.trgPort.fmu == fmuNames.toVector(1)))
  }
}
