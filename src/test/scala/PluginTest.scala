import org.scalatest._


class PluginTest extends FreeSpec {
  import TestTools._
  LOGLVL = 2

  // Run all tests in "Tests.md".
  parse("Tests.md")
    .onlyFor(Success, { (name, test) =>
      name in testCase(test)
    })
    .onlyFor(Failure, { (name, test) =>
      name in
        intercept[CompilationError.type] {
          testCase(test)
        }
    })

  // Define additional test cases below.

//  "[S] Additional Test 1" in
//    testCase("NonSuite", """
//val x: Int = 19
//""")

//  "[F] Additional Test 2" in {
//    intercept[CompilationError.type] {
//      testCase("NonSuite", """
//val x: Int = "nineteen"
//""")
//    }
//  }
}
