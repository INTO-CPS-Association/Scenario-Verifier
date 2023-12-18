package org.intocps.verification.scenarioverifier.core.masterModel
import org.intocps.verification.scenarioverifier.core._
import org.intocps.verification.scenarioverifier.core.masterModel.AlgorithmType.AlgorithmType

trait MasterModel extends ConfElement with SMTLibElement {
  def name: String
  require(name.nonEmpty, "Master model name cannot be empty")
  def scenario: ScenarioModel
  def instantiation: List[InstantiationInstruction]
  def initialization: List[InitializationInstruction]
  def cosimStep: Map[String, List[CosimStepInstruction]]
  def terminate: List[TerminationInstruction]
  private def formatInit = toArray(initialization.map(_.toConf(1)), "\n")
  private def formatStep(indentationLevel: Int): String =
    cosimStep
      .map { case (stepName, stepInstructions) =>
        s""" $stepName =
         |${indentBy(indentationLevel + 1)}${toArray(stepInstructions.map(_.toConf(indentationLevel + 2)), "\n")}
          """
      }
      .mkString("{", ",", "}")

  override def toConf(indentationLevel: Int = 0): String = {
    s"""name = $name
       |scenario = {
       |  ${scenario.toConf(indentationLevel + 1)}
       |}
       |initialization = $formatInit
       |cosim-step = ${formatStep(indentationLevel)}
       |""".stripMargin
  }

  def toSMTLib(algorithmConstraint: Map[AlgorithmType, String]): String = {
    require(algorithmConstraint.nonEmpty, "at least one algorithm must be specified")
    s"""
       |(set-option :produce-models true)
       |(set-logic QF_LIA)
       |(set-option :produce-unsat-cores true)
       |(set-option :verbosity 1)
       |${algorithmConstraint.values.mkString("\n")}
       |(exit)
      """.stripMargin
  }

  protected def coSimStepSMTLIB(synthesize: Boolean): String = {
    val instructions =
      if (cosimStep.isEmpty)
        List.empty
      else {
        val first_cosimStep_algo = cosimStep.head._2
        first_cosimStep_algo
          .filter(instruction =>
            instruction.isInstanceOf[Get] ||
              instruction.isInstanceOf[org.intocps.verification.scenarioverifier.core.Set] ||
              instruction.isInstanceOf[Step])
      }
    formatAlgorithmSMTLib(AlgorithmType.step, instructions, synthesize)
  }

  protected def formatAlgorithmSMTLib(algorithmType: AlgorithmType, instructions: List[SMTLibElement], synthesize: Boolean): String = {
    val algorithmAssertions = instructions.indices.map(i => s"(assert (= ${instructions(i).toSMTLib} $i))").mkString("\n")
    s"""
         |; ${algorithmType.toString} algorithm
         |(push 1)
         |${scenario.toSMTLIB(algorithmType)}
         |${if (!synthesize) algorithmAssertions else ""}
         |(check-sat)
         |${if (synthesize) "(get-model)" else ""}
         |(pop 1)
         |""".stripMargin
  }

  protected def formatEvents(synthesize: Boolean, isParallel: Boolean): String = ""

  def smtLIBConstraints(algorithmTypes: List[AlgorithmType], synthesize: Boolean = false): String = {
    require(algorithmTypes.nonEmpty, "algorithmTypes must not be empty")
    var algorithmConstraints = Map.empty[AlgorithmType, String]
    if (algorithmTypes.contains(AlgorithmType.init)) {
      val initInstructions = initialization
        .filterNot(instruction => instruction.isInstanceOf[EnterInitMode] || instruction.isInstanceOf[ExitInitMode])
      algorithmConstraints += AlgorithmType.init -> formatAlgorithmSMTLib(AlgorithmType.init, initInstructions, synthesize)
    }
    if (algorithmTypes.contains(AlgorithmType.step)) {
      algorithmConstraints += AlgorithmType.step -> coSimStepSMTLIB(synthesize)
    }
    if (algorithmTypes.contains(AlgorithmType.event)) {
      algorithmConstraints += AlgorithmType.event -> formatEvents(synthesize, isParallel = false)
    }
    toSMTLib(algorithmConstraints)
  }
}

final case class MasterModelFMI2(
    name: String,
    scenario: FMI2ScenarioModel,
    instantiation: List[InstantiationInstruction] = List.empty,
    initialization: List[InitializationInstruction] = List.empty,
    cosimStep: Map[String, List[CosimStepInstruction]] = Map.empty,
    terminate: List[TerminationInstruction] = List.empty)
    extends MasterModel {
  require(name.nonEmpty, "Master model name cannot be empty")

  override def toSMTLib: String =
    smtLIBConstraints(List(AlgorithmType.init, AlgorithmType.step), synthesize = false)
}

final case class MasterModelFMI3(
    name: String,
    scenario: FMI3ScenarioModel,
    instantiation: List[InstantiationInstruction] = List.empty,
    initialization: List[InitializationInstruction] = List.empty,
    cosimStep: Map[String, List[CosimStepInstruction]] = Map.empty,
    eventStrategies: Map[String, EventStrategy] = Map.empty,
    terminate: List[TerminationInstruction] = List.empty)
    extends MasterModel {
  require(name.nonEmpty, "Master model name cannot be empty")

  /**
   * Formats the master model to a CONF file
   */
  override def toConf(indentationLevel: Int = 0): String = {
    val strategies =
      eventStrategies.map(strategy => s"${sanitizeString(strategy._1)} : ${strategy._2.toConf(indentationLevel + 1)}").mkString("\n")
    s"""
       |${super.toConf(indentationLevel)}
       |event-strategies : {$strategies}
       |""".stripMargin
  }

  protected override def formatEvents(synthesize: Boolean, isParallel: Boolean): String = {
    scenario.eventEntrances
      .map(eventEntrance => {
        val algorithmAssertions = if (!synthesize) {
          val instructions = eventStrategies.values
            .find(_.eventEntrance == eventEntrance)
            .getOrElse(throw new RuntimeException(s"Event entrance ${eventEntrance.clocks.mkString(",")} not found"))
            .algorithm
          instructions.indices.map(i => s"(assert (= ${instructions(i).toSMTLib} $i))").mkString("\n")
        } else ""
        s"""
         |; Event Entrance for the clocks ${eventEntrance.clocks.map(_.toSMTLib).mkString(", ")}
         |(push 1)
         |${scenario.eventSMTLib(eventEntrance, isParallel)}
         |$algorithmAssertions
         |(check-sat)
         |${if (synthesize) "(get-model)" else ""}
         |(pop 1)
         |""".stripMargin
      })
      .mkString("\n")
  }

  /**
   * Formats the master model to a SMT-LIB file
   *
   * @return
   *   The SMT-LIB representation of the master model as a string
   */

  override def toSMTLib: String =
    smtLIBConstraints(List(AlgorithmType.init, AlgorithmType.step, AlgorithmType.event), synthesize = false)
}
