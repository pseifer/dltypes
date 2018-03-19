package de.uni_koblenz.dltypes
package backend


case class Pre(s: String) // Prefix name

sealed trait Name
case class Iri(s: String) extends Name { // IRI
  override def toString: String = "<" + s + ">"
}

case class TypedValue(value: String, tpe: Iri) extends Name {
  override def toString: String = "\"" + value + "\"^^" + tpe
}

case class Var(s: String) extends Name { // SPARQL variable
  override def toString: String = s
}

object Name {
  def parse(s: String): Name = {
    val isTypedLiteral = "\"(.*?)\"\\^\\^<(.*?)>".r
    s match {
      case isTypedLiteral(v, t) =>
        println("Is typed")
        TypedValue(v, Iri(t))
      case _ =>
        println("is iri")
        Iri(s)
    }
  }
}


sealed trait QueryExpression
case object EmptyQuery extends QueryExpression
case class ConceptAssertion(name: Name, concept: Name) extends QueryExpression
case class RoleAssertion(left: Name, right: Name, role: Iri) extends QueryExpression
case class Conjunction(left: QueryExpression, right: QueryExpression) extends QueryExpression
case class Disjunction(left: QueryExpression, right: QueryExpression) extends QueryExpression
case class Minus(left: QueryExpression, right: QueryExpression) extends QueryExpression
case class Optional(left: QueryExpression, right: QueryExpression) extends QueryExpression

case class Prefix(s: Pre, iri: Iri)

sealed trait SPARQLQuery {
  val prefixes: Seq[Prefix]
  val qe: QueryExpression
}
case class SelectQuery(prefixes: Seq[Prefix], vars: Seq[Var], qe: QueryExpression) extends SPARQLQuery
case class AskQuery(prefixes: Seq[Prefix], qe: QueryExpression) extends SPARQLQuery
