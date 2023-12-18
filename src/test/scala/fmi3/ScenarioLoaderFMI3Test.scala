package fmi3

import org.intocps.verification.scenarioverifier.core.ScenarioLoaderFMI3
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.Assertion

class ScenarioLoaderFMI3Test extends AnyFlatSpec with should.Matchers {

  private def confGenerationTest(
      resourcesFile: String,
      nFMUs: Int,
      nConnections: Int,
      nClockedConnections: Int,
      nInitializationInstructions: Int,
      nStepSizeInstructions: Int,
      nEventStrategies: Int): Assertion = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val scenario = ScenarioLoaderFMI3.load(conf)
    assert(scenario.scenario.fmus.size == nFMUs)
    assert(scenario.scenario.connections.size == nConnections)
    assert(scenario.scenario.clockConnections.size == nClockedConnections)
    assert(scenario.initialization.size - (2 * nFMUs) == nInitializationInstructions)
    assert(scenario.cosimStep.head._2.size == nStepSizeInstructions)
    assert(scenario.eventStrategies.size == nEventStrategies)
  }

  it should "be able to load a simple scenario Master Algorithm for Simple Master" in {
    confGenerationTest("../examples_fmi_3/simple_master_fmi3.conf", 2, 3, 0, 6, 8, 0)
  }

  it should "be able to load the motivation example" in {
    confGenerationTest("../examples_fmi_3/motivation_example.conf", 5, 6, 1, 3, 8, 0)
  }
}