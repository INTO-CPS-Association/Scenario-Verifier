package fmi3

import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier.api.FMI3.Verification
import org.intocps.verification.scenarioverifier.core.FMI3.ScenarioLoaderFMI3
import org.intocps.verification.scenarioverifier.synthesizer.ScenarioBuilder.FMI3ScenarioBuilder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ScalabilityTest extends AnyFlatSpec with should.Matchers with Logging {

  def FMI3SynthesizingScalability() = {
    val testScenario1 = "../examples_fmi_3/motivation_example.conf"
    val conf1 = getClass.getResourceAsStream(testScenario1)
    val scenario1 = ScenarioLoaderFMI3.load(conf1)
    val testScenario2 = "../examples_fmi_3/powersystem.conf"
    val conf2 = getClass.getResourceAsStream(testScenario2)
    val scenario2 = ScenarioLoaderFMI3.load(conf2)
    val scenario3 = FMI3ScenarioBuilder.generateScenario(10, 10, 5, 5, supportFeedthrough = false)
    logger.info("Synthesizing algorithm for scenario 1")
    Verification.synthesizeAlgorithm(scenario1.scenario)
    logger.info("Synthesizing algorithm for scenario 2")
    Verification.synthesizeAlgorithm(scenario2.scenario)
    logger.info("Synthesizing algorithm for scenario 3")
    Verification.synthesizeAlgorithm(scenario3)
  }

  it should "create valid Master Algorithm for Simple Master" in {
    FMI3SynthesizingScalability()
  }

}
