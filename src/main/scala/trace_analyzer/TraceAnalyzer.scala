package trace_analyzer

import java.io.File

import core.ModelEncoding
import org.apache.logging.log4j.scala.Logging

object TraceAnalyzer extends Logging{

  def AnalyseScenario(scenarioName: String, trace: Iterator[String], modelEncoding: ModelEncoding, outputDirectory: String): Unit = {
    val parser = new TraceParser(modelEncoding)
    val modelStates = parser.parseScenarios(trace)
    val tempStates = modelStates.filter(_.isInitState).dropWhile(_.action.actionNumber == 7)

    val errorInSimulation : Boolean = tempStates.exists(_.action.actionNumber == 8)
    val initStates = (if(errorInSimulation) tempStates.slice(1, tempStates.indexWhere(_.action.actionNumber == 8) + 2) else tempStates).grouped(2).map(_.head).toList
    val simulationStates = modelStates.filter(_.isSimulation).grouped(2).map(_.head).toList

    ScenarioPlotter.plot(new UppaalTrace(modelEncoding, initStates, simulationStates, scenarioName), outputDirectory)
  }

  def AnalyseScenario(scenarioName: String, trace: Iterator[String], modelEncoding: ModelEncoding, file: File): Unit = {
    val parser = new TraceParser(modelEncoding)
    val modelStates = parser.parseScenarios(trace)
    val tempStates = modelStates.filter(_.isInitState).dropWhile(_.action.actionNumber == 7)

    val errorInSimulation : Boolean = tempStates.exists(_.action.actionNumber == 8)
    val initStates = (if(errorInSimulation) tempStates.slice(1, tempStates.indexWhere(_.action.actionNumber == 8) + 2) else tempStates).grouped(2).map(_.head).toList
    val simulationStates = modelStates.filter(_.isSimulation).grouped(2).map(_.head).toList

    ScenarioPlotter.plot(new UppaalTrace(modelEncoding, initStates, simulationStates, scenarioName), file)
  }
}

