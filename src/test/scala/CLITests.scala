import org.intocps.verification.scenarioverifier.cli.VerifyTA
import org.intocps.verification.scenarioverifier.cli.Z3.Z3
import org.scalatest.flatspec._
import org.scalatest.matchers._

class CLITests extends AnyFlatSpec with should.Matchers {

  "VerifyTA" should "work" in {
    VerifyTA.isInstalled should be(true)
  }

  "Z3" should "work" in {
    Z3.isInstalled should be(true)
  }
}
