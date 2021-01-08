import java.io.{File, PrintWriter}
import java.nio.file.Files

import cli.VerifyTA
import cli.VerifyTA.VERIFY
import core.{ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.flatspec._
import org.scalatest.matchers._
import trace_analyzer.TraceAnalyzer

class TraceTester extends AnyFlatSpec with should.Matchers {
  def writeToTempFile(content: String) = {
    val file = Files.createTempFile("uppaal_", ".xml").toFile
    new PrintWriter(file) {
      write(content);
      close()
    }
    file
  }

  def generateTrace(scenarioPath: String, scenarioName: String) = {
    val conf = getClass.getResourceAsStream(scenarioPath)
    val encoding = new ModelEncoding(ScenarioLoader.load(conf))

    val result = ScenarioGenerator.generate(encoding)
    val f = writeToTempFile(result)
    val traceFile = Files.createTempFile("trace_", ".log").toFile

    assert(VerifyTA.checkEnvironment())
    VerifyTA.saveTraceToFile(f, traceFile)
    FileUtils.deleteQuietly(f)
    val lines = scala.io.Source.fromFile(traceFile).getLines
    TraceAnalyzer.AnalyseScenario(scenarioName, lines, encoding)
    FileUtils.deleteQuietly(traceFile)
  }

  "TraceTester" should "work" in {
    generateTrace("common_mistakes/simple_master_can_reject_step.conf", "simple_master_can_reject")
  }

  "TraceTester" should "work long" in {
    generateTrace("common_mistakes/industrial_missing_step.conf", "industrial_missing_step")
  }


}
