import java.io.{File, PrintWriter}
import java.nio.file.Files

import cli.VerifyTA
import core.{MasterModel, ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer.LoopStrategy.{LoopStrategy, maximum, minimum}
import synthesizer._

import scala.collection.immutable.HashSet

class ScenarioBuilderTest extends AnyFlatSpec with should.Matchers {
  def writeToTempFile(content: String) = {
    val file = Files.createTempFile("uppaal_", ".xml").toFile
    new PrintWriter(file) { write(content); close() }
    file
  }

  def generateSynthesisAndVerify(scenarioName: String, nFMU: Int, nConnection: Int, feedthrough: Boolean = true, strategy: LoopStrategy = maximum) = {
    val scenario = ScenarioBuilder.generateScenario(nFMU, nConnection, feedthrough)
    val synthesizer = new Synthesizer(scenario, strategy)
    val step = synthesizer.synthesizeStep()
    val init =  ScenarioLoader.generateEnterInitInstructions(scenario) ++ synthesizer.synthesizeInitialization() ++ ScenarioLoader.generateExitInitInstructions(scenario)
    val model = MasterModel(scenarioName, scenario,
      instantiation = ScenarioLoader.generateInstantiationInstructions(scenario).toList,
      initialization = init.toList,
      cosimStep = step,
      terminate = ScenarioLoader.generateTerminateInstructions(scenario).toList)
    val encoding = new ModelEncoding(model)
    val result = ScenarioGenerator.generate(encoding)
    val f = writeToTempFile(result)
    assert(VerifyTA.checkEnvironment())
    VerifyTA.verify(f) should be (0)
    FileUtils.deleteQuietly(f)
  }


  "ScenarioBuilderTest" should "create valid Simple example" in{
    generateSynthesisAndVerify("Test Simple Example", 2, 2)
  }

  "ScenarioBuilderTest" should "create valid Bigger example" in{
    generateSynthesisAndVerify("Test Big Example", 10, 20)
  }

  "ScenarioBuilderTest" should "create valid Big example with no Feedthrough" in{
    generateSynthesisAndVerify("Test Big Example", 50, 100, false)
  }

  "ScenarioBuilderTest" should "create valid Connection example" in{
    val fmuNames = HashSet("fmu1", "fmu2")
    val connections = ScenarioBuilder.generateConnections(fmuNames, 2)
    assert(connections.exists(o => o.srcPort.fmu == fmuNames.toVector(0)) &&  connections.exists(o => o.srcPort.fmu == fmuNames.toVector(1)))
    assert(connections.exists(o => o.trgPort.fmu == fmuNames.toVector(0)) &&  connections.exists(o => o.trgPort.fmu == fmuNames.toVector(1)))

  }


}
