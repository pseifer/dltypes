import org.scalatest._


class PluginTest extends FreeSpec {
  val tests = new TestTools(1, debug = false)
  import tests._

  // Run all tests in "Tests.md".
  parse("Tests.md")
    //.filterCategory("If")
    .onlyFor(Success, { (name, test) =>
      name in success(testCase(test))
    })
    .onlyFor(Warning, { (name, test) =>
      name in warning(testCase(test))
    })
    .onlyFor(Failure, { (name, test) =>
      name in failure(testCase(test))
    })
}
