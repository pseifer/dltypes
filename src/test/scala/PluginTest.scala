import org.scalatest._


class PluginTest extends FreeSpec {
  val tests = new TestTools(3)
  import tests._

  // Run all tests in "Tests.md".
  parse("Tests.md")
    .filterCategory("<none>")
    .onlyFor(Success, { (name, test) =>
      name in success(testCase(test))
    })
    .onlyFor(Warning, { (name, test) =>
      name in warning(testCase(test))
    })
    .onlyFor(Failure, { (name, test) =>
      name in failure(testCase(test))
    })

  "[S] isInstanceOf example" in {
    success(testCase("Test0",
      """
        |val x: Any = iri"PeterMccoyChardonnay"
        |if (x.isInstanceOf[`:WhiteWine`])
        |  println("yup")
      """.stripMargin))
  //   GENERATES
  //   ---------
  //   if ({
  //     val ThisIsNotAFreshName: de.uni_koblenz.dltypes.runtime.IRI = {
  //       scala.Predef.println("hi");
  //       de.uni_koblenz.dltypes.runtime.Sparql.IriHelper(scala.StringContext.apply("PeterMccoyChardonnay")).iri()
  //     };
  //     ThisIsNotAFreshName.isInstanceOf[de.uni_koblenz.dltypes.runtime.IRI].&&(ThisIsNotAFreshName.isSubsumed("$colonWhiteWine"))
  //   })
  }

  "[S] type case example" in {
    success(testCase("Test0",
      """
        |val a: Any = iri"PeterMccoyChardonnay"
        |val redAllowed = true
        |a match {
        |  case i: Int => println("integer!")
        |  case w: `:Wine` => println("a wine!")
        |  case w: `:RedWine` if redAllowed => println("a red wine!")
        |  case _: `:WhiteWine` => println("a undisclosed white wine!")
        |  case _ => println("don't know that thing")
        |}
      """.stripMargin))
  //   GENERATES
  //   ---------
  //   Main.this.a match {
  //     case (i @ (_: Int)) => scala.Predef.println("integer!")
  //     case (w @ (_: de.uni_koblenz.dltypes.runtime.IRI)) if w.isSubsumed("$colonWine") => scala.Predef.println("a wine!")
  //     case (w @ (_: de.uni_koblenz.dltypes.runtime.IRI)) if w.isSubsumed("$colonRedWine").&&(Main.this.redAllowed) => scala.Predef.println("a wine!")
  //     case _ => scala.Predef.println("don\'t know that thing")
  //   }
  }

  //"[S] positive example" in {
  //  success(testCase("Test0",
  //    """
  //      |val x: Int = 19
  //    """.stripMargin))
  //}

  //"[F] negative example" in {
  //  failure(testCase("Test0",
  //    """
  //      |val x: Int = "nineteen"
  //    """.stripMargin))
  //}
}
