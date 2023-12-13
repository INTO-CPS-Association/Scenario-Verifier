package org.intocps.verification.scenarioverifier.cli

import java.io._

import scala.sys.process.ProcessLogger

class VerifyTaProcessLogger extends ProcessLogger {

  val output: StringBuffer = new StringBuffer(80 * 15)

  override def out(s: => String): Unit = output.append(s)

  override def err(s: => String): Unit = output.append(s)

  override def buffer[T](f: => T): T = f
}

class FileProcessLogger(file: File) extends ProcessLogger with Closeable with Flushable {
  private val writer =
    new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true))))
  def out(s: => String): Unit = writer.println(s)
  def err(s: => String): Unit = writer.println(s)
  def buffer[T](f: => T): T = f
  def close(): Unit = writer.close()
  def flush(): Unit = writer.flush()
}
