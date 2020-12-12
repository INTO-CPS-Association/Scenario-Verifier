import core.{ConnectionParserSingleton, ScenarioLoader}
import org.scalatest.flatspec._
import org.scalatest.matchers._

class ParserTests extends AnyFlatSpec with should.Matchers {

  "ScenarioParser" should "load simple master configuration" in {
    val conf = getClass.getResourceAsStream("examples/simple_master.conf")
    val results = ScenarioLoader.load(conf)
    print(results)
  }

  "core.ConnectionParser" should "parse connections" in {
    val results = ConnectionParserSingleton.parse(ConnectionParserSingleton.connection, "fmu1.port1 -> fmu2.port2")
    results.successful should equal (true)
    val connection = results.get
    connection.srcPort.port should equal ("port1")
    connection.trgPort.port should equal ("port2")
  }

}
