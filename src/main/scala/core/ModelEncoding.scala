package core

class ModelEncoding(model: MasterModel) {

  val noOpEncoding = s"""{noFMU, noOp, noPort, noStep, noFMU, noCommitment, noLoop}"""

  def maxNInputs = model.scenario.fmus.map(f => f._2.inputs.size).max

  def maxNOutputs = model.scenario.fmus.map(f => f._2.outputs.size).max

  def nFMUs = model.scenario.fmus.size

  def Hmax: Int = model.scenario.maxPossibleStepSize

  def nInternal: Int = Math.max(model.scenario.fmus.map(f => nInternal(f._2)).sum[Int], 1)

  def nInternal(f: FmuModel): Int = f.outputs.map(o => o._2.dependencies.length).sum[Int]

  def nInternalInit: Int = Math.max(model.scenario.fmus.map(f => nInternalInit(f._2)).sum[Int], 1)

  def nInternalInit(f: FmuModel): Int = f.outputs.map(o => o._2.dependenciesInit.length).sum[Int]

  def nExternal: Int = model.scenario.connections.length

  def stepVariables: String = (0 until nFMUs).map(_ => "H_max").mkString(",")

  val fmuEncoding: Map[String, Int] = model.scenario.fmus.keys.zipWithIndex.toMap
  val fmuEncodingInverse = fmuEncoding.map(_.swap)

  def fmuNames = model.scenario.fmus.keys

  def fmuId(name: String) = fmuEncoding(name)

  def nInputs(f: String) = model.scenario.fmus(f).inputs.size

  def nOutputs(f: String) = model.scenario.fmus(f).outputs.size

  val fmuInputEncoding: Map[String, Map[String, Int]] = fmuNames.map(fName => (fName, model.scenario.fmus(fName).inputs.keys.zipWithIndex.toMap)).toMap
  val fmuInputEncodingInverse: Map[String, Map[Int, String]] = fmuInputEncoding.map(f => (f._1, f._2.map(_.swap)))
  val fmuOutputEncoding: Map[String, Map[String, Int]] = fmuNames.map(fName => (fName, model.scenario.fmus(fName).outputs.keys.zipWithIndex.toMap)).toMap

  def fmuInNames(f: String) = model.scenario.fmus(f).inputs.keys

  def fmuOutNames(f: String) = model.scenario.fmus(f).outputs.keys

  def fmuInputTypes(f: String) = {
    val inputs = model.scenario.fmus(f).inputs
    val inputEnc = fmuInputEncodingInverse(f)
    val array = (0 until inputs.size).map(idx => inputs(inputEnc(idx)).reactivity.toString) ++
      (inputs.size until maxNInputs).map(_ => "noPort")
    array.mkString(",")
  }

  def fmuPortName(f: String, p: String) = s"""${f}_${p}"""

  def connections = model.scenario.connections

  val connectionEncoding: Map[ConnectionModel, Int] = connections.zipWithIndex.toMap
  val connectionEncodingInverse: Map[Int, ConnectionModel] = connectionEncoding.map(_.swap)

  def connectionName(c: ConnectionModel) = s"${c.srcPort.fmu}_${c.srcPort.port}__${c.trgPort.fmu}_${c.trgPort.port}"

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

  def feedthroughInStep: String = {
    val feedthrough = model.scenario.fmus
      .flatMap(f => f._2.outputs
        .flatMap(o => o._2.dependencies
          .map(d => s"{${f._1}, ${fmuPortName(f._1, d)}, ${fmuPortName(f._1, o._1)}}"))).mkString(",")
    if (!feedthrough.isEmpty)
      return feedthrough
    else
      return "{noFMU, noPort, noPort}"

  }

  def feedthroughInInit: String = {
    val feedthrough = model.scenario.fmus
      .flatMap(f => f._2.outputs
        .flatMap(o => o._2.dependenciesInit
          .map(d => s"{${f._1}, ${fmuPortName(f._1, d)}, ${fmuPortName(f._1, o._1)}}")))
      .mkString(",")
    if (!feedthrough.isEmpty)
      return feedthrough
    else
      return "{noFMU, noPort, noPort}"
  }

  def mayRejectStep: String = (0 until nFMUs)
    .map(fmuId => model.scenario.fmus(fmuEncodingInverse(fmuId)).canRejectStep)
    .mkString(",")

  def nInstantiationOperations: Int = model.instantiation.length

  def nInitializationOperations: Int = model.initialization.length

  def nStepOperations: Int = model.cosimStep.length

  val allAlgebraicLoopsInStep: List[AlgebraicLoop] =
    ModelQuery.recFilter(model.cosimStep, i => i.isInstanceOf[AlgebraicLoop]).map(_.asInstanceOf[AlgebraicLoop])

  val allAlgebraicLoopsInInit: List[AlgebraicLoopInit] =
    ModelQuery.recInitFilter(model.initialization, i => i.isInstanceOf[AlgebraicLoopInit]).map(_.asInstanceOf[AlgebraicLoopInit])

  def nAlgebraicLoopsInStep: Int = math.max(allAlgebraicLoopsInStep.length, 1)

  def nAlgebraicLoopsInInit: Int = math.max(allAlgebraicLoopsInInit.length, 1)

  def maxOr1(l: List[Int]): Int = {
    if (l.isEmpty) {
      1
    } else {
      l.max
    }
  }

  val loopOpsEncodingInit: Map[Int, AlgebraicLoopInit] = allAlgebraicLoopsInInit.zipWithIndex.map(_.swap).toMap
  val loopOpsEncodingInitInverse: Map[AlgebraicLoopInit, Int] = loopOpsEncodingInit.map(_.swap).toMap

  val loopOpsEncoding: Map[Int, AlgebraicLoop] = allAlgebraicLoopsInStep.zipWithIndex.map(_.swap).toMap
  val loopOpsEncodingInverse: Map[AlgebraicLoop, Int] = loopOpsEncoding.map(_.swap)

  val maxNAlgebraicLoopOperationsInStep = maxOr1(allAlgebraicLoopsInStep.map(i => i.iterate.length))
  val maxNAlgebraicLoopOperationsInInit = maxOr1(allAlgebraicLoopsInInit.map(i => i.iterate.length))

  def fillNoOps(value: List[String], max: Int): List[String] = value ++ (0 until (max - value.length)).map(_ => encodeOperation(NoOP))


  def operationsPerAlgebraicLoopInStep: String = {
    (0 until nAlgebraicLoopsInStep).map(idx => {
      val loopsInstructions: AlgebraicLoop = loopOpsEncoding.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil))
      val encoded = fillNoOps(loopsInstructions.iterate.map(op => encodeOperation(op)), maxNAlgebraicLoopOperationsInStep)
      s"""{ ${encoded.mkString(",")} }"""
    }).mkString(",")
  }

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
    (0 until nAlgebraicLoopsInStep).map(idx => {
      loopOpsEncoding.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil)).untilConverged.size
    }).mkString(",")
  }

  def nOperationsPerAlgebraicLoopInStep: String = {
    (0 until nAlgebraicLoopsInStep).map(idx => {
      loopOpsEncoding.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil)).iterate.size
    }).mkString(",")
  }

  def nRetryOperationsPerAlgebraicLoopInStep: String = {
    (0 until nAlgebraicLoopsInStep).map(idx => {
      loopOpsEncoding.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil)).ifRetryNeeded.size
    }).mkString(",")
  }

  def retryOperationsPerAlgebraicLoopInStep: String = {
    (0 until nAlgebraicLoopsInStep).map(idx => {
      val loopsInstructions: AlgebraicLoop = loopOpsEncoding.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil))
      val encoded = fillNoOps(loopsInstructions.ifRetryNeeded.map(op => encodeOperation(op)), maxNRetryOperationsForAlgebraicLoopsInStep)
      s"""{ ${encoded.mkString(",")} }"""
    }).mkString(",")
  }

  def fillNoPorts(value: List[String], max: Int) = value ++ (0 until (max - value.length)).map(_ => s"""{ noFMU, noPort}""")

  def convergencePortsPerAlgebraicLoopInStep: String = {
    (0 until nAlgebraicLoopsInStep).map(idx => {
      val loopsInstructions: AlgebraicLoop = loopOpsEncoding.getOrElse(idx, AlgebraicLoop(Nil, Nil, Nil))
      val encoded = fillNoPorts(loopsInstructions.untilConverged.map(p => s"""{ ${p.fmu}, ${fmuPortName(p.fmu, p.port)} }"""), maxNConvergeOperationsForAlgebraicLoopsInStep)
      s"""{ ${encoded.mkString(",")} }"""
    }).mkString(",")
  }

  def convergencePortsPerAlgebraicLoopInInit: String = {
    (0 until nAlgebraicLoopsInInit).map(idx => {
      val loopsInstructions: AlgebraicLoopInit = loopOpsEncodingInit.getOrElse(idx, AlgebraicLoopInit(Nil, Nil))
      val encoded = fillNoPorts(loopsInstructions.untilConverged.map(p => s"""{ ${p.fmu}, ${fmuPortName(p.fmu, p.port)} }"""), maxNConvergeOperationsForAlgebraicLoopsInInit)
      s"""{ ${encoded.mkString(",")} }"""
    }).mkString(",")
  }

  val maxNRetryOperationsForAlgebraicLoopsInStep = maxOr1(allAlgebraicLoopsInStep.map(i => i.ifRetryNeeded.length))
  val maxNConvergeOperationsForAlgebraicLoopsInStep = maxOr1(allAlgebraicLoopsInStep.map(i => i.untilConverged.length))
  val maxNConvergeOperationsForAlgebraicLoopsInInit = maxOr1(allAlgebraicLoopsInInit.map(i => i.untilConverged.length))

  val allStepFindingLoopsInStep: List[StepLoop] =
    ModelQuery.recFilter(model.cosimStep, i => i.isInstanceOf[StepLoop]).map(_.asInstanceOf[StepLoop])

  def nFindStepOperations: Int = {
    assert(allStepFindingLoopsInStep.size <= 1, "More than one step finding loop is not supported yet.")
    if (allStepFindingLoopsInStep.isEmpty) {
      1
    } else {
      allStepFindingLoopsInStep.head.iterate.length
    }
  }

  val nTerminationOperations = model.terminate.length

  def nRestore = {
    assert(allStepFindingLoopsInStep.size <= 1, "More than one step finding loop is not supported yet.")
    if (allStepFindingLoopsInStep.isEmpty) {
      1
    } else {
      allStepFindingLoopsInStep.head.ifRetryNeeded.length
    }
  }

  def findStepLoopOperations: String = {
    if (allStepFindingLoopsInStep.isEmpty) {
      noOpEncoding
    } else {
      allStepFindingLoopsInStep.head.iterate.map(op => encodeOperation(op)).mkString(",")
    }
  }

  def findStepLoopRestoreOperations: String = {
    if (allStepFindingLoopsInStep.isEmpty) {
      noOpEncoding
    } else {
      assert(allStepFindingLoopsInStep.size == 1, "More than one step finding loop is not supported yet.")
      allStepFindingLoopsInStep.head.ifRetryNeeded.map(op => encodeOperation(op)).mkString(",")
    }
  }

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

  def stepOperations: String = model.cosimStep
    .map(op => encodeOperation(op))
    .mkString(",")

  def encodeOperation(op: CosimStepInstruction): String = {
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
      case AlgebraicLoop(i, y, x) => s"""{noFMU, loop, noPort, noStep, noFMU, noCommitment, ${loopOpsEncodingInverse(AlgebraicLoop(i, y, x))}}"""
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
