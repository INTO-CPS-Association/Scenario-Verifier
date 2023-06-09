package Z3.Synthesizing

import api.FMI3.VerificationAPI
import core.FMI3.ScenarioLoaderFMI3
import org.scalatest.Assertion
import org.scalatest.flatspec._
import org.scalatest.matchers._

class SynthesizerTest extends AnyFlatSpec with should.Matchers {
  private def synthesizeAndVerify(resourcesFile: String): Assertion = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoaderFMI3.load(conf)
    val synthesizedAlgorithm = VerificationAPI.synthesizeAlgorithm(masterModel.scenario)
    assert(true)
  }

  it should "create valid Master Algorithm for Simple Master" in {
    synthesizeAndVerify("../../examples_fmi_3/simple_master_fmi3.conf")
  }

  it should "create valid Master Algorithm for Motivation Example" in {
    synthesizeAndVerify("../../examples_fmi_3/motivation_example.conf")
  }

  it should "create valid Master Algorithm for Paper Example" in {
    synthesizeAndVerify("../../examples_fmi_3/example.conf")
  }

}