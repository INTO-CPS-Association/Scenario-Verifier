package org.intocps.verification.scenarioverifier.traceanalyzer

import org.apache.logging.log4j.scala.Logging
import org.intocps.verification.scenarioverifier.core.ModelEncoding

object TraceAnalyzer extends Logging {
  def createUppaalTrace(scenarioName: String, trace: Iterator[String], modelEncoding: ModelEncoding): UppaalTrace = {
    val parser = new TraceParser(modelEncoding)
    val modelStates = parser.parseTrace(trace)
    val tempStates = modelStates.filter(_.isInitState).dropWhile(_.action.actionNumber == 7)
    val errorInSimulation: Boolean = tempStates.exists(_.action.actionNumber == 8)
    val initStates = (if (errorInSimulation) tempStates.slice(1, tempStates.indexWhere(_.action.actionNumber == 8) + 2) else tempStates).grouped(2).map(_.head).toList
    val simulationStates = modelStates.filter(_.isSimulation).grouped(2).map(_.head).toList
    UppaalTrace(modelEncoding, initStates, simulationStates, scenarioName)
  }

  /**
   * Analyse a trace file and plot the results
   *
   * @param scenarioName    The name of the scenario
   * @param trace           The trace file
   * @param modelEncoding   The model encoding
   * @param outputDirectory The directory to save the plot to
   */
  def AnalyseScenario(scenarioName: String, trace: Iterator[String], modelEncoding: ModelEncoding, outputDirectory: String): String = {
    val uppaalTrace = createUppaalTrace(scenarioName, trace, modelEncoding)
    ScenarioPlotter.plot(uppaalTrace, outputDirectory)
  }
}

