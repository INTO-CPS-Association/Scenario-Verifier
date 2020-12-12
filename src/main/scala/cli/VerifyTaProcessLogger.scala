package cli

import scala.sys.process.ProcessLogger

class VerifyTaProcessLogger extends ProcessLogger {

  val output: StringBuffer = new StringBuffer(80*15)

  override def out(s: => String): Unit = output.append(s)

  override def err(s: => String): Unit = output.append(s)

  override def buffer[T](f: => T): T = f
}
