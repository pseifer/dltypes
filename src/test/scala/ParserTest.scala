import de.uni_koblenz.dltypes.backend._
import org.scalatest._

import scala.util.parsing.input.CharSequenceReader


class ParserTest extends Parser with FlatSpecLike with Matchers {

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

  "The Parser" should "parse simple iri" in {
    implicit val parserToTest: Parser[String] = IRI
    parsing(":Wine") should equal(":Wine")
  }

  "The Parser" should "parse top" in {
    implicit val parserToTest: Parser[Top.type] = TOP
    parsing("#t") should equal(Top)
    parsing("⊤") should equal(Top)
    assertFail("#f")
  }

  "The Parser" should "parse bottom" in {
    implicit val parserToTest: Parser[Bottom.type] = BOTTOM
    parsing("#f") should equal(Bottom)
    parsing("⊥") should equal(Bottom)
    assertFail("#t")
  }

  "The Parser" should "parse concepts" in {
    implicit val parserToTest: Parser[Concept] = CONCEPT
    parsing(":Wine") should equal(Concept(":Wine"))
    assertFail("Wine")
  }

  "The Parser" should "parse nominal values" in {
    implicit val parserToTest: Parser[Nominal] = NOMINAL
    parsing("{:Chardonnay}") should equal(Nominal(":Chardonnay"))
    // Whitespace.
    parsing("{ :Chardonnay }") should equal(Nominal(":Chardonnay"))
    assertFail("{ :Chardonnay")
  }

  "The Parser" should "parse roles" in {
    implicit val parserToTest: Parser[Role] = ROLE
    parsing(":hasColor") should equal(Role(":hasColor"))
    // Whitespace.
    parsing(":hasColor") should equal(Role(":hasColor"))
  }

  "The Parser" should "parse inverse roles" in {
    implicit val parserToTest: Parser[Inverse] = NEGATED_ROLE
    parsing("!:hasColor") should equal(Inverse(Role(":hasColor")))
    parsing("¬:hasColor") should equal(Inverse(Role(":hasColor")))
    // Whitespace.
    parsing("! :hasColor") should equal(Inverse(Role(":hasColor")))
    parsing("!:hasColor") should equal(Inverse(Role(":hasColor")))
  }

  "The Parser" should "parse existential and universal quantification" in {
    implicit val parserToTest: Parser[DLEConcept] = dlexpr
    // Basic cases (with nominals).

    parsing("#A:hasColor.{:Red}") should equal(Universal(Role(":hasColor"), Nominal(":Red")))
    parsing("#E:hasColor.{:Red}") should equal(Existential(Role(":hasColor"), Nominal(":Red")))
    parsing("∀:hasColor.{:Red}") should equal(Universal(Role(":hasColor"), Nominal(":Red")))
    parsing("∃:hasColor.{:Red}") should equal(Existential(Role(":hasColor"), Nominal(":Red")))
    // Whitespace.
    parsing("#A :hasColor . { :Red }") should equal(Universal(Role(":hasColor"), Nominal(":Red")))
    parsing("#E :hasColor . { :Red }") should equal(Existential(Role(":hasColor"), Nominal(":Red")))
    parsing("∀ :hasColor.{:Red}") should equal(Universal(Role(":hasColor"), Nominal(":Red")))
    parsing("∃ :hasColor.{:Red}") should equal(Existential(Role(":hasColor"), Nominal(":Red")))
    // Inverse role.
    parsing("#A :hasColor . { :Red }") should equal(Universal(Role(":hasColor"), Nominal(":Red")))
    parsing("#E :hasColor . { :Red }") should equal(Existential(Role(":hasColor"), Nominal(":Red")))
    // Quantification over concepts.
    parsing("#A:role.:Thing") should equal(Universal(Role(":role"), Concept(":Thing")))
    parsing("#E:role.:Thing") should equal(Existential(Role(":role"), Concept(":Thing")))
    // Quantification over top/bot
    parsing("#A:role.#t") should equal(Universal(Role(":role"), Top))
    parsing("#E:role.#t") should equal(Existential(Role(":role"), Top))
    parsing("#A:role.#f") should equal(Universal(Role(":role"), Bottom))
    parsing("#E:role.#f") should equal(Existential(Role(":role"), Bottom))
  }

  "The Parser" should "parse unions" in {
    implicit val parserToTest: Parser[DLEConcept] = dlexpr
    parsing(":Wine | :Food") should equal(Union(Concept(":Wine"), Concept(":Food")))
    parsing(":Wine ⊔ :Food") should equal(Union(Concept(":Wine"), Concept(":Food")))
    parsing(":Wine ⊔ :Food") should equal(Union(Concept(":Wine"), Concept(":Food")))
    assertFail(":Wine | | :Food")
  }

  "The Parser" should "parse intersections" in {
    implicit val parserToTest: Parser[DLEConcept] = dlexpr
    parsing(":Wine & :Food") should equal(Intersection(Concept(":Wine"), Concept(":Food")))
    parsing(":Wine ⊓ :Food") should equal(Intersection(Concept(":Wine"), Concept(":Food")))
  }

  "The Parser" should "parse expressions considering precedence" in {
    implicit val parserToTest: Parser[DLEConcept] = dlexpr
    // Union and intersection precedence.
    parsing(":Wine & :Food | :Door & :Wall") should equal(Union(Intersection(Concept(":Wine"), Concept(":Food")), Intersection(Concept(":Door"), Concept(":Wall"))))
    parsing("(:Wine & :Food | :Door) & :Wall") should equal(Intersection(Union(Intersection(Concept(":Wine"), Concept(":Food")), Concept(":Door")), Concept(":Wall")))
    // Parentheses, f-expr and precedence.
    parsing("#A:hasColor.(#A:type.:Wine)") should equal(Universal(Role(":hasColor"), Universal(Role(":type"), Concept(":Wine"))))
    parsing("#A:hasColor.#A:type.:Wine") should equal(Universal(Role(":hasColor"), Universal(Role(":type"), Concept(":Wine"))))
    parsing("#A:hasColor.#E:type.:Wine") should equal(Universal(Role(":hasColor"), Existential(Role(":type"), Concept(":Wine"))))
    parsing("#A:hasColor.{:Red} | {:White}") should not equal(Universal(Role(":hasColor"), Union(Nominal(":Red"), Nominal(":White"))))
    parsing("#A:hasColor.{:Red} | {:White}") should equal(Union(Universal(Role(":hasColor"), Nominal(":Red")), Nominal(":White")))
    parsing("#A:hasColor.({:Red} | {:White})") should equal(Universal(Role(":hasColor"), Union(Nominal(":Red"), Nominal(":White"))))
  }

  "The Parser" should "parse negated expressions" in {
    implicit val parserToTest: Parser[DLEConcept] = dlexpr
    parsing("!:Wine") should equal(Negation(Concept(":Wine")))
    parsing("!!:Wine") should equal(Negation(Negation(Concept(":Wine"))))
    parsing("!!!:Wine") should equal(Negation(Negation(Negation(Concept(":Wine")))))
    // Whitespace.
    parsing(" ! ! :Wine") should equal(Negation(Negation(Concept(":Wine"))))
    parsing(" !   !  ! :Wine") should equal(Negation(Negation(Negation(Concept(":Wine")))))
    // Precedence.
    parsing("!:Wine & :Food | :Door & :Wall") should equal(Union(Intersection(Negation(Concept(":Wine")), Concept(":Food")), Intersection(Concept(":Door"), Concept(":Wall"))))
    parsing("!(:Wine & :Food) | :Door & :Wall") should equal(Union(Negation(Intersection(Concept(":Wine"), Concept(":Food"))), Intersection(Concept(":Door"), Concept(":Wall"))))
    parsing("!(:Wine & :Food | :Door & :Wall)") should equal(Negation(Union(Intersection(Concept(":Wine"), Concept(":Food")), Intersection(Concept(":Door"), Concept(":Wall")))))
  }
}
