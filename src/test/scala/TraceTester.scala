import java.io.PrintWriter
import java.nio.file.Files

import cli.VerifyTA
import core.{ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.Ignore
import org.scalatest.flatspec._
import org.scalatest.matchers._
import trace_analyzer.TraceAnalyzer

//@Ignore
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
    val source = scala.io.Source.fromFile(traceFile)
    try {
      val lines = source.getLines()
      TraceAnalyzer.AnalyseScenario(scenarioName, lines, encoding)
    }
    finally source.close()
    FileUtils.deleteQuietly(traceFile)
  }


  "TraceTester" should "work for simple master" in {
    generateTrace("common_mistakes/simple_master_forget_connection.conf", "simple_master_forget_connection")
  }

    "TraceTester" should "work for simple" in {
    generateTrace("common_mistakes/simple_master_can_reject_step.conf", "simple_master_can_reject")
  }
    "TraceTester" should "work for industrial" in {
      generateTrace("common_mistakes/industrial_missing_step.conf", "industrial_missing_step")
    }

    "TraceTester" should "work for loop" in {
      generateTrace("common_mistakes/algebraic_loop_msd_gs_forget_restore.conf", "algebraic_loop_msd_gs_forget_restore")
    }
}
