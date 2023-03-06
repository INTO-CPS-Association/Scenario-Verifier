package synthesizer


object GraphUtil {
  private def pathExist[A](src: A, trg: A, edges: Set[Edge[A]]): Boolean = {
    var adjacencyList = edges.map(o => (o.srcNode, o.trgNode)).groupMap(_._1)(_._2).map(i => (i._1, i._2.to(LazyList)))
    val nodes = edges.map(_.srcNode) ++ edges.map(_.trgNode)
    adjacencyList ++= nodes.diff(adjacencyList.keys.toSet).map(i => (i, LazyList.empty[A]))
    bfs(adjacencyList, LazyList(src), LazyList.empty, LazyList.empty).contains(trg)
  }

  @annotation.tailrec
  private final def bfs[A](graph: Map[A, LazyList[A]], toVisit: LazyList[A], visited: LazyList[A], accumulator: LazyList[A]): LazyList[A] = {
    if (toVisit.isEmpty) {
      accumulator
    } else {
      val next = toVisit.head
      val succ = graph(next) diff visited diff toVisit
      bfs(graph, toVisit.tail ++ succ, visited :+ next, accumulator :+ next)
    }
  }

  def removeTransitiveEdges[A](edges: Set[Edge[A]], nonTrivialSCC: Set[A]): Set[Edge[A]] = {
    var removedEdges = Set.empty[Edge[A]]
    val nodes = edges.map(o => (o.srcNode, o.trgNode)).groupMap(_._2)(_._1).filter(i => i._2.size > 1).map(i => (i._1, i._2.diff(nonTrivialSCC)))
    nodes.foreach(n => {
      n._2.foreach(v =>
        n._2.filter(_ != v).foreach(i => {
          if (pathExist(i, v, edges -- removedEdges) && !removedEdges.contains(Edge(i, n._1)))
            removedEdges += Edge(i, n._1)
        })
      )
    })
    edges.diff(removedEdges)
  }
}
