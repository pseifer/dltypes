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

  "The Reasoner" should "handle literal data types" in {

    reasoner.prove(Subsumed(Concept(":Vintage"), Existential(Role(":hasVintageYear"), Concept(":VintageYear")))) should be(true)
    reasoner.prove(Subsumed(Existential(Role(":hasVintageYear"), Concept(":VintageYear")), Concept(":Vintage"))) should be(true)

    reasoner.prove(Subsumed(Concept(":Vintage"), Existential(Role(":hasVintageYear"), Top))) should be(true)
    reasoner.prove(Subsumed(Existential(Role(":hasVintageYear"), Top), Concept(":Vintage"))) should be(true)

    /*
    reasoner.prove(Subsumed(Existential(Data(":yearValue"), Type("xsd:integer")), Concept(":VintageYear"))) should be(true)
    reasoner.prove(Subsumed(Concept(":VintageYear"), Existential(Data(":yearValue"), Type("xsd:integer")))) should be(true) // Fails - Why?
    */

    reasoner.prove(ConceptEquality(
      Existential(Data(":yearValue"), Type("xsd:integer")),
      Existential(Data(":yearValue"), Type("xsd:integer"))
    )) should be(true)

    reasoner.prove(ConceptEquality(
      Existential(Data(":yearValue"), Type("xsd:integer")),
      Existential(Data(":yearValue"), Type("xsd:string"))
    )) should be(false)

    reasoner.prove(Subsumed(Existential(Data(":yearValue"), Type("xsd:integer")), Concept(":VintageYear"))) should be(true)
    reasoner.prove(Subsumed(Existential(Data(":yearValue"), Type("xsd:string")), Concept(":VintageYear"))) should be(false)



    // Evaluation with placeholders tests. TODO: remove

    reasoner.prove(Subsumed(
      Concept(":Wine"),
      Existential(Role(":hasMaker"), Existential(Inverse(Role(":hasMaker")), Concept(":Wine")))
    )) should be(true)

    reasoner.prove(Subsumed(
      Existential(Inverse(Role(":hasMaker")), Existential(Role(":hasMaker"), Concept(":Winery"))),
      Concept(":Winery")
    )) should be(true)
  }
}
