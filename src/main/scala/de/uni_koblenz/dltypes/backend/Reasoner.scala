package de.uni_koblenz.dltypes
package backend

import org.semanticweb.HermiT.{Reasoner => HermitReasoner}
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model._
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl

import java.io.File

import scala.collection.JavaConverters._
import scala.collection.mutable


trait Reasoner {
  val f: File
  def prove(stmt: DLETruthy): Boolean
}


class ReasonerMock(val ontology: String) extends Reasoner {
  val f = new File(ontology)
  def prove(stmt: DLETruthy): Boolean = true
}


class ReasonerHermit(val ontologyFile: File) {
  import scala.language.implicitConversions

  val manager: OWLOntologyManager = OWLManager.createOWLOntologyManager()
  val df: OWLDataFactory = manager.getOWLDataFactory

  val ontology: OWLOntology = manager.loadOntologyFromOntologyDocument(ontologyFile)
  val hermit = new HermitReasoner(ontology)

  def toIRI(s: String): IRI = IRI.create {
    if (hermit.getPrefixes.canBeExpanded(s))
      hermit.getPrefixes.expandAbbreviatedIRI(s)
    else s
  }
  //implicit val iToIRI = toIRI(_)

  def individualToOWL(individual: DLEIndividual): OWLNamedIndividual =
    individual match {
      case Individual(iri) => df.getOWLNamedIndividual(toIRI(iri))
      case IndividualN(i) => i
    }
  implicit val iIndividualToOWL: DLEIndividual => OWLNamedIndividual = individualToOWL

  def roleToOWL(role: DLERole): OWLObjectPropertyExpression =
    role match {
      case Role(iri) => df.getOWLObjectProperty(toIRI(iri))
      case Inverse(r) => df.getOWLObjectInverseOf(roleToOWL(r))
    }
  implicit val iRoleToOWL: DLERole => OWLObjectPropertyExpression = roleToOWL

  def conceptToOWL(concept: DLEConcept): OWLClassExpression =
    concept match {
      case Nominal(iri) =>
        df.getOWLObjectOneOf(new OWLNamedIndividualImpl(toIRI(iri)))
      case Concept(iri) => df.getOWLClass(toIRI(iri))
      case Top => df.getOWLThing
      case Bottom => df.getOWLNothing
      case Negation(expr) =>
        df.getOWLObjectComplementOf(conceptToOWL(expr))
      case Intersection(lexpr, rexpr) =>
        df.getOWLObjectIntersectionOf(conceptToOWL(lexpr), conceptToOWL(rexpr))
      case Union(lexpr, rexpr) =>
        df.getOWLObjectUnionOf(conceptToOWL(lexpr), conceptToOWL(rexpr))
      case Existential(role, expr) =>
        df.getOWLObjectSomeValuesFrom(roleToOWL(role), conceptToOWL(expr))
      case Universal(role, expr) =>
        df.getOWLObjectAllValuesFrom(roleToOWL(role), conceptToOWL(expr))
    }
  implicit val iConceptToOWL: DLEConcept => OWLClassExpression = conceptToOWL

  // Prove DL expressions (boolean result).
  def prove(stmt: DLETruthy): Boolean = stmt match {
    case Subsumed(c, d) => subsumed(c, d)
    case Satisfiable(c) => satisfiable(c)
    case Unsatisfiable(c) => unsatisfiable(c)
    case IndividualEquality(a, b) => equivalent(a, b)
    case ConceptEquality(c, d) => equivalent(c, d)
    case MemberOf(a, c) => memberOf(a, c)
  }

  // Query HermiT for individuals (for Concept or Role).
  def query(stmt: DLEQuery): mutable.Set[DLEIndividual] = stmt match {
    case IndividualsForConcept(c) => individualsFor(c)
    case IndividualsForRole(a, r) => individualsFor(a, r)
  }

  private def subsumed(c: DLEConcept, d: DLEConcept): Boolean = {
    val normal = df.getOWLSubClassOfAxiom(c, d)
    val negate = df.getOWLSubClassOfAxiom(c, df.getOWLObjectComplementOf(d))
    hermit.isEntailed(normal) && !hermit.isEntailed(negate)
  }

  private def satisfiable(c: DLEConcept): Boolean = hermit.isSatisfiable(c)

  private def unsatisfiable(c: DLEConcept): Boolean = !satisfiable(c)

  private def equivalent(c: DLEConcept, d: DLEConcept): Boolean =
    if (c.isOWLNothing) unsatisfiable(c)
    else if (d.isOWLNothing) unsatisfiable(d)
    else {
      val normal = df.getOWLEquivalentClassesAxiom(c, d)
      hermit.isEntailed(normal)
    }

  private def memberOf(a: DLEIndividual, c: DLEConcept): Boolean = {
    val normal = df.getOWLClassAssertionAxiom(c, a)
    val negate = df.getOWLClassAssertionAxiom(df.getOWLObjectComplementOf(c), a)
    hermit.isEntailed(normal) && !hermit.isEntailed(negate)
  }

  private def individualsFor(c: DLEConcept): mutable.Set[DLEIndividual] =
    hermit.getInstances(c, false).getFlattened.asScala.map(IndividualN)

  private def individualsFor(individual: DLEIndividual,
                             role: DLERole): mutable.Set[DLEIndividual] =
    hermit.getObjectPropertyValues(individual, role).getFlattened.asScala.map(IndividualN)

  private def equivalent(a: DLEIndividual, b: DLEIndividual): Boolean =
    hermit.isSameIndividual(a, b)
}
