package synthesizer

import java.io.File

import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.Factory.{mutGraph, mutNode}

object GraphVisualizer {
  def getName(node: Node, SCCs: List[List[Node]]): String = {
    val scc = SCCs.find(o => o.contains(node)).get
    val index = SCCs.indexOf(scc)
    node match {
      case DoStepNode(name) => f"${index}:DoStep_${name}"
      case GetNode(port) => f"${index}:Get_${port.fmu}_${port.port}"
      case SetOptimizedNode(ports) => f"${index}:Set_${ports.head.fmu}_[${ports.map(_.port).mkString(", ")}]}"
      case GetOptimizedNode(ports) => f"${index}:Get_${ports.head.fmu}_[${ports.map(_.port).mkString(", ")}]}"
      case SetNode(port) => f"${index}:Set_${port.fmu}_${port.port}"
      case RestoreNode(name) => f"${index}:Restore_${name}"
      case SaveNode(name) => f"${index}:Save_${name}"
    }
  }

  def plotGraph(name: String, edges: Set[Edge[Node]]): Unit = {
    val SCCs = getSCCs(edges)
    val nonTrivialSCCs = SCCs.filter(_.size > 1).flatten

    val g = mutGraph(name).setDirected(true)
    val nodes = (edges.map(_.trgNode) ++ edges.map(_.srcNode)).map(n => mutNode(getName(n, SCCs)).add(if (nonTrivialSCCs.contains(n)) Color.BLUE else Color.BLACK))

    edges.foreach(o => {
      val s = nodes.find(n => n.name().toString == getName(o.srcNode, SCCs)).get
      val t = nodes.find(n => n.name().toString == getName(o.trgNode, SCCs)).get
      g.add(s.addLink(t))
    })
    Graphviz.fromGraph(g).render(Format.SVG).toFile(new File(String.format("example/%s.svg", name)))
  }

  private def getSCCs(edges: Set[Edge[Node]]): List[List[Node]] = {
    val tarjan = new TarjanGraph[Node](edges)
    tarjan.topologicalSCC
  }
}
