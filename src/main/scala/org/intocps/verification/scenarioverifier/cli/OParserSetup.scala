package org.intocps.verification.scenarioverifier.cli

import scopt.DefaultOParserSetup

case class OParserSetup() extends DefaultOParserSetup {
  override def showUsageOnError = Some(true)
}
