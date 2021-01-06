package core

object ModelQuery {

  def getAllInstructions(instructions: List[CosimStepInstruction]): List[CosimStepInstruction] = {
    instructions.flatMap(f => {
      f match {
        case AlgebraicLoop(uC, iterate, ifRetryNeeded) => List(f) ++ getAllInstructions(iterate) ++ getAllInstructions(ifRetryNeeded)
        case StepLoop(_, iterate, ifRetryNeeded) => List(f) ++ getAllInstructions(iterate) ++ getAllInstructions(ifRetryNeeded)
        case _ => List(f)
      }
    })
  }

  def getAllInitInstructions(instructions: List[InitializationInstruction]): List[InitializationInstruction] = {
    instructions.flatMap(f => {
      f match {
        case AlgebraicLoopInit(uC, iterate) => List(f) ++ getAllInitInstructions(iterate)
        case _ => List(f)
      }
    })
  }
  def recFilter(instructions: List[CosimStepInstruction], p: CosimStepInstruction => Boolean): List[CosimStepInstruction] = {
    getAllInstructions(instructions).filter(p)
  }

  def recInitFilter(instructions: List[InitializationInstruction], p: InitializationInstruction => Boolean): List[InitializationInstruction] = {
    getAllInitInstructions(instructions).filter(p)
  }
}
