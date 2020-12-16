import core.{ConnectionParserSingleton, PortRef, ScenarioLoader}
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer.{DoStepNode, Edge, GetNode, GraphBuilder, Node, SetNode}

class GraphBuilderTest extends AnyFlatSpec with should.Matchers {

  "GraphBuilder" should "should build an initial graph with connection and feedthrough edges" in {
    val conf = getClass.getResourceAsStream("examples/simple_master.conf")
    val scenario = ScenarioLoader.load(conf)

    val graph = new GraphBuilder(scenario.scenario)
    val initialEdges = graph.initialEdges

    //There is an edge for all connections in the scenario
    assert(scenario.scenario.connections.forall(c => initialEdges.contains(Edge[Node](GetNode(c.srcPort), SetNode(c.trgPort)))))

    //There is an edge for all initial feedthrough in the scenario
    scenario.scenario.fmus.foreach(fmu => {
      fmu._2.outputs.foreach(o => {
        assert(o._2.dependenciesInit.forall(i => initialEdges.contains(Edge[Node](SetNode(PortRef(fmu._1, i)), GetNode(PortRef(fmu._1, o._1))))))
      })
    })

    assert(initialEdges.size == 5)
    val nodes = (initialEdges.map(o => o.srcNode) ++ initialEdges.map(o => o.trgNode))
    assert(nodes.size == 6)

    //All nodes are either get or set
    assert(nodes.forall(n => n match {
      case GetNode(_) => true
      case SetNode(_) => true
      case _ => false
    }))
  }

  "GraphBuilder" should "should build a step graph with connections, feedthrough and doStep edges" in {
    val conf = getClass.getResourceAsStream("examples/simple_master.conf")
    val scenario = ScenarioLoader.load(conf)

    val graph = new GraphBuilder(scenario.scenario)
    val stepEdges = graph.stepEdges

    //There is an edge for all connections in the scenario
    assert(scenario.scenario.connections.forall(c => stepEdges.contains(Edge[Node](GetNode(c.srcPort), SetNode(c.trgPort)))))

    //There is an edge for all initial feedthrough in the scenario
    scenario.scenario.fmus.foreach(fmu => {
      fmu._2.outputs.foreach(o => {
        assert(o._2.dependencies.forall(i => stepEdges.contains(Edge[Node](SetNode(PortRef(fmu._1, i)), GetNode(PortRef(fmu._1, o._1))))))
      })
    })

    //There is an edge from all doStep to Get in the scenario
    scenario.scenario.fmus.foreach(fmu => {
      fmu._2.outputs.foreach(o => {
        assert(o._2.dependencies.forall(i => stepEdges.contains(Edge[Node](SetNode(PortRef(fmu._1, i)), GetNode(PortRef(fmu._1, o._1))))))
      })
    })

    val nodes = stepEdges.map(o => o.srcNode) ++ stepEdges.map(o => o.trgNode)
    assert(stepEdges.size == 11)

    assert(nodes.count(i => i match {
      case DoStepNode(_, _) => true
      case _ => false
    }) == 2)

    assert(nodes.size == scenario.cosimStep.size)
    assert(nodes.forall(n => n match {
      case GetNode(_) => true
      case SetNode(_) => true
      case DoStepNode(_,_) => true
      case _ => false
    }))
  }


}
