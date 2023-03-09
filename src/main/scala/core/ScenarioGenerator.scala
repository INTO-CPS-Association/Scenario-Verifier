package core

import org.apache.logging.log4j.scala.Logging

import java.io.{File, PrintWriter}
import scala.reflect.io.Directory

object ScenarioGenerator extends Logging {
  private def writeToFile(content: String, directory: Directory, fileName: String): File = {
    require(content.nonEmpty, "Content is empty")
    require(fileName.nonEmpty, "File name is empty")
    if (!directory.exists) directory.createDirectory()
    // Create file
    val file = new File(s"${directory.path}/$fileName.xml")
    file.createNewFile();
    new PrintWriter(file) {
      write(content)
      close()
    }
    file
  }

  def generateUppaalFile(model: ModelEncoding, directory: Directory): File = {
    val uppaalModel = xml.CosimUppaalTemplate.render(model).toString().trim
    writeToFile(uppaalModel, directory, "uppaal")
  }
}