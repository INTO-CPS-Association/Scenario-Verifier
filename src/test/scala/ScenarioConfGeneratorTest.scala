import java.io.ByteArrayInputStream

import core.{ConnectionModel, FmuModel, ScenarioLoader}
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer.ConfParser.ScenarioConfGenerator

class ScenarioConfGeneratorTest extends AnyFlatSpec with should.Matchers {
  private def confGenerationTest(resourcesFile: String) = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val scenario = ScenarioLoader.load(conf)
    val generateScenario = ScenarioConfGenerator.generate(scenario, scenario.name)
    val scenarioFromGeneratedSource = ScenarioLoader.load(new ByteArrayInputStream(generateScenario.getBytes()))
    //All connections are equivalent
    assert(compareConnections(scenario.scenario.connections, scenarioFromGeneratedSource.scenario.connections))
    //All FMUs are equivalent
    assert(compareFMUs(scenario.scenario.fmus, scenarioFromGeneratedSource.scenario.fmus))

    assert(scenario.initialization == scenarioFromGeneratedSource.initialization)
    assert(scenario.cosimStep == scenarioFromGeneratedSource.cosimStep)
  }

  private def compareConnections(c1: List[ConnectionModel], c2: List[ConnectionModel]): Boolean = {
    c1.forall(i => c2.contains(i))
  }

  private def compareFMUs(FMUsOriginal: Map[String, FmuModel], FMUsGenerated: Map[String, FmuModel]): Boolean = {
    FMUsOriginal.forall(fmu => FMUsGenerated.exists(i => i == fmu))
  }

  "ScenarioConfGenerator" should "create valid Master Algorithm for Simple Master" in {
    confGenerationTest("examples/simple_master.conf")
  }

  "ScenarioConfGenerator" should "create valid Master Algorithm for Algebraic Initialization" in{
    confGenerationTest("examples/algebraic_loop_initialization.conf")
  }
  "ScenarioConfGenerator" should "create valid Master Algorithm for Industrial case study" in{
    confGenerationTest("examples/industrial_casestudy.conf")
  }

  "ScenarioConfGenerator" should "create valid Step procedure for Two Algebraic Loops" in {
    confGenerationTest("examples/two_algebraic_loops.conf")
  }
}