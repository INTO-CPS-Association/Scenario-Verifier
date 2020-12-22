package synthesizer.ConfParser

import core._
import org.apache.logging.log4j.scala.Logging

object ScenarioConfGenerator extends Logging {
  def generate(model: MasterModel, name: String): String = {
    val scenario = model.scenario
    val builder = new StringBuilder(generateScenario(scenario, name))
    builder.append("initialization = []\n")
    builder.append("cosim-step = []\n")
    builder.toString()
  }


  def generatePort(port: PortRef): String = port.fmu + "." + port.port

  def generateConnections(connections: List[ConnectionModel]): String = {
    connections.map(o => f"${generatePort(o.srcPort)} -> ${generatePort(o.trgPort)}").mkString("connections = [\n", "\n", "]\n")
  }

  def generateFMUs(fmus: Map[String, FmuModel]):String = {
    fmus.map(o => {
      val inputs = o._2.inputs.map(i => f"${i._1} = {reactivity=${i._2.reactivity.toString}}").mkString("inputs = {\n", "\n", "},\n")
      val outputs = o._2.outputs.map(e => f"${e._1} = {${e._2.dependenciesInit.mkString("dependencies-init=[",",", "]")}, ${e._2.dependencies.mkString("dependencies=[",",", "]")}}").mkString("outputs = {\n", "\n", "}\n")
      f"${o._1} = { \n ${inputs} ${outputs} }"
    }).mkString("fmus = {\n", "\n", "}\n")
  }

  def generateScenario(scenario: ScenarioModel, name: String): String = {
    val builder = new StringBuilder()
    builder.addAll(f"name = ${name}\n")
    builder.addAll("scenario = {\n")
    builder.addAll(generateFMUs(scenario.fmus))
    builder.addAll(generateConnections(scenario.connections))
    builder.addAll("}\n")

    val c = builder.toString()
    logger.info(c)
    c
  }
}
