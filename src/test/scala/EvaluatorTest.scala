import de.uni_koblenz.dltypes.backend._
import de.uni_koblenz.dltypes.tools._
import org.scalatest._


class EvaluatorTest extends FreeSpec {

  val uator = new Evaluator(new TrueReasoner)

  val q1 = Conjunction(
    RoleAssertion(Var("?x"), Var("?y"), Iri(":knows")),
    ConceptAssertion(Var("?y"), Iri(":Researcher")))

  "all" in {
    // No variables requested:
    val cs = uator.eval(Nil, q1, Map())
    println(cs)
    assert(cs == List(
      Intersection(Existential(Inverse(Role(":knows")),Existential(Role(":knows"),Top)),Concept(":Researcher")),
      Existential(Role(":knows"), Intersection(Existential(Inverse(Role(":knows")),Top),Concept(":Researcher")))))
  }
}
