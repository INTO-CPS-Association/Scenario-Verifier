package synthesizer

import java.io.File

import guru.nidi.graphviz.attribute.{Color, Label, Style}
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.Factory.{graph, mutGraph, mutNode}
import guru.nidi.graphviz.model.MutableNode

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

  /*
    def findNodesByFMU(nodes: Set[MutableNode], fmu: String) = {
      nodes.map(i => i.name().toString.split("_"))
    }
  */
  //Todo update graph with clusters

  def plotGraph(name: String, edges: Set[Edge[Node]], SCCs: List[List[Node]]): Unit = {
    val nonTrivialSCCs = SCCs.filter(_.size > 1).flatten
    val g = mutGraph(name).setDirected(true)
    val nodes = (edges.map(_.trgNode) ++ edges.map(_.srcNode)).map(n => mutNode(getName(n, SCCs)).add(if (nonTrivialSCCs.contains(n)) Color.BLUE else Color.BLACK))

    /*
        FMUs.foreach(fmu => {
          val cluster = graph.named(fmu).cluster().graphAttr().`with`(Style.FILLED, Color.LIGHTGREY,
            Label.lines(fmu.toUpperCase(),
            )).toMutable

          //cluster.add(findNodesByFMU(nodes, fmu))

        })

     */

    edges.foreach(o => {
      val s = nodes.find(n => n.name().toString == getName(o.srcNode, SCCs)).get
      val t = nodes.find(n => n.name().toString == getName(o.trgNode, SCCs)).get
      g.add(s.addLink(t))
    })
    Graphviz.fromGraph(g).render(Format.SVG).toFile(new File(String.format("example/%s.svg", name)))
  }

}
