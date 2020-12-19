import java.io.PrintWriter
import java.nio.file.Files

import cli.VerifyTA
import core.{ModelEncoding, ScenarioGenerator, ScenarioLoader}
import org.apache.commons.io.FileUtils
import org.scalatest.flatspec._
import org.scalatest.matchers._
import synthesizer.{Edge, TarjanGraph}

class GraphTester extends AnyFlatSpec with should.Matchers {
  it should "Find 2 connected components" in {
    val edges = Set[Edge[Int]](Edge[Int](1, 2),Edge[Int](2, 3), Edge[Int](4, 5))
    val tarjan = new TarjanGraph[Int](edges)
    assert(!tarjan.hasCycle)

    assert(tarjan.connectedComponents.size == 2)
    assert(tarjan.connectedComponents.values.forall(i => i.size == 3 || i.size == 2))
  }

  it should "Find 1 connected components" in {
    val edges = Set[Edge[Int]](Edge[Int](1, 2),Edge[Int](2, 3), Edge[Int](3, 4), Edge[Int](4, 5))
    val tarjan = new TarjanGraph[Int](edges)
    assert(!tarjan.hasCycle)

    //Only one connected component
    assert(tarjan.connectedComponents.size == 1)
    //Should contain 5 nodes
    assert(tarjan.connectedComponents.head._2.size == 5)
  }

  it should "Find 1 Advanced Connected components" in {
    val edges = Set[Edge[Int]](Edge[Int](1, 2),Edge[Int](2, 3), Edge[Int](3, 4), Edge[Int](5, 4))
    val tarjan = new TarjanGraph[Int](edges)
    assert(!tarjan.hasCycle)
    assert(tarjan.connectedComponents.size == 1)
    assert(tarjan.connectedComponents.head._2.size == 5)
  }

  it should "Find 3 Advanced Connected components" in {
    val edges = Set[Edge[Int]](Edge[Int](1, 2),Edge[Int](2, 3), Edge[Int](5, 6), Edge[Int](10, 4))
    val tarjan = new TarjanGraph[Int](edges)
    assert(!tarjan.hasCycle)
    assert(tarjan.connectedComponents.size == 3)
    assert(tarjan.connectedComponents(0).size == 3)
    assert(tarjan.connectedComponents(1).size == 2)
    assert(tarjan.connectedComponents(2).size == 2)
  }

}
