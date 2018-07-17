import de.uni_koblenz.dltypes.backend._
import org.scalatest._


class SPARQLParserTest extends FlatSpec {
  // Test data

  // TODO: Finish tests (results missing)

  val prefixSPARQL1 = "PREFIX : <someiri>"
  val prefixCode1 = Prefix(Pre(":"), Iri("someiri"))
  val prefixSPARQL2 = "PREFIX uni: <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#>"
  val prefixCode2 = Prefix(Pre("uni:"), Iri("http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#"))

  val conceptAssertSPARQL = "?x a :Something"
  val conceptAssertCode = ConceptAssertion(Var("x"), Iri(":Something"))
  val roleAssertSPARQL = "?x :headOf :Something"
  val roleAssertCode = RoleAssertion(Var("x"), Iri(":Something"), Iri(":headOf"))

  "The parser" should "parse" in {
    val p = new SPARQLParser
    val qs = p.parse(
      s"""
         |PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#>
         |SELECT * WHERE {
         |  {
         |  $conceptAssertSPARQL.
         |  $conceptAssertSPARQL.
         |  $roleAssertSPARQL
         |  } UNION {
         |  $conceptAssertSPARQL.
         |  $roleAssertSPARQL
         |  }
         |}
       """.stripMargin)
    println(qs)
  }

  "The parser" should "parse optional" in {
    val p = new SPARQLParser
    val qs = p.parse(
      s"""
         |PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#>
         |SELECT * WHERE {
         |  $conceptAssertSPARQL.
         |  OPTIONAL { $roleAssertSPARQL }
         |}
       """.stripMargin )
    println(qs)
  }

  "The parser" should "parse minus" in {
    val p = new SPARQLParser
    val qs = p.parse(
      s"""
         |PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#>
         |SELECT * WHERE {
         |  $conceptAssertSPARQL.
         |  MINUS {
         |    $roleAssertSPARQL.
         |    $conceptAssertSPARQL.
         |  }
         |}
       """.stripMargin )
    println(qs)
  }

  "Test" should "parse integers" in {
    val p = new SPARQLParser
    val qs = p.parse(
      s"""
         |PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#>
         |SELECT ?y WHERE {
         |  ?y :hasAge 42.0
         |}
       """.stripMargin
    )
    println(qs)
  }
}




/* old tests
// Utility wrapper for expected results.
private def parsing[T](s: String)(implicit p: Parser[T]): T = {
  val phraseParser = phrase(p)
  val input = new CharSequenceReader(s)
  phraseParser(input) match {
    case Success(t, _) => t
    case NoSuccess(msg, _) => throw new IllegalArgumentException(
      "Parser error in '" + s + "': " + msg)
  }
}

// Utility wrapper for expected failure.
private def assertFail[T](input: String)(implicit p: Parser[T]): Unit = {
  an [IllegalArgumentException] should be thrownBy { parsing(input)(p) }
}


"The Parser" should "parse prefixes" in {
  implicit val parserToTest: Parser[Prefix] = prefix
  parsing(prefixSPARQL1) should equal(prefixCode1)
  parsing(prefixSPARQL2) should equal(prefixCode2)
}

"The Parser" should "parse an entire prologue" in {
  implicit val parserToTest: Parser[List[Prefix]] = prologue
  parsing(
    s"""
      |$prefixSPARQL1
      |$prefixSPARQL2
    """.stripMargin) should equal(List(prefixCode1, prefixCode2))
  parsing(
    s"""
      |
      |$prefixSPARQL1
      |
      |$prefixSPARQL2
      |
    """.stripMargin) should equal(List(prefixCode1, prefixCode2))
}

//"The Parser" should "parse ask queries" in {
//  implicit val parserToTest: Parser[SPARQLQuery] = query
//  parsing(
//    """
//      | ASK { ?x ?y ?z }
//    """.stripMargin) should equal(AskQuery(Nil, ConceptAssertion(Right(Individual("")), Concept(""))))
//}

//"The Parser" should "parse select queries" in {
//  implicit val parserToTest: Parser[SPARQLQuery] = query
//  parsing(
//    """
//      | SELECT * WHERE { ?x ?y ?z }
//    """.stripMargin) should equal(SelectQuery(Nil, Nil, ConceptAssertion(Right(Individual("")), Concept(""))))
//}

"The Parser" should "parse concept and role assertion" in {
  implicit val parserToTest: Parser[QueryExpression] = qexpr
  parsing(conceptAssertSPARQL) should equal(conceptAssertCode)
  parsing(roleAssertSPARQL) should equal(roleAssertCode)
}

"The Parser" should "parse concept and role assertion in queries" in {
  implicit val parserToTest: Parser[SPARQLQuery] = query
  parsing(
    s"""
      |SELECT * WHERE {
      |  $conceptAssertSPARQL
      |}
    """.stripMargin) should equal(SelectQuery(Nil, Nil, conceptAssertCode))
  parsing(
    s"""
      |SELECT * WHERE {
      |  $roleAssertSPARQL
      |}
    """.stripMargin) should equal(SelectQuery(Nil, Nil, roleAssertCode))
}

"The Parser" should "parse simple conjunctions" in {
  implicit val parserToTest: Parser[QueryExpression] = qexpr
  parsing(s"$conceptAssertSPARQL. $roleAssertSPARQL") should equal(Conjunction(conceptAssertCode, roleAssertCode))
}

"The Parser" should "parse simple conjunctions in queries" in {
  implicit val parserToTest: Parser[SPARQLQuery] = query
  parsing(
    s"""
       |SELECT * WHERE {
       |  $conceptAssertSPARQL.
       |  $roleAssertSPARQL
       |}
     """.stripMargin) should equal(SelectQuery(Nil, Nil, Conjunction(conceptAssertCode, roleAssertCode)))
  parsing(
    s"""
       |SELECT * WHERE {
       |  $conceptAssertSPARQL.
       |  $roleAssertSPARQL.
       |}
       """.stripMargin) should equal(SelectQuery(Nil, Nil, Conjunction(conceptAssertCode, Conjunction(roleAssertCode, EmptyQuery))))
}
*/

