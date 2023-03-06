import cli.VerifyTA
import org.scalatest.flatspec._
import org.scalatest.matchers._

class CLITests extends AnyFlatSpec with should.Matchers {

  "VerifyTA" should "work" in {
    VerifyTA.checkEnvironment() should be (true)
  }

}
