import org.scalatest._


class PluginTest extends FreeSpec {
  import TestTools._

  LOGLVL = 2

  val parser = new TestParser()
  parser.parse("Tests.md")

  parser.cases.foreach {
    case TestCase(Success, name, code) =>
      name in {
        testCase(name, code)
      }
    case TestCase(Failure, name, code) =>
      name in {
        intercept[CompilationError.type] {
          testCase(name, code)
        }
      }
  }

  // Inline test cases (failure).

//  "Compilation should fail" in {
//    intercept[CompilationError.type] {
//      TestTools.testCase("Test0",
//"""
//val a: `:RedWine` = iri"PeterMccoyChardonnay"
//"""
//      )
//    }
//  }

  // Inline test cases (success).

//  "Compilation should succeed" in {
//    TestTools.testCase("Test0",
//"""
//val a: `:WhiteWine` = iri"PeterMccoyChardonnay"
//"""
//    )
//  }
}
