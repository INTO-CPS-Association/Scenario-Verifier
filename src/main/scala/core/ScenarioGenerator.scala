package core

import org.apache.logging.log4j.scala.Logging

object ScenarioGenerator extends Logging {
  def generate(model: ModelEncoding): String = {
    xml.CosimUppaalTemplate.render(model).toString().trim
  }
}
