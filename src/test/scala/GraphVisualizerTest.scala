import org.apache.commons.io.FileUtils
import org.intocps.verification.scenarioverifier.core.ScenarioLoaderFMI2
import org.intocps.verification.scenarioverifier.synthesizer._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class GraphVisualizerTest extends AnyFlatSpec with should.Matchers {
  def testGraph(file: String, name: String): Unit = {
    println("Testing " + file)
    val conf = getClass.getResourceAsStream(file)
    val masterModel = ScenarioLoaderFMI2.load(conf)
    val scenario = masterModel.scenario
    val graph = new GraphBuilder(scenario, true)
    val tarjanInit = new TarjanGraph[InitializationInstructionNode](graph.initialEdges)
    val tarjanStep = new TarjanGraph[StepInstructionNode](graph.stepEdges)
    val initialGraph = GraphVisualizer.plotGraph(name + "_initial", graph.initialEdges, tarjanInit.topologicalSCC)
    assert(initialGraph.exists(), "Initial graph was not created")
    FileUtils.deleteQuietly(initialGraph)
    val stepGraph = GraphVisualizer.plotGraph(name + "_step", graph.stepEdges, tarjanStep.topologicalSCC)
    assert(stepGraph.exists(), "Step graph was not created")
    FileUtils.deleteQuietly(stepGraph)
  }

  it should "should build graphs for Simple Master" in {
    testGraph("examples/simple_master.conf", "simple_master_graph")
  }

  it should "should build graphs for Industrial case" in {
    testGraph("examples/industrial_casestudy.conf", "industrial_casestudy_graph")
  }

  it should "should build graphs for Algebraic Loop Init" in {
    testGraph("examples/algebraic_loop_initialization.conf", "algebraic_loop_initialization_graph")
  }

  it should "should build graphs for Algebraic Loop Step" in {
    testGraph("examples/algebraic_loop_msd_gs.conf", "algebraic_loop_step_graph")
  }

  it should "should build graphs for Step Finding Loop" in {
    testGraph("examples/step_finding_loop_msd_1.conf", "step_finding_loop")
  }

  it should "should build graphs for Two Algebraic Loops" in {
    testGraph("examples/two_algebraic_loops.conf", "two_algebraic_loop")
  }

  it should "should build graphs for Loop within Loop" in {
    testGraph("examples/loop_within_loop.conf", "loop_within_loop")
  }

  it should "should build graphs for Step Finding Loop Delayed Ports" in {
    testGraph("examples_no_algorithm/step_finding_loop_two_delayed.conf", "step_finding_delayed")
  }

  it should "should build graphs for Step Finding Loop Three SUs" in {
    testGraph("examples_no_algorithm/step_finding_loop_three_delayed.conf", "step_finding_three_delayed")
  }

  it should "should build graphs for Simple Master v. 2" in {
    testGraph("examples_no_algorithm/simple_master_2.conf", "simple_master_2")
  }
}
