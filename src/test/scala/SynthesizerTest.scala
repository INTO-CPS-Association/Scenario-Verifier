import api.VerificationAPI
import core.ScenarioLoader
import org.scalatest.Assertion
import org.scalatest.flatspec._
import org.scalatest.matchers._

class SynthesizerTest extends AnyFlatSpec with should.Matchers {
  private def synthesizeAndVerify(resourcesFile: String): Assertion = {
    val conf = getClass.getResourceAsStream(resourcesFile)
    val masterModel = ScenarioLoader.load(conf)
    assert(VerificationAPI.synthesizeAndVerify(masterModel.name, masterModel.scenario))
  }

  "Synthesizer" should "create valid Master Algorithm for Simple Master" in {
    synthesizeAndVerify("examples/simple_master.conf")
  }

  "Synthesizer" should "create valid Master Algorithm for Master with inferred Ports" in {
    synthesizeAndVerify("examples_no_algorithm/eduard.conf")
  }

  "Synthesizer" should "create valid Master Algorithm for Tricky Scenario" in {
    synthesizeAndVerify("examples/tapas.conf")
  }

  "Synthesizer" should "create valid Master Algorithm for Spanish Scenario" in {
    synthesizeAndVerify("examples/scenario_spaniard.conf")
  }

"Synthesizer" should "create valid Master Algorithm for Simple Adaptive Master" in {
  synthesizeAndVerify("examples/simple_master_adaptive.conf")
}
"Synthesizer" should "create valid Master Algorithm for Step finding Adaptive Master" in {
  synthesizeAndVerify("examples_no_algorithm/step_finding_loop_adaptive.conf")
}

"Synthesizer" should "create valid Master Algorithm for Complex Adaptive Master" in {
  synthesizeAndVerify("examples_no_algorithm/complex_master_adaptive.conf")
}

  "Synthesizer" should "create valid Master Algorithm for Amesim" in {
    synthesizeAndVerify("examples/msd_from_amesim.conf")
  }

"Synthesizer" should "create valid Master Algorithm for Algebraic Initialization" in {
  synthesizeAndVerify("examples/algebraic_loop_initialization.conf")
  //synthesizeOptAndVerify("examples/algebraic_loop_initialization.conf")
}



  "Synthesizer" should "create valid Master Algorithm for Industrial case study" in {
    synthesizeAndVerify("examples/industrial_casestudy.conf")
    //synthesizeOptAndVerify("examples/industrial_casestudy.conf")
  }

  "Synthesizer" should "create valid Step procedure for Two Algebraic Loops" in {
    synthesizeAndVerify("examples/two_algebraic_loops.conf")
    //synthesizeOptAndVerify("examples/two_algebraic_loops.conf")
  }

  "Synthesizer" should "create valid Jacboian Step procedure for Algebraic Loop" in {
    synthesizeAndVerify("examples/algebraic_loop_msd_jac.conf")
    //synthesizeOptAndVerify("examples/algebraic_loop_msd_jac.conf")
  }

  "Synthesizer" should "create valid Gauss Seidel Step procedure for Algebraic Loop" in {
    synthesizeAndVerify("examples/algebraic_loop_msd_gs.conf")
    //synthesizeOptAndVerify("examples/algebraic_loop_msd_gs.conf", minimum)
  }

  "Synthesizer" should "create valid Step Finding procedure for Step Loop" in {
    synthesizeAndVerify("examples/step_finding_loop_msd_1.conf")
  }


  "Synthesizer" should "create a valid Step Finding And Algebraic procedure for Combined Step and Jac Algebraic" in {
    synthesizeAndVerify("examples/loop_within_loop.conf")
  }

  "Synthesizer" should "create valid Step Finding procedure for Step Loop only delayed ports" in {
    synthesizeAndVerify("examples_no_algorithm/step_finding_loop_two_delayed.conf")
  }

  "Synthesizer" should "create valid Step Finding procedure for Step Loop three delayed FMUs" in {
    synthesizeAndVerify("examples_no_algorithm/step_finding_loop_three_delayed.conf")
  }

  "Synthesizer" should "create valid Feedthrough Loop" in {
    synthesizeAndVerify("examples_no_algorithm/algebraic_loop_feedthrough.conf")
  }
}