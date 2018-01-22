package de.uni_koblenz.dltypes
package backend

import scala.util.parsing.combinator._

class Parser extends RegexParsers {
  def dlexpr: Parser[DLEConcept] = union
  def union: Parser[DLEConcept] = rep1sep(inter, UNION_TOKEN) ^^ { _.reduceLeft(Union) }
  def inter: Parser[DLEConcept] = rep1sep(f, INTER_TOKEN) ^^ { _.reduceLeft(Intersection) }
  def f: Parser[DLEConcept] =
    ( UNIVERSAL_TOKEN ~ role ~ "." ~ f ^^ { case _ ~ r ~ _ ~ c => Universal(r, c) }
    | EXISTENTIAL_TOKEN ~ role ~ "." ~ f ^^ { case _ ~ r ~ _ ~ c => Existential(r, c) }
    | concept
    | NEGATION_TOKEN ~ f ^^ { case _ ~ f => Negation(f) }
    | "(" ~ dlexpr ~ ")" ^^ { case _ ~ e ~ _ => e }
    )
  def concept: Parser[DLEConcept] = TOP | BOTTOM | NOMINAL | CONCEPT
  def role: Parser[DLERole] = ROLE | NEGATED_ROLE

  // Utility parser for specific concepts and roles.
  def IRI: Parser[String] = """:[a-zA-z]+""".r ^^ { _.toString } // TODO: Actual IRI parser
  def TOP: Parser[Top.type] = TOP_TOKEN ^^ { _ => Top }
  def BOTTOM: Parser[Bottom.type] = BOTTOM_TOKEN ^^ { _ => Bottom }
  def NOMINAL: Parser[Nominal] = "{" ~ IRI ~ "}" ^^ { case _ ~ s ~ _ => Nominal(s) }
  def CONCEPT: Parser[Concept] = IRI ^^ { s => Concept(s) }
  def ROLE: Parser[Role] = IRI ^^ { r => Role(r) }
  def NEGATED_ROLE: Parser[Inverse] = NEGATION_TOKEN ~ ROLE ^^ { case _ ~ r => Inverse(r) }
  def INDIVIDUAL: Parser[DLEIndividual] = IRI ^^ { Individual }

  // Tokens.
  def UNION_TOKEN: Parser[Any] = "|" | "⊔"
  def INTER_TOKEN: Parser[Any] = "&" | "⊓"
  def UNIVERSAL_TOKEN: Parser[Any] = "#A" | "∀"
  def EXISTENTIAL_TOKEN: Parser[Any] = "#E" | "∃"
  def NEGATION_TOKEN: Parser[Any] = "!" | "¬"
  def TOP_TOKEN: Parser[Any] = "#t" | "⊤"
  def BOTTOM_TOKEN: Parser[Any] = "#f" | "⊥"
}
