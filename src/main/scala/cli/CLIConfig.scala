package cli

import java.nio.file.Files

case class CLIConfig(
                      master: String = "examples/simple_master.conf",
                      output: String = Files.createTempFile("uppaal_", ".xml").toString,
                      verify: Boolean = false,
                      trace: Boolean = false,
                      generateAlgorithm : Boolean= false,
)
