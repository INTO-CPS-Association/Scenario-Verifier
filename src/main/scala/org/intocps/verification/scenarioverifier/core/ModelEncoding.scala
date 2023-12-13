package org.intocps.verification.scenarioverifier.core

import org.apache.logging.log4j.scala.Logging

class ModelEncoding(model: MasterModel) extends Logging {

  private val noOpEncoding = "{noFMU, noOp, noPort, noStep, noFMU, noCommitment, noLoop}"

  def maxNInputs: Int = model.scenario.fmus.map(f => f._2.inputs.size).max

  def maxNOutputs: Int = model.scenario.fmus.map(f => f._2.outputs.size).max

  def nFMUs: Int = model.scenario.fmus.size

  def Hmax: Int = model.scenario.maxPossibleStepSize

  def nInternal: Int = Math.max(model.scenario.fmus.map(f => nInternal(f._2)).sum[Int], 1)

  def nConfigs: Int = Math.max(model.scenario.config.configurations.size, 1)

  def nInternal(f: FmuModel): Int = f.outputs.map(o => o._2.dependencies.length).sum[Int]

  def nInternalInit: Int = Math.max(model.scenario.fmus.map(f => nInternalInit(f._2)).sum[Int], 1)

  def nInternalInit(f: FmuModel): Int = f.outputs.map(o => o._2.dependenciesInit.length).sum[Int]

  def nExternal: Int = model.scenario.connections.length

  def fmuModels: Map[String, FmuModel] = model.scenario.fmus

  def stepVariables: String = (0 until nFMUs).map(_ => "H_max").mkString(",")

  def getEnabled: String = (0 until maxNOutputs).map(_ => "false").mkString(",")

  def setEnabled: String = (0 until maxNInputs).map(_ => "false").mkString(",")

  val fmuEncoding: Map[String, Int] = model.scenario.fmus.keys.zipWithIndex.toMap
  val fmuEncodingInverse = fmuEncoding.map(_.swap)

  def fmuNames = model.scenario.fmus.keys

  def fmuId(name: String) = fmuEncoding(name)

  def nInputs(f: String) = model.scenario.fmus(f).inputs.size

  def nOutputs(f: String) = model.scenario.fmus(f).outputs.size

  val fmuInputEncoding: Map[String, Map[String, Int]] =
    fmuNames.map(fName => (fName, model.scenario.fmus(fName).inputs.keys.zipWithIndex.toMap)).toMap
  val fmuInputEncodingInverse: Map[String, Map[Int, String]] = fmuInputEncoding.map(f => (f._1, f._2.map(_.swap)))

  val fmuOutputEncoding: Map[String, Map[String, Int]] =
    fmuNames.map(fName => (fName, model.scenario.fmus(fName).outputs.keys.zipWithIndex.toMap)).toMap
  val fmuOutputEncodingInverse: Map[String, Map[Int, String]] = fmuOutputEncoding.map(f => (f._1, f._2.map(_.swap)))

  val isAdaptive: Boolean = model.scenario.config.configurableInputs.nonEmpty

  def fmuInNames(f: String) = model.scenario.fmus(f).inputs.keys

  def fmuOutNames(f: String) = model.scenario.fmus(f).outputs.keys

  def fmuPortName(f: String, p: String) = s"""${f}_${p}"""

  val standardConnections: List[ConnectionModel] = model.scenario.connections

  def connections: Map[String, List[ConnectionModel]] =
    model.scenario.config.configurations.map(keyValue => (keyValue._1, keyValue._2.connections))

  val connectionEncoding: Map[String, Map[ConnectionModel, Int]] =
    connections.map(keyValue => (keyValue._1, keyValue._2.zipWithIndex.toMap))
  val connectionEncodingInverse: Map[String, Map[Int, ConnectionModel]] = connectionEncoding.map(f => (f._1, f._2.map(_.swap)))

  val configuration: Map[Int, (String, String)] =
    model.scenario.config.configurations.zipWithIndex.map(_.swap).toMap.map(i => (i._1, (i._2._1, i._2._2.cosimStep)))

  def fmuInputTypes(f: String): String = {
    val inputs = model.scenario.fmus(f).inputs
    val inputEnc = fmuInputEncodingInverse(f)
    (0 until nConfigs)
      .map(id => {
        val inputPortConfig: Map[PortRef, InputPortModel] =
          if (isAdaptive) model.scenario.config.configurations(configuration(id)._1).inputs.filter(_._1.fmu == f) else Map.empty
        ((0 until inputs.size).map(idx =>
          if (inputPortConfig.keys.toList.contains(PortRef(f, inputEnc(idx))))
            inputPortConfig(PortRef(f, inputEnc(idx))).reactivity.toString
          else {
            inputs(inputEnc(idx)).reactivity.toString
          }) ++ (inputs.size until maxNInputs).map(_ => "noPort")).mkString(",")
      })
      .mkString("{", "}, {", "}")
  }

  def connectionVariable: String = {
    (0 until nFMUs)
      .map(_ =>
        (0 until maxNOutputs)
          .map(_ => """{{undefined,0}, {undefined,0}}""")
          .mkString("{", ",", "}"))
      .mkString(",")
  }

  def external: String = {
    (0 until nConfigs)
      .map(id => {
        if (isAdaptive)
          (0 until nExternal).map(idx => connectionEncodingInverse(configuration(id)._1)(idx)).map(_.toUppaal).mkString(",")
        else
          standardConnections.map(_.toUppaal).mkString(",")
      })
      .mkString("{", "}, {", "}")
  }

  private val noFeedThrough: String = "{noFMU, noPort, noPort}"

  def encodeFeedThrough(extract: OutputPortModel => List[String]): String = {
    val feedthrough = model.scenario.fmus
      .flatMap(f =>
        f._2.outputs
          .flatMap(o =>
            extract(o._2)
              .map(d => s"{${f._1}, ${fmuPortName(f._1, d)}, ${fmuPortName(f._1, o._1)}}")))
      .mkString(",")
    if (feedthrough.nonEmpty)
      feedthrough
    else {
      noFeedThrough
    }
  }

  def feedthroughInStep: String =
    (0 until nConfigs)
      .map(_ => {
        encodeFeedThrough(_.dependencies)
      })
      .mkString("{", "}, {", "}")

  def feedthroughInInit: String =
    encodeFeedThrough(_.dependenciesInit)

  def mayRejectStep: String = (0 until nFMUs)
    .map(fmuId => model.scenario.fmus(fmuEncodingInverse(fmuId)).canRejectStep)
    .mkString(",")

  def nInstantiationOperations: Int = model.instantiation.length

  def nInitializationOperations: Int = model.initialization.length

  def nStepOperations: String = model.cosimStep.valuesIterator.map(_.length).mkString(",")

  val allAlgebraicLoopsInStep: Map[String, List[AlgebraicLoop]] =
    model.cosimStep.map(keyValue => {
      (keyValue._1, ModelQuery.recFilter(keyValue._2, i => i.isInstanceOf[AlgebraicLoop]).map(_.asInstanceOf[AlgebraicLoop]))
    })

  val allAlgebraicLoopsInInit: List[AlgebraicLoopInit] =
    ModelQuery.recInitFilter(model.initialization, i => i.isInstanceOf[AlgebraicLoopInit]).map(_.asInstanceOf[AlgebraicLoopInit])

  def nAlgebraicLoopsInStep: Int = math.max(allAlgebraicLoopsInStep.valuesIterator.map(_.length).max, 1)

  def nAlgebraicLoopsInInit: Int = math.max(allAlgebraicLoopsInInit.length, 1)

  private def maxOr1(l: List[Int]): Int = if (l.isEmpty || l.forall(_ == 0)) 1 else l.max

  val loopOpsEncodingInit: Map[Int, AlgebraicLoopInit] = allAlgebraicLoopsInInit.zipWithIndex.map(_.swap).toMap
  val loopOpsEncodingInitInverse: Map[AlgebraicLoopInit, Int] = loopOpsEncodingInit.map(_.swap)

  val loopOpsEncoding: Map[String, Map[Int, AlgebraicLoop]] =
    allAlgebraicLoopsInStep.map(keyValue => (keyValue._1, keyValue._2.zipWithIndex.map(_.swap).toMap))
  val loopOpsEncodingInverse: Map[String, Map[AlgebraicLoop, Int]] = loopOpsEncoding.map(keyValue => (keyValue._1, keyValue._2.map(_.swap)))

  val maxNAlgebraicLoopOperationsInStep: Int = maxOr1(allAlgebraicLoopsInStep.valuesIterator.flatMap(i => i.map(_.iterate.length)).toList)
  val maxNAlgebraicLoopOperationsInInit: Int = maxOr1(allAlgebraicLoopsInInit.map(i => i.iterate.length))

  val maxStepOperations = model.cosimStep.valuesIterator.map(_.length).max

  private def fillDefault[A](value: List[String], max: Int, default: A, f: A => String): List[String] = {
    value ++ (0 until (max - value.length)).map(_ => f(default))
  }

  private def fillNoOps(value: List[String], max: Int): List[String] = {
    value ++ (0 until (max - value.length)).map(_ => encodeOperation(NoOP, ""))
  }

  private def fillNoPorts(value: List[String], max: Int): Seq[String] =
    value ++ (0 until (max - value.length)).map(_ => "{ noFMU, noPort}")

  def operationsPerAlgebraicLoopInInit: String = {
    (0 until nAlgebraicLoopsInInit)
      .map(idx => {
        val loopsInstructions: AlgebraicLoopInit = loopOpsEncodingInit.getOrElse(idx, AlgebraicLoopInit(Nil, Nil))
        fillNoOps(loopsInstructions.iterate.map(op => encodeOperation(op)), maxNAlgebraicLoopOperationsInInit)
          .mkString("{", ",", "}")
      })
      .mkString(",")
  }

  def getAlgebraicLoopInInit(getOperations: AlgebraicLoopInit => Int): String =
    (0 until nAlgebraicLoopsInInit)
      .map(idx => {
        getOperations(loopOpsEncodingInit.getOrElse(idx, AlgebraicLoopInit(Nil, Nil)))
      })
      .mkString(",")

  def getConvergencePorts(algebraicLoopInit: AlgebraicLoopInit): Int = algebraicLoopInit.untilConverged.size

  def getOperations(algebraicLoopInit: AlgebraicLoopInit): Int = algebraicLoopInit.iterate.size

  def getAConvergencePorts(algebraicLoop: AlgebraicLoop): Int = algebraicLoop.untilConverged.size

  def nConvergencePortsPerAlgebraicLoopInInit: String = getAlgebraicLoopInInit(getConvergencePorts)

  def nOperationsPerAlgebraicLoopInInit: String = getAlgebraicLoopInInit(getOperations)

  def nConvergencePortsPerAlgebraicLoopInStep: String = getNAlgebraicLoopOperations(getAConvergencePorts)

  def nOperationsPerAlgebraicLoopInStep: String = getNAlgebraicLoopOperations((getALoopOperations _).andThen(nOperations))

  def nRetryOperationsPerAlgebraicLoopInStep: String = getNAlgebraicLoopOperations((getARetryOperations _).andThen(nOperations))

  def retryOperationsPerAlgebraicLoopInStep: String =
    getAlgebraicLoopOp(getARetryOperations, encodeList, maxNRetryOperationsForAlgebraicLoopsInStep)

  def operationsPerAlgebraicLoopInStep: String = getAlgebraicLoopOp(getALoopOperations, encodeList, maxNAlgebraicLoopOperationsInStep)

  def getNAlgebraicLoopOperations(op: AlgebraicLoop => Int): String =
    (0 until nConfigs)
      .map(id => {
        val loopConfig = if (isAdaptive) loopOpsEncoding(configuration(id)._2) else loopOpsEncoding.values.head
        (0 until nAlgebraicLoopsInStep).map(idx => op(loopConfig.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil)))).mkString(",")
      })
      .mkString("{", "}, {", "}")

  def getAlgebraicLoopOp(
      op: AlgebraicLoop => List[CosimStepInstruction],
      f: (List[CosimStepInstruction], String) => List[String],
      nOperations: Int): String = {
    (0 until nConfigs)
      .map(id => {
        val configName = getConfigName(id)
        val loopConfig = if (isAdaptive) loopOpsEncoding(configuration(id)._2) else loopOpsEncoding.values.head
        (0 until nAlgebraicLoopsInStep)
          .map(idx => {
            val loopsInstructions: AlgebraicLoop = loopConfig.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil))
            fillNoOps(f(op(loopsInstructions), configName), nOperations).mkString("{", ",", "}")
          })
          .mkString(",")
      })
      .mkString("{", "}, {", "}")
  }

  def convergencePortsPerAlgebraicLoopInStep: String = {
    (0 until nConfigs)
      .map(id => {
        val loopConfig = if (isAdaptive) loopOpsEncoding(configuration(id)._2) else loopOpsEncoding.values.head
        (0 until nAlgebraicLoopsInStep)
          .map(idx => {
            val loopsInstructions: AlgebraicLoop = loopConfig.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil))
            fillNoPorts(
              loopsInstructions.untilConverged.map(p => s"""{ ${p.fmu}, ${fmuPortName(p.fmu, p.port)} }"""),
              maxNConvergeOperationsForAlgebraicLoopsInStep)
              .mkString("{", ",", "}")
          })
          .mkString(",")
      })
      .mkString("{", "}, {", "}")
  }

  def convergencePortsPerAlgebraicLoopInInit: String = {
    (0 until nAlgebraicLoopsInInit)
      .map(idx => {
        val loopsInstructions: AlgebraicLoopInit = loopOpsEncodingInit.getOrElse(idx, AlgebraicLoopInit(Nil, Nil))
        val encoded = fillNoPorts(
          loopsInstructions.untilConverged.map(p => s"""{ ${p.fmu}, ${fmuPortName(p.fmu, p.port)} }"""),
          maxNConvergeOperationsForAlgebraicLoopsInInit)
        s"""{ ${encoded.mkString(",")} }"""
      })
      .mkString(",")
  }

  val maxNRetryOperationsForAlgebraicLoopsInStep: Int = maxOr1(
    allAlgebraicLoopsInStep.valuesIterator.flatMap(i => i.map(_.ifRetryNeeded.length)).toList)
  val maxNConvergeOperationsForAlgebraicLoopsInStep: Int = maxOr1(
    allAlgebraicLoopsInStep.valuesIterator.flatMap(i => i.map(_.untilConverged.length)).toList)
  val maxNConvergeOperationsForAlgebraicLoopsInInit: Int = maxOr1(allAlgebraicLoopsInInit.map(i => i.untilConverged.length))

  val allStepFindingLoopsInStep: Map[String, List[StepLoop]] =
    model.cosimStep.map(keyValue =>
      (keyValue._1, ModelQuery.recFilter(keyValue._2, i => i.isInstanceOf[StepLoop]).map(_.asInstanceOf[StepLoop])))

  val maxFindStepOperations: Int = maxOr1(allStepFindingLoopsInStep.valuesIterator.flatMap(i => i.map(_.iterate.length)).toList)
  val maxFindStepRestoreOperations: Int = maxOr1(
    allStepFindingLoopsInStep.valuesIterator.flatMap(i => i.map(_.ifRetryNeeded.length)).toList)

  val nTerminationOperations: Int = model.terminate.length

  val nStepFinding: Int = {
    assert(allStepFindingLoopsInStep.valuesIterator.map(_.length).max <= 1, "More than one step finding loop is not supported yet.")
    allStepFindingLoopsInStep.valuesIterator.map(_.length).max
  }

  def nRestore: String = getStepLoop(getRetryOperations, length, 1.toString).mkString(",")

  def nFindStepOperations: String = getStepLoop(getLoopOperations, length, 1.toString).mkString(",")

  def findStepLoopOperations: String =
    getStepLoopOperations(getLoopOperations, encodeList, fillNoOps, noOpEncoding, maxFindStepOperations).mkString(",")

  def findStepLoopRestoreOperations: String =
    getStepLoopOperations(getRetryOperations, encodeList, fillNoOps, noOpEncoding, maxFindStepRestoreOperations).mkString(",")

  // TODO fix this
  def getStepLoop(
      getOperations: StepLoop => List[CosimStepInstruction],
      format: (List[CosimStepInstruction], String) => String,
      defaultValue: String): List[String] = {
    (0 until nConfigs)
      .map(id => {
        val configName = getConfigName(id)
        val configurationStepLoop =
          if (isAdaptive) allStepFindingLoopsInStep(configuration(id)._2) else allStepFindingLoopsInStep.values.head
        if (configurationStepLoop.isEmpty) defaultValue
        else configurationStepLoop.map(x => format(getOperations(x), configName)).mkString(",")
      })
      .toList
  }

  def getStepLoopOperations(
      getOperations: StepLoop => List[CosimStepInstruction],
      format: (List[CosimStepInstruction], String) => List[String],
      fillList: (List[String], Int) => List[String],
      defaultValue: String,
      nTimes: Int = 1): List[String] = {
    (0 until nConfigs)
      .map(id => {
        val configName = getConfigName(id)
        val configurationStepLoop =
          if (isAdaptive) allStepFindingLoopsInStep(configuration(id)._2) else allStepFindingLoopsInStep.values.head
        val list = if (configurationStepLoop.isEmpty) List(defaultValue) else format(getOperations(configurationStepLoop.head), configName)
        fillList(list, nTimes).mkString("{", ",", "}")
      })
      .toList
  }

  private def getConfigName(id: Int) = if (isAdaptive) configuration(id)._2 else loopOpsEncoding.keys.head

  def getARetryOperations(algebraicLoop: AlgebraicLoop): List[CosimStepInstruction] = algebraicLoop.ifRetryNeeded

  def getALoopOperations(algebraicLoop: AlgebraicLoop): List[CosimStepInstruction] = algebraicLoop.iterate

  def getRetryOperations(stepLoop: StepLoop): List[CosimStepInstruction] = stepLoop.ifRetryNeeded

  def getLoopOperations(stepLoop: StepLoop): List[CosimStepInstruction] = stepLoop.iterate

  def length(value: List[CosimStepInstruction], conf: String = ""): String = value.length.toString

  def encode(value: List[CosimStepInstruction], conf: String = ""): String = value.map(encodeOperation(_, conf)).mkString(",")

  def encodeList(value: List[CosimStepInstruction], conf: String = ""): List[String] = value.map(encodeOperation(_, conf))

  def nOperations(value: List[CosimStepInstruction]): Int = value.length

  def initializationOperations: String =
    model.initialization
      .map(op => encodeOperation(op))
      .mkString(",")

  def encodeOperation(op: InitializationInstruction): String = {
    op match {
      case AlgebraicLoopInit(i, y) =>
        s"""{noFMU, loop, noPort, noStep, noFMU, noCommitment, ${loopOpsEncodingInitInverse(AlgebraicLoopInit(i, y))}}"""
      case _ => op.toUppaal
    }
  }

  def instantiationOperations: String = model.instantiation.map(_.toUppaal).mkString(",")

  def stepOperations: String = {
    (0 until nConfigs)
      .map(id => {
        val configName = getConfigName(id)
        val cosimStep = if (isAdaptive) model.cosimStep(configuration(id)._2) else model.cosimStep.values.head
        fillNoOps(cosimStep.map(op => encodeOperation(op, configName)), maxStepOperations).mkString("{", ",", "}")
      })
      .mkString(",")
  }

  def encodeOperation(op: CosimStepInstruction, config: String = ""): String = {
    op match {
      case AlgebraicLoop(i, y, x) =>
        s"""{noFMU, loop, noPort, noStep, noFMU, noCommitment, ${loopOpsEncodingInverse(config)(AlgebraicLoop(i, y, x))}}"""
      case _ => op.toUppaal
    }
  }

  def terminationOperations: String = model.terminate.map(_.toUppaal).mkString(",")

  def variableArray(n: Int): String =
    (0 until n)
      .map(_ => "{defined,0}")
      .mkString(",")
}
