package Z3

import api.FMI3.Verification
import core.FMI3.ScenarioLoaderFMI3
import org.apache.logging.log4j.scala.Logging
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import synthesizer.ScenarioBuilder.FMI3ScenarioBuilder

class ScalabilityTest extends AnyFlatSpec with should.Matchers with Logging {

  def FMI3SynthesizingScalability() = {
    val testScenario1 = "../examples_fmi_3/motivation_example.conf"
    val conf1 = getClass.getResourceAsStream(testScenario1)
    val scenario1 = ScenarioLoaderFMI3.load(conf1)
    val testScenario2 = "../examples_fmi_3/powersystem.conf"
    val conf2 = getClass.getResourceAsStream(testScenario2)
    val scenario2 = ScenarioLoaderFMI3.load(conf2)
    val scenario3 = FMI3ScenarioBuilder.generateScenario(10, 10, 5, 5, supportFeedthrough = false)
    val scenario4 = FMI3ScenarioBuilder.generateScenario(50, 50, 10, 10, supportFeedthrough = true)
    val scenario5 = FMI3ScenarioBuilder.generateScenario(100, 100, 10, 10, supportFeedthrough = false)
    logger.info("Synthesizing algorithm for scenario 1" )
    Verification.synthesizeAlgorithmWithStatistics(scenario1.scenario)
    logger.info("Synthesizing algorithm for scenario 2" )
    Verification.synthesizeAlgorithmWithStatistics(scenario2.scenario)
    logger.info("Synthesizing algorithm for scenario 3" )
    Verification.synthesizeAlgorithmWithStatistics(scenario3)
    logger.info("Synthesizing algorithm for scenario 4" )
    Verification.synthesizeAlgorithmWithStatistics(scenario4)
    logger.info("Synthesizing algorithm for scenario 5" )
    Verification.synthesizeAlgorithmWithStatistics(scenario5)
  }

  it should "create valid Master Algorithm for Simple Master" in {
    FMI3SynthesizingScalability()
  }

}
