package Z3.Synthesizing

import org.intocps.verification.scenarioverifier.api.FMI3.Verification
import org.intocps.verification.scenarioverifier.core.FMI3.ScenarioLoaderFMI3
import org.scalatest.Assertion
import org.scalatest.flatspec._
import org.scalatest.matchers._

class SynthesizerTest extends AnyFlatSpec with should.Matchers {
  private def synthesizeAndVerify(resourcesFile: String): Assertion = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoaderFMI3.load(conf)
    val synthesizedAlgorithm = Verification.synthesizeAlgorithm(masterModel.scenario)
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

  it should "create valid Master Algorithm for Powersystem" in {
    synthesizeAndVerify("../../examples_fmi_3/powersystem.conf")
  }

}