package trace_analyzer

import core.ModelEncoding
import org.apache.logging.log4j.scala.Logging

object TraceAnalyzer extends Logging{

  def AnalyseScenario(scenarioName: String, trace: Iterator[String], modelEncoding: ModelEncoding): Unit = {
    val parser = new TraceParser(modelEncoding)
    val modelStates = parser.parseScenarios(trace)
    //TODO: This filtering could be avoided by changing the Uppaal-model
    val tempStates = modelStates.filter(_.isInitState).dropWhile(_.action.actionNumber == 7)
    val initStates = tempStates.slice(1, tempStates.indexWhere(_.action.actionNumber == 8) + 2).grouped(2).map(_.head).toList

    val simulationStates = modelStates.filter(_.isSimulation).grouped(2).map(_.head).toList
    ScenarioPlotter.plot(new UppaalTrace(modelEncoding, initStates, simulationStates, scenarioName))
  }
}

