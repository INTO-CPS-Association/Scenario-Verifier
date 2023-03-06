import core.{AlgebraicLoopInit, ConnectionModel, EnterInitMode, ExitInitMode, FmuModel, InitGet, InitSet, ScenarioLoader}
import org.scalatest.flatspec._
import org.scalatest.matchers._

import java.io.ByteArrayInputStream

class ScenarioConfGeneratorTest extends AnyFlatSpec with should.Matchers {
  private def confGenerationTest(resourcesFile: String) = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val scenario = ScenarioLoader.load(conf)
    val generatedConfiguration = scenario.toConf()
    val scenarioFromGeneratedSource = ScenarioLoader.load(new ByteArrayInputStream(generatedConfiguration.getBytes()))
    //All connections are equivalent
    assert(compareConnections(scenario.scenario.connections, scenarioFromGeneratedSource.scenario.connections))
    //All FMUs are equivalent
    assert(compareFMUs(scenario.scenario.fmus, scenarioFromGeneratedSource.scenario.fmus))
    assert(scenario.initialization.filterNot {
      case EnterInitMode(_) => true
      case ExitInitMode(_) => true
      case _ => false
    } == scenarioFromGeneratedSource.initialization.filterNot {
      case EnterInitMode(_) => true
      case ExitInitMode(_) => true
      case _ => false
    })
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

  "ScenarioConfGenerator" should "create valid Master Algorithm for Algebraic Initialization" in {
    confGenerationTest("examples/algebraic_loop_initialization.conf")
  }
  "ScenarioConfGenerator" should "create valid Master Algorithm for Industrial case study" in {
    confGenerationTest("examples/industrial_casestudy.conf")
  }

  "ScenarioConfGenerator" should "create valid Step procedure for Two Algebraic Loops" in {
    confGenerationTest("examples/two_algebraic_loops.conf")
  }

  "ScenarioConfGenerator" should "create valid Procedure for loop within loop" in {
    confGenerationTest("examples/loop_within_loop.conf")
  }

  "ScenarioConfGenerator" should "create valid Procedure for Spanish Example" in {
    confGenerationTest("examples/tapas.conf")
  }

  "ScenarioConfGenerator" should "create valid Procedure for step finding loop adaptive" in {
    confGenerationTest("examples_no_algorithm/step_finding_loop_adaptive.conf")
  }
}