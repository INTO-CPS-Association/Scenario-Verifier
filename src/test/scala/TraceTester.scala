import core.{ModelEncoding, ScenarioLoader}
import org.scalatest.flatspec._
import org.scalatest.matchers._
import trace_analyzer.TraceAnalyzer

class TraceTester extends AnyFlatSpec with should.Matchers {


  "TraceTester" should "work" in {
    val conf = getClass.getResourceAsStream("examples/simple_master.conf")
    val encoding = new ModelEncoding(ScenarioLoader.load(conf))

    val lines = scala.io.Source.fromResource("trace/trace.log").getLines
      .filterNot(i => i.matches("\\([^()]*\\)"))
        .filterNot(i => i.matches(".*\\s\\{(.*?)\\}"))
    TraceAnalyzer.AnalyseScenario("simple_master", lines, encoding)
  }

  "TraceTester" should "work long" in {
    val conf = getClass.getResourceAsStream("examples/industrial_casestudy.conf")
    val encoding = new ModelEncoding(ScenarioLoader.load(conf))

    val lines = scala.io.Source.fromResource("trace/trace_long.log").getLines
      .filterNot(i => i.matches("\\([^()]*\\)"))
      .filterNot(i => i.matches(".*\\s\\{(.*?)\\}"))
    TraceAnalyzer.AnalyseScenario("industrial", lines, encoding)
  }


}
