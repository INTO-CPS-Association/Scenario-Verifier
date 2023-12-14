package fmi3
import java.io.ByteArrayInputStream

import org.intocps.verification.scenarioverifier.api.GenerationAPI
import org.intocps.verification.scenarioverifier.core.ConnectionModel
import org.intocps.verification.scenarioverifier.core.EnterInitMode
import org.intocps.verification.scenarioverifier.core.ExitInitMode
import org.intocps.verification.scenarioverifier.core.FMI3.Fmu3Model
import org.intocps.verification.scenarioverifier.core.FMI3.ScenarioLoaderFMI3
import org.scalatest.flatspec._
import org.scalatest.matchers._
import org.scalatest.Assertion

class ConfGenerationTest extends AnyFlatSpec with should.Matchers {
  private def confGenerationTest(resourcesFile: String): Assertion = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val scenario = ScenarioLoaderFMI3.load(conf)
    val generatedConfiguration = scenario.toConf()
    val scenarioFromGeneratedSource = ScenarioLoaderFMI3.load(new ByteArrayInputStream(generatedConfiguration.getBytes()))
    // All connections are equivalent
    assert(compareConnections(scenario.scenario.connections, scenarioFromGeneratedSource.scenario.connections))
    // All FMUs are equivalent
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

  private def compareFMUs(FMUsOriginal: Map[String, Fmu3Model], FMUsGenerated: Map[String, Fmu3Model]): Boolean = {
    FMUsOriginal.forall(fmu => FMUsGenerated.exists(i => i == fmu))
  }

  it should "create conf for a simple scenario without clocks" in {
    confGenerationTest("../examples_fmi_3/example.conf")
  }

  it should "create conf for a simple scenario with clocks" in {
    confGenerationTest("../examples_fmi_3/motivation_example.conf")
  }
  it should "create  conf for an advanced scenario with clocks" in {
    confGenerationTest("../examples_fmi_3/powersystem.conf")
  }
}
