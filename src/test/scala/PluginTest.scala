import org.scalatest._


class PluginTest extends FreeSpec {
  import TestTools._
  LOGLVL = 2

  // Run all tests in "Tests.md".
  parse("Tests.md")
    //.filterCategory("Multiple type parameter")
    .onlyFor(Success, { (name, test) =>
      name in assert(success(testCase(test)))
    })
    .onlyFor(Warning, { (name, test) =>
      name in assert(warning(testCase(test)))
    })
    .onlyFor(Failure, { (name, test) =>
      name in assert(error(testCase(test)))
    })
}
