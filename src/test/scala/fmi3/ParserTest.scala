package fmi3

import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier.core.ScenarioLoaderFMI3
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ParserTest extends AnyFlatSpec with should.Matchers with Logging {
  it should "load simple master configuration" in {
    val conf = getClass.getResourceAsStream("../examples_fmi_3/simple_master_fmi3.conf")
    val results = ScenarioLoaderFMI3.load(conf)
    // Simple Test
    assert(results.initialization.size == 10)
    assert(results.scenario.connections.size == 3)
    assert(results.scenario.fmus.size == 2)
  }

  it should "load scenario without clocks" in {
    val conf = getClass.getResourceAsStream("../examples_fmi_3/example.conf")
    val results = ScenarioLoaderFMI3.load(conf)
    // Simple Test
    assert(results.scenario.connections.size == 4)
    assert(results.scenario.fmus.size == 3)
  }

  it should "load configurations for powersystem with clock" in {
    val conf = getClass.getResourceAsStream("../examples_fmi_3/powersystem.conf")
    val results = ScenarioLoaderFMI3.load(conf)
    // Simple Test
    assert(results.scenario.fmus.size == 8)
    assert(results.scenario.connections.size == 71)
  }
}
