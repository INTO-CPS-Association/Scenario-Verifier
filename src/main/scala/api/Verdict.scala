package api

import core.CosimStepInstruction

final case class Verdict(correct: Boolean, possibleActions: Set[CosimStepInstruction]) {
  require(correct || possibleActions.nonEmpty, "If the verdict is incorrect, there must be at least one possible action")
}
