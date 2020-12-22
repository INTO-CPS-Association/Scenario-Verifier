package synthesizer.MST

import synthesizer.EdgeCost

import scala.collection.mutable

object MST {
  import collection.mutable.ListBuffer

  def mst(edges: Set[EdgeCost]): Set[EdgeCost] = {
    val nodes = edges.map(_.srcNode) ++ edges.map(_.trgNode)
    val X = mutable.Set(nodes.head)
    val V = mutable.Set() ++ nodes.tail
    val T = ListBuffer[EdgeCost]()
    while (X.size < nodes.size) {
      val edge = edges
        .filter({ case EdgeCost(in, out, _) => X.contains(in) && V.contains(out) || X.contains(out) && V.contains(in) })
        .toList.minBy(e => e.cost)
      X += edge.trgNode += edge.srcNode
      V -= edge.trgNode -= edge.srcNode
      T += edge
    }
    T.toSet
  }

}