import org.scalatest._


class PluginTest extends FreeSpec {
  TestTools.LOGLVL = 2

  // Inline test cases (failure).

  "Compilation should fail" in {
    intercept[CompilationError.type] {
      TestTools.testCase("Test0",
"""
val a: `:RedWine` = iri"PeterMccoyChardonnay"
"""
      )
    }
  }

  // Inline test cases (success).

  "Compilation should succeed" in {
    TestTools.testCase("Test0",
"""
val a: `:WhiteWine` = iri"PeterMccoyChardonnay"
"""
    )
  }

  //// Run all "success" test cases.
  //TestTools.loadFolder("success").foreach { case (name, code) =>
  //  name in {
  //    TestTools.testCase(name, code)
  //  }
  //}

  //// Run all "failure" test cases.
  //TestTools.loadFolder("failure").foreach { case (name, code) =>
  //  name in {
  //    intercept[CompilationError.type] {
  //      TestTools.testCase(name, code)
  //    }
  //  }
  //}
}
