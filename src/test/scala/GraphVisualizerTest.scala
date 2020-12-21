import core.{ConnectionParserSingleton, MasterModel, PortRef, ScenarioLoader, ScenarioModel}
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer.{DoStepNode, Edge, GetNode, GraphBuilder, GraphVisualizer, Node, SetNode}

class GraphVisualizerTest extends AnyFlatSpec with should.Matchers {
  def testGraph(file: String, name: String): Unit = {
    val conf = getClass.getResourceAsStream(file)
    val masterModel = ScenarioLoader.load(conf)
    val scenario = masterModel.scenario
    val graph = new GraphBuilder(scenario)
    GraphVisualizer.plotGraph(name + "_initial", graph.initialEdges)
    GraphVisualizer.plotGraph(name+ "_step", graph.stepEdges)
    GraphVisualizer.plotGraph(name + "_initial_opt", graph.initialEdgesOptimized)
    GraphVisualizer.plotGraph(name+ "_step_optimized", graph.stepEdgesOptimized)
  }

  "GraphVisualizer" should "should build graphs for Simple Master" in {
    testGraph("examples/simple_master.conf", "simple_master_graph")
  }

  "GraphVisualizer" should "should build graphs for Industrial case study" in{
    testGraph("examples/industrial_casestudy.conf", "industrial_casestudy_graph")
  }

  "GraphVisualizer" should "should build graphs for Algebraic Loop Init" in{
    testGraph("examples/algebraic_loop_initialization.conf", "algebraic_loop_initialization_graph")
  }

  "GraphVisualizer" should "should build graphs for Algebraic Loop Step" in{
    testGraph("examples/algebraic_loop_msd_gs.conf", "algebraic_loop_step_graph")
  }
}
