package org.intocps.verification.scenarioverifier.prettyprint

import org.apache.logging.log4j.scala.Logger

// Taken from https://stackoverflow.com/questions/15718506/scala-how-to-print-case-classes-like-pretty-printed-tree
object PPrint {
  def pprint(obj: Any, depth: Int = 0, paramName: Option[String] = None, l: Logger): Unit = {
    val indent = "  " * depth
    val prettyName = paramName.fold("")(x => s"$x: ")
    val ptype = obj match {
      case _: Iterable[Any] => ""
      case obj: Product => obj.productPrefix
      case _ => obj.toString
    }

    l.debug(s"$indent$prettyName$ptype")

    obj match {
      case seq: Iterable[Any] =>
        seq.foreach(i => pprint(i, depth + 1, l = l))
      case obj: Product =>
        (obj.productIterator zip obj.productElementNames)
          .foreach { case (subObj, paramName) => pprint(subObj, depth + 1, Some(paramName), l) }
      case _ =>
    }
  }
}
