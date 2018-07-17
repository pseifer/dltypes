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
      case isTypedLiteral(v, t) => TypedValue(v, Iri(t))
      case _ => Iri(s)
    }
  }
}

object QueryExpression {

  private def takeVar(n: Name): List[Var] = {
    n match {
      case v@Var(_) => List(v)
      case _ => Nil
    }
  }

  private def collectO(qe: QueryExpression): List[Var] = {
    qe match {
      case ConceptAssertion(n, c) => takeVar(n) ++ takeVar(c)
      case RoleAssertion(l, r, _) => takeVar(l) ++ takeVar(r)

      case Conjunction(l, r) => collectO(l) ++ collectO(r)
      case Disjunction(l, r) => collectO(l) ++ collectO(r)
      case Minus(l, r) => collectO(l) ++ collectO(r)
      case Optional(l, r) => collectO(l) ++ collectO(r)
      case EmptyQuery => Nil
    }
  }

  private def skipO(qe: QueryExpression): List[Var] = {
    qe match {
      case Conjunction(l, r) => skipO(l) ++ skipO(r)
      case Disjunction(l, r) => skipO(l) ++ skipO(r)
      case Minus(l, r) => skipO(l) ++ skipO(r)
      case Optional(left, right) => skipO(left) ++ collectO(right)
      case _ => Nil
    }
  }

  private def collect(qe: QueryExpression): List[Var] = {
    qe match {
      case ConceptAssertion(n, c) => takeVar(n) ++ takeVar(c)
      case RoleAssertion(l, r, _) => takeVar(l) ++ takeVar(r)

      case Conjunction(l, r) => collect(l) ++ collect(r)
      case Disjunction(l, r) => collect(l) ++ collect(r)
      case Minus(l, r) => collect(l) ++ collect(r)
      case Optional(l, _) => collect(l) // skip _
      case EmptyQuery => Nil
    }
  }

  def optionalVars(qe: QueryExpression): Set[Var] =
    skipO(qe).toSet

  def nonOptionalVars(qe: QueryExpression): Set[Var] =
    collect(qe).toSet

  def exclusiveOptionalVars(qe: QueryExpression): Set[Var] = {
    val o = optionalVars(qe)
    val n = nonOptionalVars(qe)
    // those which are in optional subtree but not also outside it
    o.diff(n)
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
