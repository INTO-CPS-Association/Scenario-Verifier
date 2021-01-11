import core.{ConnectionParserSingleton, MasterModel, PortRef, ScenarioLoader, ScenarioModel}
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer.{DoStepNode, Edge, GetNode, GraphBuilder, Node, SetNode}

class GraphBuilderTest extends AnyFlatSpec with should.Matchers {
  def testInitialGraph(file: String): Unit ={
    val conf = getClass.getResourceAsStream(file)
    val masterModel = ScenarioLoader.load(conf)
    val scenario = masterModel.scenario
    val graph = new GraphBuilder(scenario)
    val initialEdges = graph.initialEdges

    //There is an edge for all connections in the scenario
    assert(scenario.connections.forall(c => initialEdges.contains(Edge[Node](GetNode(c.srcPort.fmu, c.srcPort), SetNode(c.trgPort.fmu, c.trgPort)))))

    //There is an edge for all initial feedthrough in the scenario
    scenario.fmus.foreach(fmu => {
      fmu._2.outputs.foreach(o => {
        assert(o._2.dependenciesInit.forall(i => initialEdges.contains(Edge[Node](SetNode(fmu._1, PortRef(fmu._1, i)), GetNode(fmu._1, PortRef(fmu._1, o._1))))))
      })
    })

    val nodes = (initialEdges.map(o => o.srcNode) ++ initialEdges.map(o => o.trgNode))
    assert(nodes.size == (masterModel.initialization.size - scenario.fmus.size * 2))

    //All nodes are either get or set
    assert(nodes.forall(n => n match {
      case GetNode(_, _) => true
      case SetNode(_, _) => true
      case _ => false
    }))
  }

  "GraphBuilder" should "should build an initial graph for Simple Master" in {
    testInitialGraph("examples/simple_master.conf")
  }

  "GraphBuilder" should "should build an initial graph for Industrial case study" in{
    testInitialGraph("examples/industrial_casestudy.conf")
  }

  "GraphBuilder" should "should build an initial graph for Two Algebraic Loops" in {
    testInitialGraph("examples/two_algebraic_loops.conf")
  }


  "GraphBuilder" should "should build a step graph with connections, feedthrough and doStep edges" in {
    val conf = getClass.getResourceAsStream("examples/simple_master.conf")
    val scenario = ScenarioLoader.load(conf)

    val graph = new GraphBuilder(scenario.scenario)
    val stepEdges = graph.stepEdges

    val nodes = stepEdges.map(o => o.srcNode) ++ stepEdges.map(o => o.trgNode)

    assert(nodes.count(i => i match {
      case DoStepNode(_) => true
      case _ => false
    }) == 2)

    assert(nodes.size == scenario.cosimStep.size)
    assert(nodes.forall(n => n match {
      case GetNode(_, _) => true
      case SetNode(_, _) => true
      case DoStepNode(_) => true
      case _ => false
    }))
  }


}
