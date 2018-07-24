import de.uni_koblenz.dltypes.backend._
import org.scalatest._
import org.semanticweb.owlapi.model.IRI


class SPARQLParserTest extends FlatSpec {
  // Setup globals for this test run.
  Globals.ontology = IRI.create { "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine" }
  Globals.prefixes += ":" -> (Globals.ontology.toString + "#")

  // Test data
  val conceptAssertSPARQL = "?x a :Something"
  val conceptAssertCode =
    ConceptAssertion(
      Var("x"),
      Iri("http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#Something"))

  val roleAssertSPARQL = "?x :headOf :Something"
  val roleAssertCode =
    RoleAssertion(
      Var("x"),
      Iri("http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#Something"),
      Iri("http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#headOf"))

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
    assert(qs ==
      Right(SelectQuery(List(),List(Var("x")),
        Disjunction(
          Conjunction(
            Conjunction(
              conceptAssertCode,
              conceptAssertCode),
            roleAssertCode),
          Conjunction(
            conceptAssertCode,
            roleAssertCode)))))
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
    assert(qs ==
      Right(SelectQuery(List(), List(Var("x")),
        Optional(conceptAssertCode, roleAssertCode)))
    )
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
    assert(qs ==
      Right(SelectQuery(List(), List(Var("x")),
        Minus(
          conceptAssertCode,
          Conjunction(
            roleAssertCode,
            conceptAssertCode)))))
  }

  "The parser" should "parse integers" in {
    val p = new SPARQLParser
    val qs = p.parse(
      s"""
         |PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#>
         |SELECT ?x WHERE {
         |  ?x :hasAge 42.0
         |}
       """.stripMargin
    )
    assert(qs ==
      Right(SelectQuery(List(), List(Var("x")),
        RoleAssertion(
          Var("x"),
          TypedValue("42.0", Iri("http://www.w3.org/2001/XMLSchema#decimal")),
          Iri("http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#hasAge"))
      )))
  }
}
