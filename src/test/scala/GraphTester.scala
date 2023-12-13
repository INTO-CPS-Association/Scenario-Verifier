import org.intocps.verification.scenarioverifier.synthesizer.Edge
import org.intocps.verification.scenarioverifier.synthesizer.TarjanGraph
import org.scalatest.flatspec._
import org.scalatest.matchers._

class GraphTester extends AnyFlatSpec with should.Matchers {
  it should "Find 2 connected components" in {
    val edges = Set[Edge[Int]](Edge[Int](1, 2), Edge[Int](2, 3), Edge[Int](4, 5))
    val tarjan = new TarjanGraph[Int](edges)
    assert(!tarjan.hasCycle)

    assert(tarjan.connectedComponents.size == 2)
    assert(tarjan.connectedComponents.forall(i => i.size == 3 || i.size == 2))
  }

  it should "Find 1 connected components" in {
    val edges = Set[Edge[Int]](Edge[Int](1, 2), Edge[Int](2, 3), Edge[Int](3, 4), Edge[Int](4, 5))
    val tarjan = new TarjanGraph[Int](edges)
    assert(!tarjan.hasCycle)

    // Only one connected component
    assert(tarjan.connectedComponents.size == 1)
    // Should contain 5 nodes
    assert(tarjan.connectedComponents.head.size == 5)
  }

  it should "Find 1 Advanced Connected components" in {
    val edges = Set[Edge[Int]](Edge[Int](1, 2), Edge[Int](2, 3), Edge[Int](3, 4), Edge[Int](5, 4))
    val tarjan = new TarjanGraph[Int](edges)
    assert(!tarjan.hasCycle)
    assert(tarjan.connectedComponents.size == 1)
    assert(tarjan.connectedComponents.head.size == 5)
  }

  it should "Find 3 Advanced Connected components" in {
    val edges = Set[Edge[Int]](Edge[Int](1, 2), Edge[Int](2, 3), Edge[Int](5, 6), Edge[Int](10, 4))
    val tarjan = new TarjanGraph[Int](edges)
    assert(!tarjan.hasCycle)
    assert(tarjan.connectedComponents.size == 3)
    assert(tarjan.connectedComponents.forall(i => i.size == 3 || i.size == 2))
  }

  it should "find order of SCC and Connected Components" in {
    val edges = Set[Edge[Int]](Edge[Int](1, 2), Edge[Int](2, 3), Edge[Int](5, 6), Edge[Int](10, 4))
    val tarjan = new TarjanGraph[Int](edges)
    assert(!tarjan.hasCycle)
    assert(tarjan.topologicalconnectedSCC.size == 3)
    assert(tarjan.topologicalconnectedSCC.head.flatten == List(1, 2, 3))

  }

  it should "find order of SCC and Connected Components Test" in {
    val edges = Set[Edge[Int]](Edge[Int](1, 2), Edge[Int](2, 3), Edge[Int](3, 2), Edge[Int](5, 6), Edge[Int](10, 4))
    val tarjan = new TarjanGraph[Int](edges)
    assert(tarjan.hasCycle)
    assert(tarjan.topologicalconnectedSCC.size == 3)
    assert(tarjan.topologicalconnectedSCC.flatten.map(o => o.toSet).contains(Set(2, 3)))
  }

}
