package de.uni_koblenz.dltypes
package backend

import org.semanticweb.owlapi.model._


sealed trait DLE

sealed trait DLETruthy extends DLE
case class Subsumed(c: DLEConcept, d: DLEConcept) extends DLETruthy
case class IndividualEquality(a: DLEIndividual, b: DLEIndividual) extends DLETruthy
case class ConceptEquality(c: DLEConcept, d: DLEConcept) extends DLETruthy
case class MemberOf(a: DLEIndividual, c: DLEConcept) extends DLETruthy
case class Satisfiable(c: DLEConcept) extends DLETruthy
case class Unsatisfiable(c: DLEConcept) extends DLETruthy

sealed trait DLEQuery extends DLE
case class IndividualsForConcept(c: DLEConcept) extends DLEQuery
case class IndividualsForRole(a: DLEIndividual, r: DLERole) extends DLEQuery

sealed trait DLERole extends DLE
case class Role(iri: String) extends DLERole
case class Inverse(role: Role) extends DLERole

sealed trait DLEIndividual extends DLE {
  def name: String
  def iri: String = name
}
case class Individual(i: String) extends DLEIndividual {
  def name: String = i
}

case class IndividualN(i: OWLNamedIndividual) extends DLEIndividual {
  def name: String = i.getIRI.getFragment
  override def iri: String = i.getIRI.toString
  override def toString: String = name
}

sealed trait DLEConcept extends DLE
case object Top extends DLEConcept
case object Bottom extends DLEConcept
case class Nominal(iri: String) extends DLEConcept
case class Concept(iri: String) extends DLEConcept
case class Negation(expr: DLEConcept) extends DLEConcept
case class Intersection(lexpr: DLEConcept, rexpr: DLEConcept) extends DLEConcept
case class Union(lexpr: DLEConcept, rexpr: DLEConcept) extends DLEConcept
case class Existential(role: DLERole, expr: DLEConcept) extends DLEConcept
case class Universal(role: DLERole, expr: DLEConcept) extends DLEConcept
