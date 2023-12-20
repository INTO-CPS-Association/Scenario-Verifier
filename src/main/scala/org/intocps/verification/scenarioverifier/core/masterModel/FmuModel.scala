package org.intocps.verification.scenarioverifier.core.masterModel

import org.intocps.verification.scenarioverifier.core.masterModel.AlgorithmType.AlgorithmType

trait FmuModel extends ConfElement {
  def inputs: Map[String, InputPortModel]
  def outputs: Map[String, OutputPortModel]
  def canRejectStep: Boolean
  def path: String

  override def toConf(indentationLevel: Int): String = {
    s"""
       |${indentBy(indentationLevel + 1)}can-reject-step = $canRejectStep,
       |${indentBy(indentationLevel + 1)}inputs = {
       |${indentBy(indentationLevel + 2)}${inputs
        .map { case (port, inputPortModel) => s"${sanitizeString(port)} = ${inputPortModel.toConf(indentationLevel)}" }
        .mkString("\n")}
       |${indentBy(indentationLevel + 1)}},
       |${indentBy(indentationLevel + 1)}outputs = {
       |${indentBy(indentationLevel + 2)}${outputs
        .map { case (port, outputPortModel) => s"${sanitizeString(port)} = ${outputPortModel.toConf(indentationLevel)}" }
        .mkString("\n")}
       |${indentBy(indentationLevel + 1)}}
       |""".stripMargin
  }
}

final case class Fmu2Model(
    inputs: Map[String, FMI2InputPortModel],
    outputs: Map[String, FMI2OutputPortModel],
    canRejectStep: Boolean,
    path: String)
    extends FmuModel {
  require(
    inputs.keySet.intersect(outputs.keySet).isEmpty,
    s"FMU inputs (${inputs.keySet.mkString(", ")}) and outputs (${outputs.keySet.mkString(", ")}) must be disjoint.")

  lazy val reactiveInputs: Map[String, FMI2InputPortModel] = inputs.filter(_._2.reactivity == Reactivity.reactive)
  lazy val delayedInputs: Map[String, FMI2InputPortModel] = inputs.filter(_._2.reactivity == Reactivity.delayed)

  private def portVarsDecl(fmuName: String, ports: List[String]): String =
    ports.map(port => s"(declare-const ${sanitizeString(fmuName)}_${sanitizeString(port)} Int)").mkString("\n")

  private def dependenciesAssertions(fmuName: String, port: String, dependencies: List[String]): String =
    dependencies
      .map(dependency =>
        s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(port)} ${sanitizeString(fmuName)}_${sanitizeString(dependency)}))")
      .mkString("\n")

  private def initSMTLib(fmuName: String): String = {
    // All outputs must be after their dependencies
    val outputsAfterDependencies = outputs
      .map { case (port, outputPortModel) =>
        dependenciesAssertions(fmuName, port, outputPortModel.dependenciesInit)
      }
      .filter(_.nonEmpty)
      .mkString("\n")
    s"""
       |; Feed through dependencies
       |$outputsAfterDependencies
       |""".stripMargin
  }

  private def stepSMTLib(fmuName: String): String = {
    val stepName = s"${sanitizeString(fmuName)}_step"
    val step = s"(declare-const $stepName Int)"
    // All delayed inputs must be after the step (bigger than the step)
    // val delayedInputsAfterStep = delayedInputs.map(port => s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(port._1)} $stepName))").mkString("\n")
    // All reactive inputs must be before the step (smaller than the step)
    val reactiveInputsBeforeStep = reactiveInputs
      .map { case (port, _) =>
        s"(assert (< ${sanitizeString(fmuName)}_${sanitizeString(port)} $stepName))"
      }
      .mkString("\n")
    // All outputs must be after the step (bigger than the step)
    // val outputsAfterStep = outputs.map {
    //  case (port, _) => s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(port)} $stepName))"
    // }.mkString("\n")

    // All outputs must be after their dependencies
    val outputsAfterDependencies = outputs
      .map { case (port, outputPortModel) =>
        dependenciesAssertions(fmuName, port, outputPortModel.dependencies)
      }
      .filter(_.nonEmpty)
      .mkString("\n")
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
      case _ => ""
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

  override def toConf(indentationLevel: Int): String = {
    s"""
       |${indentBy(indentationLevel)}{
       |${super.toConf(indentationLevel)}
       |${indentBy(indentationLevel)}}
       |""".stripMargin
  }
}

final case class Fmu3Model(
    inputs: Map[String, FMI3InputPortModel],
    outputs: Map[String, FMI3OutputPortModel],
    inputClocks: Map[String, InputClockModel],
    outputClocks: Map[String, OutputClockModel],
    canRejectStep: Boolean,
    path: String = "")
    extends FmuModel {

  lazy val timeBasedClocks: Map[String, InputClockModel] = inputClocks.filter(_._2.typeOfClock == ClockType.timed)

  private def clockedInputs: Map[String, FMI3InputPortModel] = inputs.filter(input => input._2.clocks.nonEmpty)

  private def clockedOutputs: Map[String, FMI3OutputPortModel] = outputs.filter(output => output._2.clocks.nonEmpty)

  def reactiveInputs: Map[String, FMI3InputPortModel] =
    inputs.filter(input => input._2.reactivity == Reactivity.reactive && input._2.clocks.isEmpty)

  def delayedInputs: Map[String, FMI3InputPortModel] =
    inputs.filter(input => input._2.reactivity == Reactivity.delayed && input._2.clocks.isEmpty)

  require(
    inputs.keySet.intersect(outputs.keySet).isEmpty,
    s"FMU inputs (${inputs.keySet.mkString(", ")}) and outputs (${outputs.keySet.mkString(", ")}) must be disjoint.")

  override def toConf(indentationLevel: Int): String = {
    s"""
       |${indentBy(indentationLevel)}{
       |${super.toConf(indentationLevel)}
       |${indentBy(indentationLevel + 1)}input-clocks = {
       |${indentBy(indentationLevel + 2)}${inputClocks
        .map { case (port, inputClockModel) => s"${sanitizeString(port)} = ${inputClockModel.toConf()}" }
        .mkString(s"\n${indentBy(indentationLevel + 2)}")}
       |${indentBy(indentationLevel + 1)}},
       |${indentBy(indentationLevel + 1)}output-clocks = {
       |${indentBy(indentationLevel + 2)}${outputClocks
        .map { case (port, outputClockModel) => s"${sanitizeString(port)} = ${outputClockModel.toConf()}" }
        .mkString(s"\n${indentBy(indentationLevel + 2)}")}
       |${indentBy(indentationLevel + 1)}},
       |${indentBy(indentationLevel)}}
       |""".stripMargin
  }

  /*
  def coSimStepSMTLIB(fmuName: String): String = {
    s"""
       |; FMU $fmuName constraints
       |; Delayed inputs are after Step
       |${delayedInputs.map(input => s"(assert (> ${sanitizeString(fmuName)}_step ${sanitizeString(fmuName)}_${sanitizeString(input._1)}))").mkString("\n")}
       |; Reactive inputs are before Step
       |${reactiveInputs.map(input => s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(input._1)} ${sanitizeString(fmuName)}_step))").mkString("\n")}
       |; Step is before outputs
       |${outputs.filter(output => output._2.clocks.isEmpty).map(output => s"(assert (> ${sanitizeString(fmuName)}-step ${sanitizeString(fmuName)}_${sanitizeString(output._1)}))").mkString("\n")}
       |; Feed-through means that the input is before the output
       |${outputs.filter(output => output._2.dependencies.nonEmpty).map(output => output._2.dependencies.map(dependency => s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(dependency)} ${sanitizeString(fmuName)}_${sanitizeString(output._1)}))").mkString("\n")).mkString("\n")}
       |""".stripMargin
  }

  private def initSMTLIB(fmuName: String): String = {
    s"""
       |; FMU $fmuName constraints
       |; Feedthrough means that the input is before the output
       |${outputs.filter(output => output._2.dependenciesInit.nonEmpty).map(output => output._2.dependencies.map(dependency => s"(assert (> ${sanitizeString(fmuName)}_${sanitizeString(dependency)} ${sanitizeString(fmuName)}_${sanitizeString(output._1)}))").mkString("\n")).mkString("\n")}
       |""".stripMargin
  }

  def eventSMTLib(fmuName: String, clocks: List[String]): String = {
    val clockNamesActions = clocks.map(clock => s"${fmuName}_$clock")
    val clockedVariables = clocks.foreach(clock => {
      val inputsOfClock = clockedInputs.filter(input => input._2.clocks.contains(clock))
      val outputsOfClock = clockedOutputs.filter(output => output._2.clocks.contains(clock))
      val inputClockActionNames = inputsOfClock.map(input => s"${fmuName}_${input._1}_set")
      val outputClockActionNames = outputsOfClock.map(output => s"${fmuName}_${output._1}_get")
      val clockedVariableActions = (inputClockActionNames ++ outputClockActionNames).map(port => s"(declare-const $port Int)")
      val clockedVariableConstraints = (inputClockActionNames ++ outputClockActionNames).map(port => s"(assert (>= $port 0))")
      // Clocked variables are before the step
      val clockedVariableStep = (inputClockActionNames ++ outputClockActionNames).map(port => s"(assert (> ${fmuName}_step $port))")
      // All actions of a clock happen after a clock event
      val clockedVariableClock = (inputClockActionNames ++ outputClockActionNames).map(port => s"(assert (< $clock $port))")
    })
    val clockActions = clockNamesActions.map(clock => s"(declare-const $clock Int)")
    val clockConstraints = clockNamesActions.map(clock => s"(assert (>= $clock 0))")
    val clockStep = clockNamesActions.map(clock => s"(assert (> ${fmuName}_step $clock))")


    s"""
       |; FMU $fmuName constraints
       |; Declare clock variables
       |${clockActions.mkString("\n")}
       |${clockConstraints.mkString("\n")}
       |; Clocked Variables
       |""".stripMargin
  }
   */
}
