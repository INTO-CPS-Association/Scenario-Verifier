import org.intocps.verification.scenarioverifier.core.PortRef
import org.intocps.verification.scenarioverifier.core.ScenarioLoaderFMI2
import org.intocps.verification.scenarioverifier.synthesizer.DoStepNode
import org.intocps.verification.scenarioverifier.synthesizer.Edge
import org.intocps.verification.scenarioverifier.synthesizer.GetNode
import org.intocps.verification.scenarioverifier.synthesizer.GraphBuilder
import org.intocps.verification.scenarioverifier.synthesizer.InitializationInstructionNode
import org.intocps.verification.scenarioverifier.synthesizer.SetNode
import org.intocps.verification.scenarioverifier.synthesizer.SetTentativeNode
import org.intocps.verification.scenarioverifier.synthesizer.StepInstructionNode
import org.scalatest.flatspec._
import org.scalatest.matchers._

class GraphBuilderTest extends AnyFlatSpec with should.Matchers {
  def testInitialGraph(file: String): Unit = {
    val conf = getClass.getResourceAsStream(file)
    val masterModel = ScenarioLoaderFMI2.load(conf)
    val scenario = masterModel.scenario
    val graph = new GraphBuilder(scenario)
    val initialEdges = graph.initialEdges

    // There is an edge for all connections in the scenario
    assert(scenario.connections.size == initialEdges.count(o =>
      o.srcNode.isInstanceOf[GetNode] && (o.trgNode.isInstanceOf[SetNode] || o.trgNode.isInstanceOf[SetTentativeNode])))

    // There is an edge for all initial feedthrough in the scenario
    scenario.fmus.foreach(fmu => {
      fmu._2.outputs.foreach(o => {
        assert(
          o._2.dependenciesInit.forall(i =>
            initialEdges.contains(
              Edge[InitializationInstructionNode](SetNode(fmu._1, PortRef(fmu._1, i)), GetNode(fmu._1, PortRef(fmu._1, o._1)))) ||
              initialEdges.contains(
                Edge[InitializationInstructionNode](SetTentativeNode(fmu._1, PortRef(fmu._1, i)), GetNode(fmu._1, PortRef(fmu._1, o._1))))))
      })
    })

    val nodes = initialEdges.map(o => o.srcNode) ++ initialEdges.map(o => o.trgNode)
    assert(nodes.size == (masterModel.initialization.size - scenario.fmus.size * 2))

    // All nodes are either get or set
    assert(nodes.forall(_.isInstanceOf[InitializationInstructionNode]))
  }

  it should "should build an initial graph for Simple Master" in {
    testInitialGraph("examples/simple_master.conf")
  }

  it should "should build an initial graph for Industrial case study" in {
    testInitialGraph("examples/industrial_casestudy.conf")
  }

  it should "should build an initial graph for Two Algebraic Loops" in {
    testInitialGraph("examples/two_algebraic_loops.conf")
  }

  it should "should build a step graph with connections, feedthrough and doStep edges" in {
    val conf = getClass.getResourceAsStream("examples/simple_master.conf")
    val scenario = ScenarioLoaderFMI2.load(conf)

    val graph = new GraphBuilder(scenario.scenario)
    val stepEdges = graph.stepEdges

    val nodes = stepEdges.map(o => o.srcNode) ++ stepEdges.map(o => o.trgNode)
    assert(nodes.count(_.isInstanceOf[DoStepNode]) == 2)
    assert(nodes.size == scenario.cosimStep.values.head.size)
    assert(nodes.forall(_.isInstanceOf[StepInstructionNode]))
  }

}
