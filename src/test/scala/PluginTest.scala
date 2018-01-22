import org.scalatest._


class PluginTest extends FreeSpec {
  TestTools.LOGLVL = 2

  // Run all "success" test cases.
  TestTools.loadFolder("success").foreach { case (name, code) =>
    name in {
      TestTools.testCase(name, code)
    }
  }

  // Run all "failure" test cases.
  TestTools.loadFolder("failure").foreach { case (name, code) =>
    name in {
      intercept[CompilationError.type] {
        TestTools.testCase(name, code)
      }
    }
  }
}
