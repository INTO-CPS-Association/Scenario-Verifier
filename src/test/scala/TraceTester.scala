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
    new PrintWriter(file) { write(content); close() }
    file
  }


  def generateTrace(encoding: ModelEncoding, traceFile: File) ={
    val result = ScenarioGenerator.generate(encoding)
    val f = writeToTempFile(result)
    assert(VerifyTA.checkEnvironment())
    VerifyTA.saveTraceToFile(f, traceFile)
    FileUtils.deleteQuietly(f)
  }

  "TraceTester" should "work" in {
    val conf = getClass.getResourceAsStream("examples/simple_master.conf")
    val encoding = new ModelEncoding(ScenarioLoader.load(conf))

    val traceFile = new File("trace_simple_master.log")
    generateTrace(encoding, traceFile)

    val lines = scala.io.Source.fromFile(traceFile).getLines
    TraceAnalyzer.AnalyseScenario("simple_master", lines, encoding)
  }

  /*
  "TraceTester" should "work long" in {
    val conf = getClass.getResourceAsStream("examples/industrial_casestudy.conf")
    val encoding = new ModelEncoding(ScenarioLoader.load(conf))

    val lines = scala.io.Source.fromResource("trace/trace_long.log").getLines
    TraceAnalyzer.AnalyseScenario("industrial", lines, encoding)
  }*/




}
