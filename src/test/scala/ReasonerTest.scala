import de.uni_koblenz.dltypes.backend._
import de.uni_koblenz.dltypes.tools._

import org.scalatest._
import java.io.File


class ReasonerTest extends Parser with FlatSpecLike with Matchers {
  // Initialize a reasoner with the test ontology.
  val fi = new File(getClass.getResource("/wine.rdf").getFile)
  val reasoner = new ReasonerHermit(fi)

  // TODO: Add more test cases.

  "The Reasoner" should "prove simple concept equality" in {
    reasoner.prove(ConceptEquality(Concept(":Wine"), Concept(":Wine"))) should be(true)
    reasoner.prove(ConceptEquality(Concept(":RedWine"), Concept(":RedWine"))) should be(true)
    reasoner.prove(ConceptEquality(Concept(":RedWine"), Concept(":Wine"))) should be(false)
  }

  "The Reasoner" should "prove simple concept subsumption" in {
    reasoner.prove(Subsumed(Concept(":Wine"), Concept(":Wine"))) should be(true)
    reasoner.prove(Subsumed(Concept(":RedWine"), Concept(":Wine"))) should be(true)
    reasoner.prove(Subsumed(Concept(":Wine"), Concept(":RedWine"))) should be(false)
  }
}
