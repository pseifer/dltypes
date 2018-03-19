package de.uni_koblenz.dltypes
package backend

import de.uni_koblenz.dltypes.tools._
import de.uni_koblenz.dltypes.tools.DLEConcept.simplify

import scala.util.Try


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

  @throws(classOf[FaultyQueryExpressionError])
  private def qexprToCS(qexpr: QueryExpression): CS = { qexpr match {
    case ConceptAssertion(Var(x), Iri(i)) => ConceptConstraint(Variable(x), Concept(i))
    case ConceptAssertion(Var(x), TypedValue(_, Iri(i))) => LiteralConstraint(Variable(x), Type(i))
    case RoleAssertion(Var(x), Iri(b), Iri(r)) => RoleConstraint(Variable(x), Role(r), Nominal(b))
    case RoleAssertion(Iri(a), Var(x), Iri(r)) => InvRoleConstraint(Variable(x), Inverse(Role(r)), Nominal(a))
    case RoleAssertion(Var(x1), Var(x2), Iri(r)) => Role2Constraint(Variable(x1), Variable(x2), Role(r))
    case RoleAssertion(Var(x), TypedValue(_, Iri(b)), Iri(r)) => LiteralRoleConstraint(Variable(x), Data(r), Type(b))
    case Conjunction(left, right) => Con(qexprToCS(left), qexprToCS(right))
    case Disjunction(left, right) => Dis(qexprToCS(left), qexprToCS(right))
    case Minus(left, right) => Neg(qexprToCS(left), qexprToCS(right))
    case Optional(left, right) => Opt(qexprToCS(left), qexprToCS(right))
    case _ => throw new FaultyQueryExpressionError
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

  def eval(vs: Seq[Var], qe: QueryExpression, placeholders: ConstraintMap): Try[List[DLEConcept]] =
    Try {
      val cs = qexprToCS(qe)    // build up constraint sets
      val constraints = cs.get  // evaluate constraints

      val res =
        if (MyGlobal.strictQueryTyping) {
          // First Pass: Resolve constraints with placeholders.
          val before = resolve(constraints)

          // Check if placeholder constraints are satisfied by true argument types.
          before.foreach { case (v@Variable(vi), c) =>
            val mapped = Variable(s"?$vi")
            if (placeholders.contains(mapped)) {
              val test = Subsumed(placeholders(mapped), c)
              if (reasoner.prove(test))
                Unit
              else
                // TODO: Proper error reporting
                throw new RuntimeException("FAILURE " + test + " was not true!")
            }
          }

          // Second pass: Resolve constraints, but replace placeholders with argument types.
          resolve(constraints.map { case (v@Variable(vi), c) =>
            val mapped = Variable(s"?$vi")
            if (placeholders.contains(mapped))
              v -> placeholders(mapped)
            else
              v -> c
          })
        }
        else {
          // Resolve constraints, intersect placeholder types with their constraints.
          resolve(constraints.map { case (v@Variable(vi), c) =>
            val mapped = Variable(s"?$vi")
            if (placeholders.contains(mapped))
              v -> Intersection(c, placeholders(mapped))
            else
              v -> c
          })
        }

      // Check if types are satisfiable.
      if (!res.values.map { case x => reasoner.prove(Satisfiable(x)) }.forall(identity))
        throw new UnsatisfiableQueryError

      if (vs.isEmpty) // Nil == SELECT * ...
        res.toList.sortWith { case ((Variable(x1), _), (Variable(x2), _)) => x1 < x2 }.map(_._2)
      else
      // Select only variables present in vs.
        vs.map { case Var(x) => res(Variable(x)) }.toList
    }
}
