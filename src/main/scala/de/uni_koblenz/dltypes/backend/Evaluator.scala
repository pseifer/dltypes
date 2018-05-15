package de.uni_koblenz.dltypes
package backend

import de.uni_koblenz.dltypes.backend.DLTypesError.EitherDL
import de.uni_koblenz.dltypes.tools._
import de.uni_koblenz.dltypes.tools.DLEConcept.simplify


class Evaluator(val reasoner: Reasoner) {

  // ConstraintMap: Maps variables to DLEConcepts.
  type ConstraintMap = Map[Variable, DLEConcept]

  // Similar to ConstraintMap, but each variable may be associated with
  // multiple DLEConcepts (needed for intermediate steps).
  type ConstraintMultiMap = Map[Variable, List[DLEConcept]]

  // Merge two constraint maps, if a variable appears in both, prefer left.
  private def mergeLeft(m1: ConstraintMap, m2: ConstraintMap): ConstraintMap = m2 ++ m1

  // Merge two constraint maps into constraint multi map (i.e., if variable appears on
  // both sides, keep all constraints.
  private def merge(m1: ConstraintMap, m2: ConstraintMap): ConstraintMultiMap =
    // Wrap in lists and merge as sequences.
    (m1.mapValues(List(_)).toSeq ++ m2.mapValues(List(_)).toSeq)
      // Group by keys.
      .groupBy(_._1)
      // Transform values back to simple lists.
      .mapValues(_.flatMap(_._2).toList)

  // Join constraint multi map (fold) with function f, to obtain constraint map.
  private def join(m: ConstraintMultiMap, init: DLEConcept, f: (DLEConcept, DLEConcept) => DLEConcept): ConstraintMap =
    m.mapValues(lst => lst.foldLeft(init)(f))

  // Algebraic data type for Constraint Sets.
  sealed trait CS {
    def get: ConstraintMap
  }

  // Conjunction (^)
  case class Con(s1: CS, s2: CS) extends CS {
    def get = join(merge(s1.get, s2.get), Top, Intersection)
  }

  // Disjunction (v)
  case class Dis(s1: CS, s2: CS) extends CS {
    def get = join(merge(s1.get, s2.get), Bottom, Union)
  }

  // Optional (.)
  case class Opt(s1: CS, s2: CS) extends CS {
    def get = {
      // Opt(s1, s2) := Dis(s1, Con(s1, s2))
      val ts1 = s1.get // optimized to calculate s1 only once.
      join(merge(ts1,
        join(merge(ts1, s2.get), Top, Intersection)
      ), Bottom, Union)
    }
  }

  // Negation (-)
  case class Neg(s1: CS, s2: CS) extends CS {
    def get = mergeLeft(s1.get, s2.get)
  }

  // Basic constraint: Concept.
  case class ConceptConstraint(x: Variable, c: Concept) extends CS {
    def get = Map(x -> c)
  }

  // Basic constraint: Literal Type.
  case class LiteralConstraint(x: Variable, t: Type) extends CS {
    def get = Map(x -> t)
  }

  // Basic constraint: (DataProperty) Role to Type.
  case class LiteralRoleConstraint(x: Variable, r: Data, t: Type) extends CS {
    def get = Map(x -> Existential(r, t))
  }

  // Basic constraint: Role to nominal.
  case class RoleConstraint(x: Variable, r: Role, n: Nominal) extends CS {
    def get = Map(x -> Existential(r, n))
  }

  // Basic constraint: Inverse role to nominal.
  case class InvRoleConstraint(x: Variable, i: Inverse, n: Nominal) extends CS {
    def get = Map(x -> Existential(i, n))
  }

  // Basic constraint: Bilateral roles between two variables.
  case class Role2Constraint(x1: Variable, x2: Variable, r: Role) extends CS {
    def get =
      if (x1 == x2) // Same variable.
        Map(x1 -> Intersection(Existential(r, x1), Existential(Inverse(r), x1)))
      else
        Map(x1 -> Existential(r, x2), x2 -> Existential(Inverse(r), x1))
  }

  private def qexprToCS(qexpr: QueryExpression): Option[CS] = {
    // Helper for recursive query expressions.
    def recf(e1: QueryExpression, e2: QueryExpression, op: (CS, CS) => CS): Option[CS] =
      qexprToCS(e1).flatMap(l => qexprToCS(e2).flatMap(r => Some(op(l, r))))

    qexpr match {
      // TODO: ConceptAssertion(placeholder, ?y) (e.g. $w a ?y)  !!!
      // Basically ConceptAssertion with two variables.
    case ConceptAssertion(Var(x), Iri(i)) => Some(ConceptConstraint(Variable(x), Concept(i)))
    case ConceptAssertion(Var(x), TypedValue(_, Iri(i))) => Some(LiteralConstraint(Variable(x), Type(i)))
    case RoleAssertion(Var(x), Iri(b), Iri(r)) => Some(RoleConstraint(Variable(x), Role(r), Nominal(b)))
    case RoleAssertion(Iri(a), Var(x), Iri(r)) => Some(InvRoleConstraint(Variable(x), Inverse(Role(r)), Nominal(a)))
    case RoleAssertion(Var(x1), Var(x2), Iri(r)) => Some(Role2Constraint(Variable(x1), Variable(x2), Role(r)))
    case RoleAssertion(Var(x), TypedValue(_, Iri(b)), Iri(r)) => Some(LiteralRoleConstraint(Variable(x), Data(r), Type(b)))
    case Conjunction(left, right) => recf(left, right, Con)
    case Disjunction(left, right) => recf(left, right, Dis)
    case Minus(left, right) => recf(left, right, Neg)
    case Optional(left, right) => recf(left, right, Opt)
    case _ => None
  } }

  // Test, whether DLEConcept contains variables.
  private def isSimple(c: DLEConcept): Boolean = !c.hasVariables

  // Test, whether DLEConcept contains only one Variable 'v'.
  private def selfOnly(v: Variable, c: DLEConcept): Boolean = {
    val t = c.getVariables
    t.size == 1 && t.head == v // contains exactly one, which is self.
  }

  // Substitute Variable 'v' by Top.
  private def selfToTop(v: Variable, c: DLEConcept): DLEConcept =
    DLEConcept.substitute(c, v, Top)

  private def resolve(cs: ConstraintMap): ConstraintMap = {
    def go(cs: ConstraintMap): ConstraintMap = {
      // If all values are simple, result is done.
      if (cs.values.map(isSimple).forall(identity))
        cs
      else {
        // Recur after 1 substitution step.
        resolve(cs.map { case (v, c) =>
          // Substitute all variables in c.
          val cNew = c.getVariables.foldLeft(c) { case (cc, vi) =>
            DLEConcept.substitute(cc, vi, cs(vi))
          }
          // Simplify expressions and substitute self reference with Top.
          v -> selfToTop(v, simplify(cNew))
        })
      }
    }
    // Simplify expressions and substitute self reference with Top,
    // then start resolution.
    go(cs.map { case (v,c) => v -> selfToTop(v, simplify(c)) })
  }

  def eval(vs: Seq[Var],
           qe: QueryExpression,
           placeholders: ConstraintMap,
           strict: Boolean): EitherDL[List[DLEConcept]] =

    // Build the constraint set.
    qexprToCS(qe) match {
      case None =>
        Left(InferenceError("Query expression is faulty. There might be something wrong with the SPARQL query."))
      case Some(cs) => {
        // Evaluate constraints.
        val constraints = cs.get

        val res =
          if (strict || MyGlobal.strictQueryTyping) {
            // First Pass: Resolve constraints with placeholders.
            val before = resolve(constraints)

            // Check if placeholder constraints are subsumed by true argument types.
            val sat = before.map {
              case (Variable(vi), c) =>
                val mapped = Variable(s"?$vi")
                if (placeholders.contains(mapped)) {
                  val test = Subsumed(placeholders(mapped), c)
                  Some((reasoner.prove(test), test))
                }
                else
                  None
            }.filter( x => x.isDefined && !x.get._1).map(_.get._2)
              //.map(x =>
              //if (x.isDefined && !x.get._1)
              //  x.get._2)

            // There was an error.
            if (sat.nonEmpty)
              Left(SPARQLError(s"Constraints not subsumed by argument types for: ${
                sat.map(t => "\n" + t.toString)
              }"))
            else
              // Second pass: Resolve constraints, but replace placeholders with argument types.
              Right(resolve(constraints.map { case (v@Variable(vi), c) =>
                val mapped = Variable(s"?$vi")
                if (placeholders.contains(mapped))
                  v -> placeholders(mapped)
                else
                  v -> c
              }))
          }
          else {
            // Resolve constraints, intersect placeholder types with their constraints.
            Right(resolve(constraints.map { case (v@Variable(vi), c) =>
              val mapped = Variable(s"?$vi")
              if (placeholders.contains(mapped))
                v -> Intersection(c, placeholders(mapped))
              else
                v -> c
            }))
          }

        res.flatMap { r =>
          // Check if types are satisfiable.
          if (r.values.map { case x => reasoner.prove(Satisfiable(x)) }.exists(_ == false))
            Left(TypeError("Query is not satisfiable."))
          // Using SELECT * in query, return all.
          else if (vs.isEmpty) // Nil == SELECT * ...
            Right(r.toList.sortWith { case ((Variable(x1), _), (Variable(x2), _)) => x1 < x2 }.map(_._2))
          else
          // Otherwise, project to selected variables.
            Right(vs.map { case Var(x) => r(Variable(x)) }.toList)
        }
      }
    }
}
