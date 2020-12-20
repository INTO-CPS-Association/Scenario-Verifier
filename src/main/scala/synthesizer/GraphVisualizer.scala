package synthesizer

import java.io.File
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.Factory.{mutGraph, mutNode}

object GraphVisualizer {
  def getName(node: Node): String = node match {
    case DoStepNode(name) => f"DoStep_${name}"
    case GetNode(port) =>f"Get_${port.fmu}_${port.port}"
    case SetOptimizedNode(ports) =>f"Set_${ports.head.fmu}_[${ports.mkString(",")}]}"
    case GetOptimizedNode(ports) =>f"Get_${ports.head.fmu}_[${ports.mkString(",")}]}"
    case SetNode(port) =>f"Set_${port.fmu}_${port.port}"
    case RestoreNode(name) => f"Restore_${name}"
    case SaveNode(name) => f"Save_${name}"
  }

  def plotGraph(name: String, edges: Set[Edge[Node]]): Unit = {
    val g = mutGraph(name).setDirected(true)
    val nodes = edges.map(e => mutNode(getName(e.srcNode)).add(Color.BLACK)).toSet ++ edges.map(e => mutNode(getName(e.trgNode)).add(Color.BLACK)).toSet
    edges.foreach(o => {
      val s = nodes.find(n => n.name().toString == getName(o.srcNode)).get
      val t = nodes.find(n => n.name().toString == getName(o.trgNode)).get
      g.add(s.addLink(t))
    })
    Graphviz.fromGraph(g).height(500).render(Format.PNG).toFile(new File(String.format("example/%s.png", name)))
  }
}
