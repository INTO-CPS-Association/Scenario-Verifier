import core.ScenarioLoader
import org.scalatest.Ignore
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer.{GraphBuilder, GraphVisualizer, Node, TarjanGraph}
@Ignore
class GraphVisualizerTest extends AnyFlatSpec with should.Matchers {
  def testGraph(file: String, name: String): Unit = {
    val conf = getClass.getResourceAsStream(file)
    val masterModel = ScenarioLoader.load(conf)
    val scenario = masterModel.scenario
    val graph = new GraphBuilder(scenario, true)
    val tarjanInit = new TarjanGraph[Node](graph.initialEdges)
    val tarjanStep = new TarjanGraph[Node](graph.stepEdges)
    val tarjanInitOpt = new TarjanGraph[Node](graph.initialEdgesOptimized)
    val tarjanStepOpt = new TarjanGraph[Node](graph.stepEdgesOptimized)
    GraphVisualizer.plotGraph(name + "_initial", graph.initialEdges, tarjanInit.topologicalSCC)
    GraphVisualizer.plotGraph(name+ "_step", graph.stepEdges, tarjanStep.topologicalSCC)
    GraphVisualizer.plotGraph(name + "_initial_opt", graph.initialEdgesOptimized, tarjanInitOpt.topologicalSCC)
    GraphVisualizer.plotGraph(name+ "_step_optimized", graph.stepEdgesOptimized, tarjanStepOpt.topologicalSCC)
  }

  "GraphVisualizer" should "should build graphs for Simple Master" in {
    testGraph("examples/simple_master.conf", "simple_master_graph")
  }

  "GraphVisualizer" should "should build graphs for Industrial case" in{
    testGraph("examples/industrial_casestudy.conf", "industrial_casestudy_graph")
  }

  "GraphVisualizer" should "should build graphs for Algebraic Loop Init" in{
    testGraph("examples/algebraic_loop_initialization.conf", "algebraic_loop_initialization_graph")
  }

  "GraphVisualizer" should "should build graphs for Algebraic Loop Step" in{
    testGraph("examples/algebraic_loop_msd_gs.conf", "algebraic_loop_step_graph")
  }

  "GraphVisualizer" should "should build graphs for Step Finding Loop" in{
    testGraph("examples/step_finding_loop_msd_1.conf", "step_finding_loop")
  }

  "GraphVisualizer" should "should build graphs for Two Algebraic Loops" in{
    testGraph("examples/two_algebraic_loops.conf", "two_algebraic_loop")
  }

  "GraphVisualizer" should "should build graphs for Loop within Loop" in{
    testGraph("examples/loop_within_loop.conf", "loop_within_loop")
  }

  "GraphVisualizer" should "should build graphs for Step Finding Loop Delayed Ports" in{
    testGraph("examples_no_algorithm/step_finding_loop_two_delayed.conf", "step_finding_delayed")
  }

  "GraphVisualizer" should "should build graphs for Step Finding Loop Three SUs" in{
    testGraph("examples_no_algorithm/step_finding_loop_three_delayed.conf", "step_finding_three_delayed")
  }

  "GraphVisualizer" should "should build graphs for Simple Master v. 2" in{
    testGraph("examples_no_algorithm/simple_master_2.conf", "simple_master_2")
  }
}
