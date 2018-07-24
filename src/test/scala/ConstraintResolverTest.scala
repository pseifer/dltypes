import de.uni_koblenz.dltypes.backend._
import de.uni_koblenz.dltypes.tools._
import org.scalatest._


class ConstraintResolverTest extends FreeSpec {
  val resolver = new ConstraintResolver(new TrueReasoner)

  def runTest(q: QueryExpression, r: List[DLEConcept], strict: Boolean = false): Boolean = {
    val cs = resolver.eval(Nil, q, Map(), strict = strict)
    println(cs)
    println(r)
    cs.right.get == r
  }

  // { ?x a :Researcher }
  "Concept Assertion" in {
    assert(runTest(
      ConceptAssertion(Var("?x"), Iri(":Researcher")),
      List(Concept(":Researcher"))
  ))}

  // { ?x a :Researcher . ?x a :Professor }
  "Conjunction of Concept Assertion" in {
    assert(runTest(
      Conjunction(
        ConceptAssertion(Var("?x"), Iri(":Researcher")),
        ConceptAssertion(Var("?x"), Iri(":Professor"))),
      List(Intersection(Concept(":Researcher"), Concept(":Professor")))
  ))}

  // { ?x :knows :y . ?y a :Researcher }
  "Conjunction: Concept Assertion and Role Assertion" in {
    assert(runTest(
      Conjunction(
        RoleAssertion(Var("?x"), Var("?y"), Iri(":knows")),
        ConceptAssertion(Var("?y"), Iri(":Researcher"))),
      List(
        Existential(Role(":knows"), Intersection(Existential(Inverse(Role(":knows")),Top),Concept(":Researcher"))),
        Intersection(Existential(Inverse(Role(":knows")),Existential(Role(":knows"),Top)),Concept(":Researcher"))
      )
    ))}

  // { ?x :knows :y }
  "Role Assertion" in {
    assert(runTest(
        RoleAssertion(Var("?x"), Var("?y"), Iri(":knows")),
      List(
        Existential(
          Role(":knows"),
          Existential(
            Inverse(Role(":knows")),Top)),
        Existential(
          Inverse(Role(":knows")),
          Existential(
            Role(":knows"),Top))
      )
    ))}

  // { ?x :knows :y } UNION { ?y a :Researcher }
  "Disjunction: Concept Assertion and Role Assertion" in {
    assert(runTest(
      Disjunction(
        RoleAssertion(Var("?x"), Var("?y"), Iri(":knows")),
        ConceptAssertion(Var("?y"), Iri(":Researcher"))),
      List(
        Existential(
          Role(":knows"),
          Union(
            Existential(Inverse(Role(":knows")),Top),
            Concept(":Researcher"))),
        Union(
          Existential(Inverse(Role(":knows")),Existential(Role(":knows"),Top)),
          Concept(":Researcher"))
      )
    ))}
}
