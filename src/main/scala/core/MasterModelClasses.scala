package core

import core.AlgorithmType.AlgorithmType
import core.Reactivity.Reactivity
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._

import scala.collection.immutable

object AlgorithmType extends Enumeration {
  type AlgorithmType = Value
  val init, step, event = Value
}

object Reactivity extends Enumeration {
  type Reactivity = Value
  val reactive, delayed, noPort = Value
}


trait ConfElement {
  def toConf(indentationLevel: Int): String

  private val indentation = "  "

  private def shouldBeSanitized(str: String): Boolean =
    str.replaceAll("\\W", "") != str

  protected def sanitizeString(str: String): String =
    if (shouldBeSanitized(str)) "\"" + str + "\""
    else str

  protected def indentBy(indentationLevel: Int): String = indentation * indentationLevel

  protected def generatePort(port: PortRef): String = sanitizeString(port.fmu + "." + port.port)

  protected def toArray(elements: List[String], delimiter: String = ","): String = {
    elements.filterNot(s => s.isEmpty || s.isBlank).mkString("[", delimiter, "]")
  }

  protected def toMap(elements: List[String]): String = {
    elements.mkString("{", ",", "}")
  }
}

trait SMTLibElement {
  def toSMTLib: String
}

trait UppaalModel {
  protected def sanitize(s: String): String = s
    .replaceAll("\\W", "")

  def fmuPortName(portRef: PortRef) = s"${sanitize(portRef.fmu)}_${sanitize(portRef.port)}"

  def toUppaal: String
}

case class InputPortModel(reactivity: Reactivity) extends ConfElement {
  override def toConf(indentationLevel: Int = 0): String = s"{reactivity=${reactivity.toString}}"
}

final case class OutputPortModel(dependenciesInit: List[String], dependencies: List[String]) extends UppaalModel with ConfElement {
  override def toUppaal: String = s"{${dependencies.mkString(",")}}"

  override def toConf(indentationLevel: Int = 0): String = s"{dependencies-init=${toArray(dependenciesInit)}, dependencies=${toArray(dependencies)}}"
}

final case class FmuModel(
                           inputs: Map[String, InputPortModel],
                           outputs: Map[String, OutputPortModel],
                           canRejectStep: Boolean,
                           path: String
                         ) extends ConfElement {
  require(inputs.keySet.intersect(outputs.keySet).isEmpty, s"FMU inputs (${inputs.keySet.mkString(", ")}) and outputs (${outputs.keySet.mkString(", ")}) must be disjoint.")

  lazy val reactiveInputs: Map[String, InputPortModel] = inputs.filter(_._2.reactivity == Reactivity.reactive)
  lazy val delayedInputs: Map[String, InputPortModel] = inputs.filter(_._2.reactivity == Reactivity.delayed)

  override def toConf(indentationLevel: Int): String = {
    s"""
       |${indentBy(indentationLevel)}{
       |${indentBy(indentationLevel + 1)}can-reject-step = $canRejectStep,
       |${indentBy(indentationLevel + 1)}inputs = {
       |${indentBy(indentationLevel + 2)}${inputs.map { case (port, inputPortModel) => s"${sanitizeString(port)} = ${inputPortModel.toConf()}" }.mkString("\n")}
       |${indentBy(indentationLevel + 1)}},
       |${indentBy(indentationLevel + 1)}outputs = {
       |${indentBy(indentationLevel + 2)}${outputs.map { case (port, outputPortModel) => s"${sanitizeString(port)} = ${outputPortModel.toConf()}" }.mkString("\n")}
       |${indentBy(indentationLevel + 1)}}
       |${indentBy(indentationLevel)}}""".stripMargin
  }

  private def portVarsDecl(fmuName: String, ports: List[String]): String =
    ports.map(port => s"(declare-const ${sanitizeString(fmuName)}_${sanitizeString(port)} Int)").mkString("\n")

  private def dependenciesAssertions(fmuName: String, port: String, dependencies: List[String]): String =
    dependencies.map(dependency => s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(port)} ${sanitizeString(fmuName)}_${sanitizeString(dependency)}))").mkString("\n")

  private def initSMTLib(fmuName: String): String = {
    // All outputs must be after their dependencies
    val outputsAfterDependencies = outputs.map {
      case (port, outputPortModel) =>
        dependenciesAssertions(fmuName, port, outputPortModel.dependenciesInit)
    }.filter(_.nonEmpty).mkString("\n")
    s"""
       |; Feed through dependencies
       |$outputsAfterDependencies
       |""".stripMargin
  }

  private def stepSMTLib(fmuName: String): String = {
    val stepName = s"${sanitizeString(fmuName)}_step"
    val step = s"(declare-const $stepName Int)"
    // All delayed inputs must be after the step (bigger than the step)
    //val delayedInputsAfterStep = delayedInputs.map(port => s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(port._1)} $stepName))").mkString("\n")
    // All reactive inputs must be before the step (smaller than the step)
    val reactiveInputsBeforeStep = reactiveInputs.map {
      case (port, _) => s"(assert (< ${sanitizeString(fmuName)}_${sanitizeString(port)} $stepName))"
    }.mkString("\n")
    // All outputs must be after the step (bigger than the step)
    //val outputsAfterStep = outputs.map {
    //  case (port, _) => s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(port)} $stepName))"
    //}.mkString("\n")

    // All outputs must be after their dependencies
    val outputsAfterDependencies = outputs.map {
      case (port, outputPortModel) =>
        dependenciesAssertions(fmuName, port, outputPortModel.dependencies)
    }.filter(_.nonEmpty).mkString("\n")
    s"""
       |; Step action of the FMU
       |$step
       |; All reactive inputs must be before the step
       |$reactiveInputsBeforeStep
       |; Feed through dependencies - inputs must be before outputs
       |$outputsAfterDependencies
       |""".stripMargin
  }


  def toSMTLib(fmuName: String, algorithmType: AlgorithmType): String = {
    val specificConstraints = algorithmType match {
      case AlgorithmType.init => initSMTLib(fmuName)
      case AlgorithmType.step => stepSMTLib(fmuName)
    }
    s"""
       |; FMU $fmuName constraints
       |; Output actions of $fmuName
       |${portVarsDecl(fmuName, outputs.keySet.toList)}
       |; Input actions of $fmuName
       |${portVarsDecl(fmuName, inputs.keySet.toList)}
       |$specificConstraints
       |""".stripMargin
  }
}

final case class ConnectionModel(
                                  srcPort: PortRef,
                                  trgPort: PortRef,
                                ) extends UppaalModel with ConfElement with SMTLibElement {
  require(srcPort.fmu != trgPort.fmu, "srcPort and trgPort must not be in the same FMU")

  override def toUppaal: String =
    f"""{${sanitize(srcPort.fmu)}, ${sanitize(fmuPortName(srcPort))}, ${sanitize(trgPort.fmu)}, ${sanitize(fmuPortName(trgPort))}}"""

  override def toConf(indentationLevel: Int): String = s"${indentBy(indentationLevel)}${generatePort(srcPort)} -> ${generatePort(trgPort)}"

  def toSMTLib: String =
    s"(assert (< ${srcPort.toSMTLib} ${trgPort.toSMTLib}))"
}

final case class ScenarioModel(
                                fmus: Map[String, FmuModel],
                                config: AdaptiveModel,
                                connections: List[ConnectionModel],
                                maxPossibleStepSize: Int
                              ) extends ConfElement {

  require(fmus.nonEmpty, "fmus must not be empty")
  require(maxPossibleStepSize > 0, "maxPossibleStepSize must be greater than 0")

  def enrich(): ScenarioModel = {
    val enrichedFmus = fmus.map {
      fmu =>
        connections.filter(c => c.srcPort.fmu.equalsIgnoreCase(fmu._1) || c.trgPort.fmu.equalsIgnoreCase(fmu._1))
          .foldLeft(fmu)(
            (fmuModel, c) => {
              val model = if (c.trgPort.fmu.equalsIgnoreCase(fmu._1) && !fmuModel._2.inputs.contains(c.trgPort.port))
                fmuModel._2.copy(inputs = fmuModel._2.inputs + (c.trgPort.port -> InputPortModel(Reactivity.delayed)))
              else if (c.srcPort.fmu.equalsIgnoreCase(fmu._1) && !fmuModel._2.outputs.contains(c.srcPort.port))
                fmuModel._2.copy(outputs = fmuModel._2.outputs + (c.srcPort.port -> OutputPortModel(List.empty, List.empty)))
              else fmuModel._2
              (fmuModel._1, model)
            }
          )
    }
    this.copy(fmus = enrichedFmus)
  }

  override def toConf(indentationLevel: Int): String = {
    s"""
       |${indentBy(indentationLevel)}fmus = {
       |${indentBy(indentationLevel + 1)}${fmus.map { case (fmu, fmuModel) => s"$fmu = ${fmuModel.toConf(indentationLevel + 1)}" }.mkString("\n")}
       |${indentBy(indentationLevel)}}
       |${indentBy(indentationLevel)}connections = ${toArray(connections.map(_.toConf(indentationLevel + 1)))}
       |""".stripMargin
  }

  def toSMTLib(algorithmType: AlgorithmType): String = {
    require(fmus.nonEmpty, "fmus must not be empty")
    require(connections.nonEmpty, "connections must not be empty")
    require(config.configurations.size <= 1, "the scenario must not be adaptive")
    val actionsWithoutStep: immutable.Iterable[String] =
      fmus.foldLeft(immutable.Iterable.empty[String])((actions, fmu) =>
        fmu._2.inputs.map(port => s"${sanitizeString(fmu._1)}_${sanitizeString(port._1)}").toList ++
          fmu._2.outputs.map(port => s"${sanitizeString(fmu._1)}_${sanitizeString(port._1)}").toList ++
          actions
      ).toList.sorted

    val actions = if (algorithmType == AlgorithmType.step) actionsWithoutStep ++ fmus.map(fmu => s"${fmu._1}_step").toList else actionsWithoutStep
    val numberOfActions = actions.size
    val fmuDeclarations = fmus.keySet.toList.sorted.map(fmu => fmus(fmu).toSMTLib(fmu, algorithmType)).mkString("\n")
    val connectionAssertions = connections.map(_.toSMTLib).mkString("\n")
    val reactiveInputs = fmus.flatMap(fmu => fmu._2.reactiveInputs.map(i => PortRef(fmu._1, i._1))).toList
    val reactiveConnections = connections.filter(c => reactiveInputs.contains(c.srcPort))
    val delayedConnections = connections.filterNot(c => reactiveInputs.contains(c.trgPort))
    val delayedConstraints = delayedConnectionConstraints(delayedConnections)
    s"""$fmuDeclarations
       |; Connections - Assert that the source port is smaller than the target port
       |$connectionAssertions
       |; Delayed connections - The get and set can either be done before or after the step - but they need to be consistent across the connection
       |; Assert that all the actions are bigger than 0
       |(assert (and ${actions.map(a => s"(>= $a 0)").mkString("\n\t")}))
       |; Assert that all the actions are smaller than the maxAction
       |(assert (and ${actions.map(action => s"(< $action $numberOfActions)").mkString("\n\t")}))
       |; Assert that all actions are different
       |(assert (distinct ${actions.mkString("\n\t")}))
       |""".stripMargin
  }

  private def delayedConnectionConstraints(delayedConnections: List[ConnectionModel]) = {
    delayedConnections.map(c => {
      val srcActionName = s"${sanitizeString(c.srcPort.fmu)}_${sanitizeString(c.srcPort.port)}"
      val trgActionName = s"${sanitizeString(c.trgPort.fmu)}_${sanitizeString(c.trgPort.port)}"
      val srcFMUStep = s"${sanitizeString(c.srcPort.fmu)}_step"
      val trgFMUStep = s"${sanitizeString(c.trgPort.fmu)}_step"
      //Delayed connections - The get and set can either be done before or after the step
      s"""
         |(assert (or
         |          (and (< $srcActionName $srcFMUStep) (< $trgActionName $trgFMUStep))
         |          (and (> $srcActionName $srcFMUStep) (> $trgActionName $trgFMUStep))
         |))""".stripMargin
    }).mkString("\n")
  }
}


final case class MasterModel(
                              name: String,
                              scenario: ScenarioModel,
                              instantiation: List[InstantiationInstruction] = List.empty,
                              initialization: List[InitializationInstruction] = List.empty,
                              cosimStep: Map[String, List[CosimStepInstruction]] = Map.empty,
                              terminate: List[TerminationInstruction] = List.empty
                            ) extends ConfElement {
  require(name.nonEmpty, "Master model name cannot be empty")

  override def toConf(indentationLevel: Int = 0): String = {
    val init = toArray(initialization.map(_.toConf(indentationLevel + 1)), "\n")
    val step = cosimStep.map { case (stepName, stepInstructions) =>
      s""" $stepName =
         |${indentBy(indentationLevel + 1)}${toArray(stepInstructions.map(_.toConf(indentationLevel + 2)), "\n")}
          """
    }.mkString("{", ",", "}")
    s"""name = $name
       |scenario = {
       |  ${scenario.toConf(indentationLevel + 1)}
       |}
       |initialization = $init
       |cosim-step = $step
       |""".stripMargin
  }

  def toSMTLib(algorithmTypes: List[AlgorithmType],
               produceModel: Boolean = false): String = {
    require(algorithmTypes.nonEmpty, "algorithmTypes must not be empty")
    val initializationConstraints = if (algorithmTypes.contains(AlgorithmType.init)) {
      val initInstructions = initialization
        .filter(instruction => instruction.isInstanceOf[InitGet] || instruction.isInstanceOf[InitSet])
        .map(_.toSMTLib)
      val initAlgorithmAssertions = initInstructions.indices.map(i => s"(assert (= ${initInstructions(i)} $i))").mkString("\n")
      s"""
         |; Scenario encoding - Initialization Procedure
         |${scenario.toSMTLib(AlgorithmType.init)}
         |$initAlgorithmAssertions
         |""".stripMargin
    } else ""

    val coSimStepConstraints: String = if (algorithmTypes.contains(AlgorithmType.step)) {
      val first_cosimStep = cosimStep.head._2
      val stepInstructions = first_cosimStep
        .filter(instruction => instruction.isInstanceOf[Get] || instruction.isInstanceOf[Set] || instruction.isInstanceOf[Step])
        .map(_.toSMTLib)
      val stepAlgorithmAssertions = stepInstructions.indices.map(i => s"(assert (= ${stepInstructions(i)} $i))").mkString("\n")
      s"""
         |; Scenario encoding - CosimStep Procedure
         |${scenario.toSMTLib(AlgorithmType.step)}
         |$stepAlgorithmAssertions
         |""".stripMargin
    } else ""

    val constraints: List[String] =
      List(initializationConstraints, coSimStepConstraints).filter(_.nonEmpty)

    s"""
       |(set-option :produce-models true)
       |(set-logic QF_LIA)
       |(set-option :produce-unsat-cores true)
       |(set-option :verbosity 1)
       |${
      constraints.map(constraint =>
        s"""
           |(push 1)
           |$constraint
           |(check-sat)
           |${if (produceModel) "(get-model)" else ""}
           |(pop 1)
           |""".stripMargin).mkString("\n")
    }
       |(exit)
      """.stripMargin
  }
}

case class MasterModelDTO(
                           name: String,
                           scenario: ScenarioModel,
                           initialization: List[InitializationInstruction],
                           cosimStep: Map[String, List[CosimStepInstruction]],
                         )


case class AdaptiveModel(
                          configurableInputs: List[PortRef],
                          configurations: Map[String, ConfigurationModel]
                        )

case class ConfigurationModel(
                               inputs: Map[PortRef, InputPortModel],
                               cosimStep: String,
                               connections: List[ConnectionModel]
                             )

object MasterModelDTO {
  implicit val configurationModelEncoder: Encoder.AsObject[ConfigurationModel] = deriveEncoder[ConfigurationModel]
  implicit val ConfigurationModelDecoder: Decoder[ConfigurationModel] = deriveDecoder[ConfigurationModel]

  implicit val scenarioModelEncoder: Encoder.AsObject[ScenarioModel] = deriveEncoder[ScenarioModel]
  implicit val scenarioModelDecoder: Decoder[ScenarioModel] = deriveDecoder[ScenarioModel]

  implicit val adaptiveModelEncoder: Encoder.AsObject[AdaptiveModel] = deriveEncoder[AdaptiveModel]
  implicit val adaptiveModelDecoder: Decoder[AdaptiveModel] = deriveDecoder[AdaptiveModel]

  implicit val connectionModelEncoder: Encoder.AsObject[ConnectionModel] = deriveEncoder[ConnectionModel]
  implicit val connectionModelDecoder: Decoder[ConnectionModel] = deriveDecoder[ConnectionModel]

  implicit val inputPortModelEncoder: Encoder.AsObject[InputPortModel] = deriveEncoder[InputPortModel]
  implicit val inputPortModelDecoder: Decoder[InputPortModel] = deriveDecoder[InputPortModel]

  implicit val stepLoopEncoder: Encoder.AsObject[StepLoop] = deriveEncoder[StepLoop]
  implicit val stepLoopDecoder: Decoder[StepLoop] = deriveDecoder[StepLoop]

  //implicit val masterModelEncoder: Encoder.AsObject[MasterModelDTO] = deriveEncoder[MasterModelDTO]
  //implicit val masterModelDecoder: Decoder[MasterModelDTO] = deriveDecoder[MasterModelDTO]

  implicit val portRefKeyEncoder: KeyEncoder[PortRef] = (portRef: PortRef) => portRef.fmu + "." + portRef.port
  implicit val portRefKeyDecoder: KeyDecoder[PortRef] = (portRef: String) => {
    val p = portRef.split(".").map(_.replace(".", ""))
    Some(PortRef(p.head, p.last))
  }

  implicit val reactiveDecoder: Decoder[Reactivity.Value] = Decoder.decodeEnumeration(Reactivity)
  implicit val reactiveEncoder: Encoder[Reactivity.Value] = Encoder.encodeEnumeration(Reactivity)
}