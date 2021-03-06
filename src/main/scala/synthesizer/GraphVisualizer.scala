package synthesizer

import java.io.File

import guru.nidi.graphviz.attribute.{Color, Label, Style}
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.Factory.{graph, mutGraph, mutNode}
import guru.nidi.graphviz.model.MutableNode
import org.apache.logging.log4j.scala.Logging

object GraphVisualizer extends Logging{
  def getName(node: Node, SCCs: List[List[Node]]): String = {
    val scc = SCCs.find(_.contains(node)).get
    val index = SCCs.indexOf(scc)
    node match {
      case DoStepNode(name) => f"${index}:DoStep_${name}"
      case GetNode(_, port) => f"${index}:Get_${port.fmu}_${port.port}"
      case SetOptimizedNode(fmu, ports) => f"${index}:Set_${fmu}_[${ports.map(_.port).mkString(", ")}]}"
      case GetOptimizedNode(fmu, ports) => f"${index}:Get_${fmu}_[${ports.map(_.port).mkString(", ")}]}"
      case SetNode(_, port) => f"${index}:Set_${port.fmu}_${port.port}"
      case RestoreNode(name) => f"${index}:Restore_${name}"
      case SaveNode(name) => f"${index}:Save_${name}"
    }
  }

  def plotGraph(name: String, edges: Set[Edge[Node]], SCCs: List[List[Node]]): Unit = {
    val nonTrivialSCCs = SCCs.filter(_.size > 1).flatten
    val g = mutGraph(name).setDirected(true)
    val nodes = (edges.map(_.trgNode) ++ edges.map(_.srcNode)).groupBy(_.fmuName).filterNot(i => i._1 == "Empty")
    nodes.foreach(fmu => {
      val FMUName = fmu._1
      val cluster = graph.named(FMUName).cluster().graphAttr().`with`(Label.lines(FMUName.toUpperCase())).toMutable
      fmu._2.map(n => cluster.add(mutNode(getName(n, SCCs)).add(if (nonTrivialSCCs.contains(n)) Color.BLUE else Color.BLACK)))
      cluster.addTo(g)
    })

    edges.foreach(o => {
      val s = mutNode(getName(o.srcNode, SCCs))
      val t = mutNode(getName(o.trgNode, SCCs))
      g.add(s.addLink(t))
    })
    Graphviz.fromGraph(g).render(Format.SVG).toFile(new File(String.format("example/%s.svg", name)))
  }
}
