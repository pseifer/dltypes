package de.uni_koblenz.dltypes.backend

import de.uni_koblenz.dltypes.tools._


sealed trait TypingResult
case class OtherFailure(reason: DLTypesError) extends TypingResult
case class Result(tests: DLETruthy, error: Option[DLTypesError]) extends TypingResult {
  def success: Boolean = error.isEmpty
}
case object EmptyResult extends TypingResult


sealed trait CheckEntity
case class CheckCovariant(lhs: DLEConcept, rhs: DLEConcept) extends CheckEntity
case class CheckContravariant(lhs: DLEConcept, rhs: DLEConcept) extends CheckEntity
case class CheckInvariant(lhs: DLEConcept, rhs: DLEConcept) extends CheckEntity
case class CheckEquality(lhs: DLEConcept, rhs: DLEConcept) extends CheckEntity


class TypeChecker {

  // ***** TYPING QUERIES

  type Query = Either[List[SPARQLError], String]
  type ParsedQuery = Either[List[DLTypesError], SPARQLQuery]
  type Placeholders = Map[Variable, DLEConcept]

  val reasoner = new ReasonerHermit(Globals.ontology)

  val parser = new SPARQLParser

  val evaluator = new ConstraintResolver(reasoner)

  private class Gensym(val queryParts: List[String]) {
    val vs: String = queryParts.foldLeft("")(_ + _)
    private var c = 0
    def fresh(): String = {
      c += 1
      val s = s"?placeholderVariable$c"
      if (vs.contains(s))
        fresh()
      else
        s
    }
  }

  private def getMapping(tpe: String): Option[String] = {
    if (typeMap.contains(tpe))
      Some("\"\"^^" + typeMap(tpe))
    else
      None
  }

  private val typeMap: Map[String, String] =
    Map(
      "Int" -> "xsd:integer",
      "Float" -> "xsd:float",
      "Double" -> "xsd:double",
      "Boolean" -> "xsd:boolean",
      "String" -> "xsd:string"
    )

  private def parse(query: Query): ParsedQuery = {
    query.flatMap { q =>
      parser.parse(q) match {
        case Left(e) => Left(List(e))
        case Right(p) => Right(p)
      }
    }
  }

  private def constructQuery(queryParts: List[String]): Query =
    constructQuery(queryParts, List.fill(3)(Right(Top)))._1

  private def constructQuery(queryParts: List[String],
                             arguments: List[Either[String, DLEConcept]]): (Query, Placeholders)  = {

    var placeholders: Placeholders = Map()

    val query =
      if (arguments.isEmpty && queryParts.nonEmpty)
        Right(queryParts.head)
      else {
        val gensym = new Gensym(queryParts)
        val inner = arguments.map {
          case Right(dl) =>
            val v = gensym.fresh()
            placeholders += Variable(v) -> dl
            Right(v)
          case Left(t) => getMapping(t) match {
            case None => Left(SPARQLError(s"Argument of type $t is not allowed."))
            case Some(x) => Right(x)
          }
        }

        if (inner.exists(_.isLeft)) {
          Left(inner.filter(_.isLeft).map(_.left.get))
        }
        else {
          val rinner = inner.filter(_.isRight).map(_.right.get)
          Right(queryParts
            .drop(1)
            .zip(rinner)
            .map { case (s1, s2) => s2 + s1 }
            .foldLeft(queryParts.head)(_ + _))
        }
      }

    (query, placeholders)
  }

  def isAsk(queryParts: List[String]): Either[List[DLTypesError], Boolean] = {
    val parsed = parse(constructQuery(queryParts))
    parsed.map {
      case AskQuery(_, _) => true
      case SelectQuery(_, _, _) => false
    }
  }

  // Calculate the queries arity.
  def arity(queryParts: List[String]): Int = {
    val parsed = parse(constructQuery(queryParts))
    parsed.map {
      case AskQuery(_, _) => 0
      case SelectQuery(_, vs, _) => vs.length
    }.right.getOrElse(0)
  }

  // Returns a list of boolean values for all variables
  // the query returns, where 'true' means the variable
  // exists only in 'optional' part of the query and
  // 'false' the opposite.
  def whichIsOptional(queryParts: List[String]): Either[List[DLTypesError], List[Boolean]] = {
    val parsed = parse(constructQuery(queryParts))
    parsed.map {
      case AskQuery(_, _) => Nil
      case SelectQuery(_, vs, qe) =>
        val eOpt = QueryExpression.exclusiveOptionalVars(qe)
        vs.map { v =>
          if (eOpt.contains(v))
            true
          else
            false
        }.toList
    }
  }

  // Run type inference on the query consisting of 'queryParts'
  // and 'arguments', returning Errors or the appropriate Concepts.
  def typeQuery(queryParts: List[String],
                arguments: List[Either[String, DLEConcept]],
                strict: Boolean): Either[List[DLTypesError], List[DLEConcept]] = {

    val (query, placeholders) = constructQuery(queryParts, arguments)
    val parsed = parse(query)

    parsed.flatMap {
      case AskQuery(_, _) => Right(Nil) // Nothing to do, ASK queries are already typed.
      case SelectQuery(_, vs, qe) =>
        evaluator.eval(vs, qe, placeholders, strict) match {
          case Left(e) => Left(List(e))
          case Right(t) => Right(t)
        }
    }
  }

  // Check if concept is satisfiable.
  def notDefinedOrSatisfiable(dle: DLEConcept): Boolean = {
    !reasoner.isDefinedAndSatisfiable(dle)
  }

  // Return lub and its satisfiability.
  def lub(dls: List[DLEConcept]): DLEConcept = {
    DLEConcept.unionOf(dls)
  }

  // ***** TYPE CHECKING DL CONCEPTS


  private def doTypeCheck(lhs: DLEConcept, rhs: DLEConcept,
                      testFn: (DLEConcept, DLEConcept) => DLETruthy): TypingResult =
    doCheck(lhs, rhs, testFn, { t =>
      TypeError(s"<${t.pretty()}> was not true.")
    })

  private def doEqCheck(lhs: DLEConcept, rhs: DLEConcept): TypingResult =
    doCheck(lhs, rhs,
      { (l, r) => Satisfiable(Intersection(l, r)) },
      { _ => Warning(s"This equality check will always be false.")})

  private def doCheck(lhs: DLEConcept, rhs: DLEConcept,
                      testFn: (DLEConcept, DLEConcept) => DLETruthy,
                      resuFn: DLETruthy => DLTypesError): TypingResult = {

    if (lhs == rhs)
      EmptyResult
    else {
      val test = testFn(rhs, lhs)
      val result = reasoner.prove(test)
      if (result)
        Result(test, None)
      else
        Result(test, Some(resuFn(test)))
    }
  }

  def check(e: CheckEntity): TypingResult = e match {
    case CheckCovariant(lhs, rhs) => doTypeCheck(lhs, rhs, Subsumed)
    case CheckContravariant(lhs, rhs) => doTypeCheck(rhs, lhs, Subsumed)
    case CheckInvariant(lhs, rhs) => doTypeCheck(lhs, rhs, ConceptEquality)
    case CheckEquality(lhs, rhs) => doEqCheck(lhs, rhs)
  }
}
