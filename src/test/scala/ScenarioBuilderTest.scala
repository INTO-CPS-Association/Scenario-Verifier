import java.io.{File, PrintWriter}
import java.nio.file.Files

import cli.VerifyTA
import core.{MasterModel, ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.Ignore
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer.LoopStrategy.{LoopStrategy, maximum, minimum}
import synthesizer._

import scala.collection.immutable.HashSet

class ScenarioBuilderTest extends AnyFlatSpec with should.Matchers {
  def writeToTempFile(content: String) = {
    val file = Files.createTempFile("uppaal_", ".xml").toFile
    new PrintWriter(file) {
      write(content);
      close()
    }
    file
  }

  def generateSynthesisAndVerify(scenarioName: String, nFMU: Int, nConnection: Int, feedthrough: Boolean = true, canRejectStep: Boolean = false, strategy: LoopStrategy = maximum) = {
    val scenario = ScenarioBuilder.generateScenario(nFMU, nConnection, feedthrough, canRejectStep)
    val synthesizer = new SynthesizerSimple(scenario, strategy)
    val step = synthesizer.synthesizeStep()

    val init = ScenarioLoader.generateEnterInitInstructions(scenario) ++ synthesizer.synthesizeInitialization() ++ ScenarioLoader.generateExitInitInstructions(scenario)

    val model = MasterModel(scenarioName, scenario,
      instantiation = ScenarioLoader.generateInstantiationInstructions(scenario).toList,
      initialization = init.toList,
      cosimStep = Map("cosim1" -> step),
      terminate = ScenarioLoader.generateTerminateInstructions(scenario).toList)
    val encoding = new ModelEncoding(model)

    /*
    val graph = new GraphBuilder(scenario, true)
    val tarjanStep = new TarjanGraph[Node](graph.stepEdges)
    GraphVisualizer.plotGraph(scenarioName, graph.stepEdges, tarjanStep.topologicalSCC)
     */

    val result = ScenarioGenerator.generate(encoding)
    val f = writeToTempFile(result)

    assert(VerifyTA.checkEnvironment())
    time {
      VerifyTA.verify(f) should be(0)
    }
    FileUtils.deleteQuietly(f)
  }

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) / 1000000 + "ms")
    result
  }

  "ScenarioBuilderTest" should "create valid Simple example" in {
    generateSynthesisAndVerify("Test_Simple_Example", 2, 2)
  }

  "ScenarioBuilderTest" should "create valid Bigger example" in {
    generateSynthesisAndVerify("Big_Advanced_Example", 10, 20)
  }

  "ScenarioBuilderTest" should "create valid Big and Advanced example" in {
    generateSynthesisAndVerify("Big_Very_Advanced_Example", 5, 10, true, true)
  }

  "ScenarioBuilderTest" should "create valid Big and Advanced with Feedthrough example" in {
    generateSynthesisAndVerify("Big_Very_Advanced_Example", 10, 20, true, true)
  }

  "ScenarioBuilderTest" should "create valid Big example with no Feedthrough" ignore  {
    generateSynthesisAndVerify("Test_Simple_Big_Example", 20, 40, false, true)
  }

  "ScenarioBuilderTest" should "create very big simple example" ignore  {
    generateSynthesisAndVerify("Big_Simple_Example", 100, 200, false, false)
  }

  "ScenarioBuilderTest" should "create valid Connection example" in {
    val fmuNames = HashSet("fmu1", "fmu2")
    val connections = ScenarioBuilder.generateConnections(fmuNames, 2)
    assert(connections.exists(o => o.srcPort.fmu == fmuNames.toVector(0)) && connections.exists(o => o.srcPort.fmu == fmuNames.toVector(1)))
    assert(connections.exists(o => o.trgPort.fmu == fmuNames.toVector(0)) && connections.exists(o => o.trgPort.fmu == fmuNames.toVector(1)))
  }
}
