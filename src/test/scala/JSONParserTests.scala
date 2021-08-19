import core.{ConnectionParserSingleton, ScenarioLoader}
import org.scalatest.flatspec._
import org.scalatest.matchers._

class JSONParserTests extends AnyFlatSpec with should.Matchers {

  /*
  "JSONScenarioParser" should "load simple master configuration" in {
    val conf = getClass.getResourceAsStream("examples/simple_master.json")
    val results = ScenarioLoader.loadJson(conf)
    //Simple Test
    assert(results.initialization.size == 10)
    assert(results.cosimStep.values.head.size == 8)
    assert(results.scenario.connections.size == 3)
    assert(results.scenario.fmus.size == 2)
    print(results)
  }

   */

  "json.ConnectionParser" should "parse connections" in {

  }

}
