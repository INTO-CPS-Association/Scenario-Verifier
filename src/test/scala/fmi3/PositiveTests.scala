package fmi3

import org.intocps.verification.scenarioverifier.api.FMI3.Verification
import org.intocps.verification.scenarioverifier.core.FMI3.ScenarioLoaderFMI3
import org.scalatest.flatspec._
import org.scalatest.matchers._
import org.scalatest.Assertion

class PositiveTests extends AnyFlatSpec with should.Matchers {
  def generateAndVerify(resourcesFile: String): Assertion = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoaderFMI3.load(conf)
    assert(Verification.verifyAlgorithm(masterModel))
  }

  it should "work for example.conf" in {
    generateAndVerify("../examples_fmi_3/example.conf")
  }

  it should "work for predatorprey.conf" in {
    generateAndVerify("../examples_fmi_3/simple_master_fmi3.conf")
  }
}
