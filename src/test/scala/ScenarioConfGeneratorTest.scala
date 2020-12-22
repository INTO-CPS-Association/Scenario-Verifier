import java.io.{ByteArrayInputStream, File, PrintWriter}

import cli.VerifyTA
import core.{ConnectionModel, FmuModel, MasterModel, ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer.ConfParser.ScenarioConfGenerator
import synthesizer.LoopStrategy.{LoopStrategy, maximum, minimum}
import synthesizer._

class ScenarioConfGeneratorTest extends AnyFlatSpec with should.Matchers {
  def GenerateConf(resourcesFile: String) = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val scenario = ScenarioLoader.load(conf)
    val generateScenario = ScenarioConfGenerator.generate(scenario, scenario.name)
    val scenarioFromGeneratedSource = ScenarioLoader.load(new ByteArrayInputStream(generateScenario.getBytes()))
    //All connections are equivalent
    assert(compareConnections(scenario.scenario.connections, scenarioFromGeneratedSource.scenario.connections))
    //All FMUs are equivalent
    assert(compareFMUs(scenario.scenario.fmus, scenarioFromGeneratedSource.scenario.fmus))
  }

  private def compareConnections(c1: List[ConnectionModel], c2: List[ConnectionModel]): Boolean = {
    c1.forall(i => c2.contains(i))
  }

  private def compareFMUs(FMUsOriginal: Map[String, FmuModel], FMUsGenerated: Map[String, FmuModel]): Boolean = {
    FMUsOriginal.forall(fmu => FMUsGenerated.exists(i => i == fmu))
  }

  "ScenarioConfGenerator" should "create valid Master Algorithm for Simple Master" in {
    GenerateConf("examples/simple_master.conf")
  }

  "ScenarioConfGenerator" should "create valid Master Algorithm for Algebraic Initialization" in{
    GenerateConf("examples/algebraic_loop_initialization.conf")
  }
  "ScenarioConfGenerator" should "create valid Master Algorithm for Industrial case study" in{
    GenerateConf("examples/industrial_casestudy.conf")
  }

  "ScenarioConfGenerator" should "create valid Step procedure for Two Algebraic Loops" in {
    GenerateConf("examples/two_algebraic_loops.conf")
  }



}