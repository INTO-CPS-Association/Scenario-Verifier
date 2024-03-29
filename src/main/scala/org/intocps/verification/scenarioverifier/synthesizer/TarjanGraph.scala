package org.intocps.verification.scenarioverifier.synthesizer

import scala.collection.mutable

class TarjanGraph[A](edges: Iterable[Edge[A]]) {
  lazy val tarjan: mutable.Buffer[mutable.Buffer[A]] = {
    var s = mutable.Buffer.empty[A] // Stack to keep track of nodes reachable from current node
    val index = mutable.Map.empty[A, Int] // index of each node
    val lowLink = mutable.Map.empty[A, Int] // The smallest index reachable from the node
    val ret = mutable.Buffer.empty[mutable.Buffer[A]] // Keep track of SCC in graph

    def visit(v: A): Unit = {
      // Set index and lowlink of node on first visit
      index(v) = index.size
      lowLink(v) = index(v)
      // Add to stack
      s += v

      edges
        .filter(_.srcNode == v)
        .map(_.trgNode)
        .foreach(w => {
          if (!index.contains(w)) {
            // Perform DFS from node W, if node w is not explored yet
            visit(w)
          }
          if (s.contains(w)) {
            // Node w is on the stack meaning - it means there is a path from w to v
            // and since node w is a neighbor to node v there is also a path from v to w
            lowLink(v) = math.min(lowLink(w), lowLink(v))
          }
        })

      // The lowlink value haven't been updated meaning it is the root of a cycle/SCC
      if (lowLink(v) == index(v)) {
        // Add the elements to the cycle that has been added to the stack and whose lowlink has been updated by node v's lowlink
        // This is the elements on the stack that is placed behind v
        val n = s.length - s.indexOf(v)
        ret += s.takeRight(n)
        // Remove these elements from the stack
        s = s.dropRight(n)
      }
    }

    // Perform a DFS from  all no nodes that hasn't been explored
    edges.foreach(v => if (!index.contains(v.srcNode)) visit(v.srcNode))
    ret
  }

  // A cycle exist if there is a SCC with at least two components
  lazy val hasCycle: Boolean = tarjan.exists(_.size >= 2)
  lazy val tarjanCycle: Seq[A] = tarjan.filter(_.size >= 2).distinct.flatten.toSeq

  lazy val topologicalSCC: List[List[A]] = {
    tarjan.map(_.toList).reverse.toList
  }

  lazy val topologicalEdges: Set[Edge[A]] =
    (0 until (topologicalSCC.size - 1))
      .flatMap(i => edges.filter(e => topologicalSCC(i).contains(e.srcNode) && topologicalSCC(i + 1).contains(e.trgNode)))
      .toSet

  lazy val topologicalconnectedSCC: Set[List[List[A]]] = {
    connectedComponents.zipWithIndex.map(c => topologicalSCC.filter(o => c._1.contains(o.head)))
  }

  lazy val connectedComponents: Set[List[A]] = {
    val visited = mutable.Set.empty[A]
    val connectedComponents = mutable.Map.empty[Int, List[A]]
    val undirectedEdges = edges.map(o => Edge(o.trgNode, o.srcNode)) ++ edges

    def DFSUtil(v: A, i: Int): List[A] = {
      // Add to stack
      visited += v
      var inConnection = List[A](v)
      undirectedEdges
        .filter(_.srcNode == v)
        .map(_.trgNode)
        .foreach(w => {
          if (!visited.contains(w)) {
            inConnection = inConnection.appendedAll(DFSUtil(w, i))
          }
        })
      inConnection
    }

    var i: Int = 0
    edges.foreach(v =>
      if (!visited.contains(v.srcNode)) {
        connectedComponents.addOne((i, DFSUtil(v.srcNode, i)))
        i += 1
      })

    connectedComponents.values.toSet
  }
}
