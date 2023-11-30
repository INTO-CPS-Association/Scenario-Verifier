package org.intocps.verification.scenarioverifier.api

import org.intocps.verification.scenarioverifier.core.CosimStepInstruction

final case class Verdict(correct: Boolean, possibleActions: Set[CosimStepInstruction]) {
  //require(correct || possibleActions.nonEmpty, "If the verdict is incorrect, there must be at least one possible action")
}
