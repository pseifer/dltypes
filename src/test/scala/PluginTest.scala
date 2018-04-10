import org.scalatest._


class PluginTest extends FreeSpec {
  val tests = new TestTools(3)
  import tests._

  // Run all tests in "Tests.md".
  parse("Tests.md")
    //.filterCategory("<none>")
    .onlyFor(Success, { (name, test) =>
      name in success(testCase(test))
    })
    .onlyFor(Warning, { (name, test) =>
      name in warning(testCase(test))
    })
    .onlyFor(Failure, { (name, test) =>
      name in failure(testCase(test))
    })

  "Role projection" in {
    success(testCase("test",
    """
      |val p = iri"PeterMccoyChardonnay"
      |val x: List[`:Winery`] = p.`:hasMaker`
    """.stripMargin))
  }

  "Strict query" in {
    success(testCase("test",
    """
      |val p = iri"PeterMccoyChardonnay"
      |val x: List[`:Winery`] = strictsparql"PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#> SELECT ?y WHERE { $p :hasMaker ?y }"
    """.stripMargin))
  }

  "Equality (warn)" in {
    success(testCase("test",
      """
        |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val p1 = rw == ww
      """.stripMargin))
  }

  "Equality (ok)" in {
    success(testCase("test",
    """
      |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
      |val ww: `:Wine` = iri"PeterMccoyChardonnay"
      |val p1 = rw == ww
    """.stripMargin))
  }

  "Query with arguments I" in {
    success(testCase("test",
    """
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |val x1: List[`:Winery`] = sparql"PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#> SELECT ?y WHERE { $w :hasMaker ?y }"
    """.stripMargin))
  }

  "Query with arguments II" in {
    failure(testCase("test",
      """
        |val w: `:Winery` = iri"PeterMccoy"
        |val x: List[`:Wine`] = sparql"PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#> SELECT ?y WHERE { ?y :hasMaker $w }"
    """.stripMargin))
  }

  "Query with arguments III" in {
    success(testCase("test",
      """
        |val w: `:Winery` = iri"PeterMccoy"
        |val x: List[`∃:hasMaker.:Winery`] = sparql"PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#> SELECT ?y WHERE { ?y :hasMaker $w }"
      """.stripMargin))
  }

  "Query with Scala arguments (success)" in {
    success(testCase("Test0",
      """
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |val i: Int = 1998
        |val x = sparql"PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#> SELECT ?y WHERE { $w :hasVintageYear ?y. ?y :yearValue $i }"
        |val y: `:VintageYear` = x.head
      """.stripMargin))
  }

  "Query with Scala arguments (failure)" in {
    failure(testCase("Test0",
      """
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |val i: String = "1998"
        |val x = sparql"PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#> SELECT ?y WHERE { $w :hasVintageYear ?y. ?y :yearValue $i }"
      """.stripMargin))
  }

  "Explicit inference (for lists)" in {
    success(testCase("test",
      """
        |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val ws = List[`???`](rw, ww)
        |val ws2: List[`:Wine`] = ws
      """.stripMargin))
  }

  "Explicit inference (for if)" in {
    success(testCase("test",
      """
        |val p = 1 < 2
        |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val e: `???` = if (p) { rw } else { ww }
        |val e2: `:Wine` = e
    """.stripMargin))
  }

  "Runtime: Type Case" in {
    success(testCase("test",
      """
        |val a: Any = iri"PeterMccoyChardonnay"
        |val redAllowed = true
        |a match {
        |  case i: Int => println("integer!")
        |  case w: `:WhiteWine` =>
        |    val g: `:Wine` = w
        |    println("white wine")
        |  case w: `:RedWine` if redAllowed => println("a red wine!")
        |  case _: `:Wine` => println("an undisclosed white wine!")
        |  case _ => println("don't know that thing")
        |}
    """.stripMargin))
    // GENERATES
    // ---------
    //Main.this.a match {
    //  case (i @ (_: Int)) => scala.Predef.println("integer!")
    //  case (x$1 @ (_: de.uni_koblenz.dltypes.runtime.IRI)) if x$1.isSubsumed(de.uni_koblenz.dltypes.tools.Concept.apply(":WhiteWine")) => {
    //    val w: DLTypeDefs.:WhiteWine = x$1;
    //    val g: DLTypeDefs.:Wine = w;
    //    scala.Predef.println("white wine")
    //  }
    //  case (x$2 @ (_: de.uni_koblenz.dltypes.runtime.IRI)) if x$2.isSubsumed(de.uni_koblenz.dltypes.tools.Concept.apply(":RedWine")).&&(Main.this.redAllowed) => {
    //    val w: DLTypeDefs.:RedWine = x$2;
    //    scala.Predef.println("a red wine!")
    //  }
    //  case (x$3 @ (_: de.uni_koblenz.dltypes.runtime.IRI)) if x$3.isSubsumed(de.uni_koblenz.dltypes.tools.Concept.apply(":Wine")) => scala.Predef.println("an undisclosed white wine!")
    //  case _ => scala.Predef.println("don\'t know that thing")
    //}
    // ---------
  }

  "Runtime: instanceOf" in {
    success(testCase("test",
      """
      |val x: Any = iri"PeterMccoyChardonnay"
      |if (x.isInstanceOf[`:RedWine | :WhiteWine`])
      |  println("always " + 19)
    """.
        stripMargin))
    // GENERATES
    // ---------
    // if ({
    // val x$1: Any = Main.this.x
    // x$1.isInstanceOf[de.uni_koblenz.dltypes.runtime.IRI].&&(x$1.asInstanceOf[de.uni_koblenz.dltypes.runtime.IRI].isSubsumed(
    //   de.uni_koblenz.dltypes.tools.Union.apply(
    //     de.uni_koblenz.dltypes.tools.Concept.apply(":RedWine"),
    //     de.uni_koblenz.dltypes.tools.Concept.apply(":WhiteWine"))))
    // })
    //   scala.Predef.println("always".+(19))
    // ---------
  }
}
