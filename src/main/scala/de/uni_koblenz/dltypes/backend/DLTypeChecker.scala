package de.uni_koblenz.dltypes.backend

import java.io.File

import de.uni_koblenz.dltypes.backend.MyGlobal.AnyDLType
import de.uni_koblenz.dltypes.tools._


sealed trait TypingResult
case class OtherFailure(reason: DLTypesError) extends TypingResult
case class Result(tests: DLETruthy, error: Option[DLTypesError]) extends TypingResult {
  def success: Boolean = error.isEmpty
}
case object EmptyResult extends TypingResult


sealed trait CheckEntity
// TODO: Check doesn't take list anymore, as soon as Query types are fixed!!
case class CheckCovariant(lhs: AnyDLType, rhs: AnyDLType) extends CheckEntity
case class CheckContravariant(lhs: AnyDLType, rhs: AnyDLType) extends CheckEntity
case class CheckInvariant(lhs: AnyDLType, rhs: AnyDLType) extends CheckEntity
case class CheckEquality(lhs: AnyDLType, rhs: AnyDLType) extends CheckEntity


class DLTypeChecker {

  // ***** TYPING QUERIES

  // Lazy, since MyGlobal.ontologies is set by DLTypes.init (after
  // CheckerAnalyzerPlugin is initialized.
  val fi = new File(MyGlobal.ontology) // This...
  // ... is in normal execution guaranteed to be != Nil, since DLTypes.init fails if
  // no ontology is provided. If the phase is instantiated by other means
  // (e.g., in testing), this has to be set explicitly!
  // TODO: It might still be an invalid file, handle error.
  val reasoner = new ReasonerHermit(fi)

  val parser = new SPARQLParser

  val evaluator = new Evaluator(reasoner)

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

  def typeQuery(queryParts: List[String],
                arguments: List[Either[String, AnyDLType]],
                targets: List[AnyDLType],
                strict: Boolean): List[DLTypesError] = {
    // Initialize placeholder map.
    var placeholders: Map[Variable, DLEConcept] = Map()

    val query =
      if (arguments.isEmpty && queryParts.nonEmpty)
        Right(queryParts.head)
      else {
        val gensym = new Gensym(queryParts)
        val inner = arguments.map {
          case Right(t) =>
            val dl = MyGlobal.lookup(t)
            if (dl.isLeft)
              Left(dl.left.get)
            else {
              val v = gensym.fresh()
              placeholders += Variable(v) -> dl.right.get
              Right(v)
            }
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

    val ts = query.flatMap { q =>
      // Join the query with prefixes.
      val parsed = parser.parse(MyGlobal.getPrefixes + q) match {
        case Left(e) => Left(List(e))
        case Right(p) => Right(p)
      }
      parsed.flatMap {
        case AskQuery(_, _) => Right(Nil) // Nothing to do, ASK queries are already typed.
        case SelectQuery(_, vs, qe) =>
          evaluator.eval(vs, qe, placeholders, strict) match {
            case Left(e) => Left(List(e))
            case Right(t) => Right(t)
          }
      }
    }

    if (ts.isLeft)
      ts.left.get
    else {
      val assignResults = ts.right.get.zip(targets).map { case (tpe, target) =>
        MyGlobal.register(target, tpe)
      }
      if (assignResults.contains(false))
        List(InternalError("A DL type was already assigned."))
      else
        Nil
    }
  }


  // ***** TYPE CHECKING DL CONCEPTS

  private def interSat(t: AnyDLType): Option[OtherFailure] = {
    t match {
      case MyGlobal.UnionDLType(_) =>
        MyGlobal.lookup(t).map { lhsT =>
          val test = Satisfiable(lhsT)
          (lhsT, reasoner.prove(test))
        } match {
          case Left(e) => Some(OtherFailure(e))
          case Right((c, b)) if !b =>
            Some(OtherFailure(TypeError(s"Concept ${c.pretty} is not satisfiable")))
          case _ => None
        }
      case _ => None
    }
  }

  private def doTypeCheck(lhs: AnyDLType, rhs: AnyDLType,
                      testFn: (DLEConcept, DLEConcept) => DLETruthy): TypingResult =
    doCheck(lhs, rhs, testFn, { t =>
      TypeError(s"<${t.pretty}> was not true.")
    })

  private def doEqCheck(lhs: AnyDLType, rhs: AnyDLType): TypingResult =
    doCheck(lhs, rhs,
      { (l, r) => Satisfiable(Intersection(l, r)) },
      { _ => Warning(s"This equality check will always be false.")})

  private def doCheck(lhs: AnyDLType, rhs: AnyDLType,
                      testFn: (DLEConcept, DLEConcept) => DLETruthy,
                      resuFn: DLETruthy => DLTypesError): TypingResult = {

    // Check satisfiability for intersection dl types.
    val satLhs = interSat(lhs)
    val satRhs = interSat(rhs)
    if (satLhs.isDefined) satLhs.get
    else if (satRhs.isDefined) satRhs.get

    // If satisfiable (or no intersection type) proceed with type check.
    // Note: For unsatisfiable concept, type check would always fail anyways.
    else {
      MyGlobal.lookup(lhs).flatMap { lhsT =>
        MyGlobal.lookup(rhs).map { rhsT =>
          if (lhsT == rhsT)
            EmptyResult
          else {
            val test = testFn(lhsT, rhsT)
            val result = reasoner.prove(test)
            if (result)
              Result(test, None)
            else
              Result(test, Some(resuFn(test)))
          }
        }
      } match {
        case Left(v) => OtherFailure(v)
        case Right(v) => v
      }
    }
  }

  def check(e: CheckEntity): TypingResult = e match {
    case CheckCovariant(lhs, rhs) => doTypeCheck(lhs, rhs, Subsumed)
    case CheckContravariant(lhs, rhs) => doTypeCheck(rhs, lhs, Subsumed)
    case CheckInvariant(lhs, rhs) => doTypeCheck(lhs, rhs, ConceptEquality)
    case CheckEquality(lhs, rhs) => doEqCheck(lhs, rhs)
  }
}
