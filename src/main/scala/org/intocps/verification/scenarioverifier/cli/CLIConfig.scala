package org.intocps.verification.scenarioverifier.cli

final case class CLIConfig(
    master: String = "examples/simple_master.conf",
    verify: Boolean = false,
    trace: Boolean = false,
    generateAlgorithm: Boolean = false)
