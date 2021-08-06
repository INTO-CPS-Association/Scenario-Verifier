package core

import org.apache.logging.log4j.scala.Logging

class ModelEncoding(model: MasterModel) extends Logging {

  val noOpEncoding = "{noFMU, noOp, noPort, noStep, noFMU, noCommitment, noLoop}"

  def maxNInputs = model.scenario.fmus.map(f => f._2.inputs.size).max

  def maxNOutputs = model.scenario.fmus.map(f => f._2.outputs.size).max

  def nFMUs = model.scenario.fmus.size

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

  val fmuInputEncoding: Map[String, Map[String, Int]] = fmuNames.map(fName => (fName, model.scenario.fmus(fName).inputs.keys.zipWithIndex.toMap)).toMap
  val fmuInputEncodingInverse: Map[String, Map[Int, String]] = fmuInputEncoding.map(f => (f._1, f._2.map(_.swap)))

  val fmuOutputEncoding: Map[String, Map[String, Int]] = fmuNames.map(fName => (fName, model.scenario.fmus(fName).outputs.keys.zipWithIndex.toMap)).toMap
  val fmuOutputEncodingInverse: Map[String, Map[Int, String]] = fmuOutputEncoding.map(f => (f._1, f._2.map(_.swap)))

  val isAdaptive: Boolean = model.scenario.config.configurableInputs.nonEmpty

  def fmuInNames(f: String) = model.scenario.fmus(f).inputs.keys

  def fmuOutNames(f: String) = model.scenario.fmus(f).outputs.keys

  def fmuPortName(f: String, p: String) = s"""${f}_${p}"""

  def connections = model.scenario.connections

  val connectionEncoding: Map[ConnectionModel, Int] = connections.zipWithIndex.toMap
  val connectionEncodingInverse: Map[Int, ConnectionModel] = connectionEncoding.map(_.swap)

  def connectionName(c: ConnectionModel) = s"${c.srcPort.fmu}_${c.srcPort.port}__${c.trgPort.fmu}_${c.trgPort.port}"

  val configurationStep: Map[Int, (String, String)] = model.scenario.config.configurations.zipWithIndex.map(_.swap).toMap.map(i => (i._1, (i._2._1, i._2._2.cosimStep)))


  def fmuInputTypes(f: String) = {
    val inputs = model.scenario.fmus(f).inputs
    val inputEnc = fmuInputEncodingInverse(f)
    (0 until nConfigs).map(id => {
      val confName = configurationStep(id)._1
      val inputPortConfig = model.scenario.config.configurations(confName).inputs.filter(_._1.fmu == f)
      val array = (0 until inputs.size).map(idx =>
        if (inputPortConfig.keys.toList.contains(PortRef(f, inputEnc(idx))))
          inputPortConfig(PortRef(f, inputEnc(idx))).reactivity.toString
        else
          inputs(inputEnc(idx)).reactivity.toString
      ) ++ (inputs.size until maxNInputs).map(_ => "noPort")
      array.mkString(",")
    }).mkString("{", "}, {", "}")
  }

  def connectionVariable: String = {
    (0 until nFMUs).map(_ =>
      (0 until maxNOutputs)
        .map(_ => """{{undefined,0}, {undefined,0}}""")
        .mkString("{", ",", "}")).mkString(",")
  }

  def external: String = (0 until nExternal)
    .map(idx => connectionEncodingInverse(idx))
    .map(c => f"""{${c.srcPort.fmu}, ${fmuPortName(c.srcPort.fmu, c.srcPort.port)}, ${c.trgPort.fmu}, ${fmuPortName(c.trgPort.fmu, c.trgPort.port)}}""")
    .mkString(",")

val noFeedThrough : String = "{noFMU, noPort, noPort}"

  def feedthroughInStep: String = {
    (0 until nConfigs).map(_ => {
      val feedthrough = model.scenario.fmus
        .flatMap(f => f._2.outputs
          .flatMap(o => o._2.dependencies
            .map(d => feedThroughString(f, o, d)))).mkString(",")
      if (!feedthrough.isEmpty)
        feedthrough
      else {
        noFeedThrough
      }
    }).mkString("{", "}, {", "}")
  }

  private def feedThroughString(f: (String, FmuModel), o: (String, OutputPortModel), d: String) = {
    s"{${f._1}, ${fmuPortName(f._1, d)}, ${fmuPortName(f._1, o._1)}}"
  }

  def feedthroughInInit: String = {
    val feedthrough = model.scenario.fmus
      .flatMap(f => f._2.outputs
        .flatMap(o => o._2.dependenciesInit
          .map(d => feedThroughString(f, o, d))))
      .mkString(",")
    if (!feedthrough.isEmpty)
      feedthrough
    else
      noFeedThrough
  }

  def mayRejectStep: String = (0 until nFMUs)
    .map(fmuId => model.scenario.fmus(fmuEncodingInverse(fmuId)).canRejectStep)
    .mkString(",")

  def nInstantiationOperations: Int = model.instantiation.length

  def nInitializationOperations: Int = model.initialization.length

  def nStepOperations: String = model.cosimStep.valuesIterator.map(_.length).mkString(",")

  val allAlgebraicLoopsInStep: Map[String, List[AlgebraicLoop]] = model.cosimStep.map(keyValue => {
    (keyValue._1, ModelQuery.recFilter(keyValue._2, i => i.isInstanceOf[AlgebraicLoop]).map(_.asInstanceOf[AlgebraicLoop]))
  })

  val allAlgebraicLoopsInInit: List[AlgebraicLoopInit] =
    ModelQuery.recInitFilter(model.initialization, i => i.isInstanceOf[AlgebraicLoopInit]).map(_.asInstanceOf[AlgebraicLoopInit])

  def nAlgebraicLoopsInStep: Int = math.max(allAlgebraicLoopsInStep.valuesIterator.map(_.length).max, 1)

  def nAlgebraicLoopsInInit: Int = math.max(allAlgebraicLoopsInInit.length, 1)

  def maxOr1(l: List[Int]): Int = if (l.isEmpty || l.forall(_ == 0)) 1 else l.max

  val loopOpsEncodingInit: Map[Int, AlgebraicLoopInit] = allAlgebraicLoopsInInit.zipWithIndex.map(_.swap).toMap
  val loopOpsEncodingInitInverse: Map[AlgebraicLoopInit, Int] = loopOpsEncodingInit.map(_.swap).toMap

  val loopOpsEncoding: Map[String, Map[Int, AlgebraicLoop]] = allAlgebraicLoopsInStep.map(keyValue => (keyValue._1, keyValue._2.zipWithIndex.map(_.swap).toMap))
  val loopOpsEncodingInverse: Map[String, Map[AlgebraicLoop, Int]] = loopOpsEncoding.map(keyValue => (keyValue._1, keyValue._2.map(_.swap)))

  val maxNAlgebraicLoopOperationsInStep = maxOr1(allAlgebraicLoopsInStep.valuesIterator.flatMap(i => i.map(_.iterate.length)).toList)
  val maxNAlgebraicLoopOperationsInInit = maxOr1(allAlgebraicLoopsInInit.map(i => i.iterate.length))

  val maxStepOperations = model.cosimStep.valuesIterator.map(_.length).max

  def fillNoOps(value: List[String], max: Int): List[String] = value ++ (0 until (max - value.length)).map(_ => encodeOperation(NoOP, ""))
  def fillNoPorts(value: List[String], max: Int) = value ++ (0 until (max - value.length)).map(_ => s"""{ noFMU, noPort}""")


  def operationsPerAlgebraicLoopInInit: String = {
    (0 until nAlgebraicLoopsInInit).map(idx => {
      val loopsInstructions: AlgebraicLoopInit = loopOpsEncodingInit.getOrElse(idx, AlgebraicLoopInit(Nil, Nil))
      val encoded = fillNoOps(loopsInstructions.iterate.map(op => encodeOperation(op)), maxNAlgebraicLoopOperationsInInit)
      s"""{ ${encoded.mkString(",")} }"""
    }).mkString(",")
  }

  def nConvergencePortsPerAlgebraicLoopInInit: String = {
    (0 until nAlgebraicLoopsInInit).map(idx => {
      loopOpsEncodingInit.getOrElse(idx, AlgebraicLoopInit(Nil, Nil)).untilConverged.size
    }).mkString(",")
  }

  def nOperationsPerAlgebraicLoopInInit: String = {
    (0 until nAlgebraicLoopsInInit).map(idx => {
      loopOpsEncodingInit.getOrElse(idx, AlgebraicLoopInit(Nil, Nil)).iterate.size
    }).mkString(",")
  }

  def nConvergencePortsPerAlgebraicLoopInStep: String = {
    (0 until nConfigs).map(id => {
      val conf = configurationStep(id)
      val loopConfig = loopOpsEncoding(conf._2)
      (0 until nAlgebraicLoopsInStep).map(idx => {
        loopConfig.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil)).untilConverged.size
      }).mkString(",")
    }
    ).mkString("{", "}, {", "}")
  }

  def nOperationsPerAlgebraicLoopInStep: String = getAlgebraicLoop(getLoopOperations, length).mkString("{", "}, {", "}")
  def nRetryOperationsPerAlgebraicLoopInStep: String = getAlgebraicLoop(getRetryOperations, length).mkString("{", "}, {", "}")

  def retryOperationsPerAlgebraicLoopInStep: String = getAlgebraicLoopOp(getRetryOperations, encodeList, maxNRetryOperationsForAlgebraicLoopsInStep)
  def operationsPerAlgebraicLoopInStep: String = getAlgebraicLoopOp(getLoopOperations, encodeList, maxNAlgebraicLoopOperationsInStep)

  def getAlgebraicLoop(op: AlgebraicLoop => List[CosimStepInstruction], f: (List[CosimStepInstruction], String) => String): List[String] = {
    (0 until nConfigs).map(id => {
      val conf = configurationStep(id)
      val configName = if (isAdaptive) conf._1 else ""
      val loopConfig = if (isAdaptive) loopOpsEncoding(conf._2) else loopOpsEncoding.values.head
      (0 until nAlgebraicLoopsInStep).map(idx => {
        val loopsInstructions: AlgebraicLoop = loopConfig.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil))
        f(op(loopsInstructions), configName).mkString
      }).mkString(",")
    }).toList
  }

  def getAlgebraicLoopOp(op: AlgebraicLoop => List[CosimStepInstruction], f: (List[CosimStepInstruction], String) => List[String], nOperations: Int): String = {
    (0 until nConfigs).map(id => {
      val conf = configurationStep(id)
      val configName = if (isAdaptive) conf._1 else ""
      val loopConfig = if (isAdaptive) loopOpsEncoding(conf._2) else loopOpsEncoding.values.head
      (0 until nAlgebraicLoopsInStep).map(idx => {
        val loopsInstructions: AlgebraicLoop = loopConfig.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil))
        fillNoOps(f(op(loopsInstructions), configName), nOperations).mkString("{", ",", "}")
      }).mkString(",")
    }).mkString("{", "}, {", "}")
  }

  def convergencePortsPerAlgebraicLoopInStep: String = {
    (0 until nConfigs).map(id => {
      val conf = configurationStep(id)
      val loopConfig = loopOpsEncoding(conf._2)
      (0 until nAlgebraicLoopsInStep).map(idx => {
        val loopsInstructions: AlgebraicLoop = loopConfig.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil))
        val encoded = fillNoPorts(loopsInstructions.untilConverged.map(p => s"""{ ${p.fmu}, ${fmuPortName(p.fmu, p.port)} }"""), maxNConvergeOperationsForAlgebraicLoopsInStep)
        s"""{ ${encoded.mkString(",")} }"""
      }).mkString(",")
    }).mkString("{", "}, {", "}")
  }

  def convergencePortsPerAlgebraicLoopInInit: String = {
    (0 until nAlgebraicLoopsInInit).map(idx => {
      val loopsInstructions: AlgebraicLoopInit = loopOpsEncodingInit.getOrElse(idx, AlgebraicLoopInit(Nil, Nil))
      val encoded = fillNoPorts(loopsInstructions.untilConverged.map(p => s"""{ ${p.fmu}, ${fmuPortName(p.fmu, p.port)} }"""), maxNConvergeOperationsForAlgebraicLoopsInInit)
      s"""{ ${encoded.mkString(",")} }"""
    }).mkString(",")
  }

  val maxNRetryOperationsForAlgebraicLoopsInStep = maxOr1(allAlgebraicLoopsInStep.valuesIterator.flatMap(i => i.map(_.ifRetryNeeded.length)).toList)
  val maxNConvergeOperationsForAlgebraicLoopsInStep = maxOr1(allAlgebraicLoopsInStep.valuesIterator.flatMap(i => i.map(_.untilConverged.length)).toList)
  val maxNConvergeOperationsForAlgebraicLoopsInInit = maxOr1(allAlgebraicLoopsInInit.map(i => i.untilConverged.length))

  val allStepFindingLoopsInStep: Map[String, List[StepLoop]] = model.cosimStep.map(keyValue =>
    (keyValue._1, ModelQuery.recFilter(keyValue._2, i => i.isInstanceOf[StepLoop]).map(_.asInstanceOf[StepLoop])))


  val maxFindStepOperations = maxOr1(allStepFindingLoopsInStep.valuesIterator.flatMap(i => i.map(_.iterate.length)).toList)
  val maxFindStepRestoreOperations = maxOr1(allStepFindingLoopsInStep.valuesIterator.flatMap(i => i.map(_.ifRetryNeeded.length)).toList)


  val nTerminationOperations = model.terminate.length

  val nStepFinding: Int = {
    assert(allStepFindingLoopsInStep.valuesIterator.map(_.length).max <= 1, "More than one step finding loop is not supported yet.")
    allStepFindingLoopsInStep.valuesIterator.map(_.length).max
  }

  def nRestore: String = getStepLoop(getRetryOperations, length, 1.toString).mkString(",")
  def nFindStepOperations: String = getStepLoop(getLoopOperations, length, 1.toString).mkString(",")
  def findStepLoopOperations: String = getStepLoop(getLoopOperations, encode, noOpEncoding).mkString("{", "}, {", "}")
  def findStepLoopRestoreOperations: String = getStepLoop(getRetryOperations, encode, noOpEncoding).mkString("{", "}, {", "}")

  def getStepLoop(op: StepLoop => List[CosimStepInstruction], f: (List[CosimStepInstruction], String) => String, defaultValue: String): List[String] = {
    (0 until nConfigs).map(id => {
      val conf = configurationStep(id)
      val configName = if (isAdaptive) conf._1 else ""
      val configurationStepLoop = if (isAdaptive) allStepFindingLoopsInStep(conf._2) else allStepFindingLoopsInStep.values.head
      if (configurationStepLoop.isEmpty) defaultValue else configurationStepLoop.map(x => f(op(x), configName)).mkString
    }).toList
  }

  def getRetryOperations(algebraicLoop: AlgebraicLoop): List[CosimStepInstruction] = algebraicLoop.ifRetryNeeded
  def getLoopOperations(algebraicLoop: AlgebraicLoop): List[CosimStepInstruction] = algebraicLoop.iterate
  def getRetryOperations(stepLoop: StepLoop): List[CosimStepInstruction] = stepLoop.ifRetryNeeded
  def getLoopOperations(stepLoop: StepLoop): List[CosimStepInstruction] = stepLoop.iterate
  def length(value: List[CosimStepInstruction], conf: String = ""): String = value.length.toString
  def encode(value: List[CosimStepInstruction], conf: String = ""): String = value.map(encodeOperation(_, conf)).mkString(",")
  def encodeList(value: List[CosimStepInstruction], conf: String = ""): List[String] = value.map(encodeOperation(_, conf))

  def initializationOperations: String = model.initialization
    .map(op => encodeOperation(op))
    .mkString(",")

  def encodeOperation(op: InitializationInstruction): String = {
    op match {
      case InitSet(PortRef(fmu, port)) => s"""{${fmu}, set, ${fmuPortName(fmu, port)}, noStep, noFMU, final, noLoop}"""
      case InitGet(PortRef(fmu, port)) => s"""{${fmu}, get, ${fmuPortName(fmu, port)}, noStep, noFMU, final, noLoop}"""
      case AlgebraicLoopInit(i, y) => s"""{noFMU, loop, noPort, noStep, noFMU, noCommitment, ${loopOpsEncodingInitInverse(AlgebraicLoopInit(i, y))}}"""
      case EnterInitMode(fmu) => s"""{${fmu}, enterInitialization, noPort, noStep, noFMU, noCommitment, noLoop}"""
      case ExitInitMode(fmu) => s"""{${fmu}, exitInitialization, noPort, noStep, noFMU, noCommitment, noLoop}"""
    }
  }

  def instantiationOperations: String = model.instantiation
    .map(op => encodeOperation(op))
    .mkString(",")

  def encodeOperation(op: InstantiationInstruction): String = {
    op match {
      case Instantiate(fmu) => s"""{${fmu}, instantiate, noPort, noStep, noFMU, noCommitment, noLoop}"""
      case SetupExperiment(fmu) => s"""{${fmu}, setupExperiment, noPort, noStep, noFMU, noCommitment, noLoop}"""
    }
  }

  def stepOperations: String = {
    (0 until nConfigs).map(id => {
      val conf = configurationStep(id)
      model.cosimStep(conf._2).map(op => encodeOperation(op, conf._1)).mkString(",")
    }).mkString("{", "}, {", "}")
  }

  def encodeOperation(op: CosimStepInstruction, config: String = ""): String = {
    op match {
      case Set(PortRef(fmu, port)) => s"""{${fmu}, set, ${fmuPortName(fmu, port)}, noStep, noFMU, final, noLoop}"""
      case SetTentative(PortRef(fmu, port)) => s"""{${fmu}, set, ${fmuPortName(fmu, port)}, noStep, noFMU, tentative, noLoop}"""
      case Get(PortRef(fmu, port)) => s"""{${fmu}, get, ${fmuPortName(fmu, port)}, noStep, noFMU, final, noLoop}"""
      case GetTentative(PortRef(fmu, port)) => s"""{${fmu}, get, ${fmuPortName(fmu, port)}, noStep, noFMU, tentative, noLoop}"""
      case Step(fmu, by) => by match {
        case DefaultStepSize() => s"""{${fmu}, step, noPort, H, noFMU, noCommitment, noLoop}"""
        case RelativeStepSize(fmu_step) => s"""{${fmu}, step, noPort, noStep, ${fmu_step}, noCommitment, noLoop}"""
        case AbsoluteStepSize(step_size) => s"""{${fmu}, step, noPort, $step_size, noFMU, noCommitment, noLoop}"""
      }
      case SaveState(fmu) => s"""{${fmu}, save, noPort, noStep, noFMU, noCommitment, noLoop}"""
      case RestoreState(fmu) => s"""{${fmu}, restore, noPort, noStep, noFMU, noCommitment, noLoop}"""
      case AlgebraicLoop(i, y, x) => s"""{noFMU, loop, noPort, noStep, noFMU, noCommitment, ${loopOpsEncodingInverse(config)(AlgebraicLoop(i, y, x))}}"""
      case StepLoop(_, _, _) => s"""{noFMU, findStep, noPort, noStep, noFMU, noCommitment, noLoop}"""
      case NoOP => noOpEncoding
    }
  }

  def terminationOperations: String = model.terminate
    .map(op => encodeOperation(op))
    .mkString(",")

  def encodeOperation(op: TerminationInstruction): String = {
    op match {
      case Terminate(fmu) => s"""{${fmu}, terminate, noPort, noStep, noFMU, noCommitment, noLoop}"""
      case FreeInstance(fmu) => s"""{${fmu}, freeInstance, noPort, noStep, noFMU, noCommitment, noLoop}"""
      case Unload(fmu) => s"""{${fmu}, unload, noPort, noStep, noFMU, noCommitment, noLoop}"""
    }
  }

  def variableArray(n: Int): String = (0 until n)
    .map(_ => "{defined,0}")
    .mkString(",")
}
