package de.uni_koblenz.dltypes.backend

import de.uni_koblenz.dltypes.backend.DLTypesError.EitherDL
import de.uni_koblenz.dltypes.tools.DLEConcept

import scala.collection.{Set, mutable}


object MyGlobal {

  // Represent DL types.
  sealed trait AnyDLType
  case class InferredDLType(s: String) extends AnyDLType
  case class RegisteredDLType(s: String) extends AnyDLType
  case class UnionDLType(ss: List[AnyDLType]) extends AnyDLType

  val inferred_type_name = "InferredDLType"
  val registered_type_name = "RegisteredDLType"

  // Generator for query types.
  private object Gensym {
    private var c: Int = 0

    // Fresh registered type.
    def fresh(): String = {
      c += 1
      s"$registered_type_name$c"
    }

    // Fresh inferred type.
    def freshi(): String = {
      c += 1
      s"$inferred_type_name$c"
    }
  }

  // Generate and register a new DL type.
  def newDLType(dl: DLEConcept): String = {
    val t = Gensym.fresh()
    symbolTable += t
    dtypeTable += RegisteredDLType(t) -> Some(dl)
    t
  }

  // Generate and register DL types for a query with given arity.
  def newQueryType(arity: Int): List[String] = {
    (0 until arity).map { _ =>
      val t = Gensym.fresh()
      symbolTable += t
      dtypeTable += RegisteredDLType(t) -> None
      t
    }.toList
  }

  // Register the inferred and matching registered types.
  def newInferredDLType(): String = {
    val t = Gensym.freshi()
    symbolTable += t
    itypeTable += InferredDLType(t) -> (None, Set[DLEConcept]())
    t
  }

  def getPrefixes: String = {
    val t = prefixes map { pre =>
      s"PREFIX ${pre._1} <${pre._2}>"
    }
    t.fold("")(_ + "\n" + _)
  }

  // Register the concept dl to the variable v.
  // Returns 'false' if the variable was already assigned
  // (does not change the binding in this case).
  def register(v: AnyDLType, dl: DLEConcept): Boolean = { v match {
    case t @ RegisteredDLType(_) =>
      if (dtypeTable.get(t).isEmpty) false
      else {
        dtypeTable += t -> Some(dl)
        true
      }
    case _ => false // No registration for UnionDLTypes required
    }
  }

  // Lookup DL type name in symbol tables.
  def lookup(v: AnyDLType): EitherDL[DLEConcept] = v match {
    case t @ RegisteredDLType(_) => dtypeTable.get(t) match {
      case Some(Some(dl)) => Right(dl)
      case Some(None) => Left(InternalError(s"Encountered DL type, which was not yet defined ($t)"))
      case None => Left(InternalError(s"Encountered unseen DL type ($t)"))
    }
    case t @ InferredDLType(_) => itypeTable.getOrElse(t, (None, Set())) match {
      case (Some(concept), _) => Right(concept)
      case (None, set) =>
        if (set.isEmpty)
          Left(InternalError(s"Encountered unseen (explicit) inferred DL type ($t)"))
        else {
          val concept = DLEConcept.unionOf(set.toList)
          itypeTable += t -> (Some(concept), Set[DLEConcept]())
          Right(concept)
        }
    }
    case UnionDLType(xs) =>
      val ts = xs .map { x => lookup(x) }
      if (ts.exists(_.isLeft))
        Left(InternalError("Encountered wrong DL type in intersection type."))
      else
        Right(DLEConcept.unionOf(ts.map(_.right.get)))
  }

  def updateConstraints(i: InferredDLType, c: AnyDLType): EitherDL[Boolean] = {
    lookup(c).flatMap { concept =>
      itypeTable.getOrElse(i, (None, Set())) match {
        case (None, xs) =>
          itypeTable += i -> (None, Set(concept) ++ xs)
          Right(true)
        case _ => Right(false)
      }
    }
  }

  // Settings

  // This activates strict typing mode for queries,
  // where subsumption for constraints is checked vs the argument type.
  // This is not suitable for most practical applications.
  val strictQueryTyping = false

  // If strictQueryTyping is active, this setting regulates
  // whether warnings or errors are produced in (strict) error cases.
  // TODO: currently unused!
  val strictWarningsOnly = false

  // All names (DL types) that where encountered during the collector phase.
  // This is used to generate the type aliases for DL types.
  val symbolTable: mutable.Set[String] = mutable.Set()

  // Mapping from RegisteredDLType to respective DLEConcept.
  // These are the real concepts for the specified DL types. Might be None,
  // if it is part of a yet-to-infer SPARQL query type.
  private val dtypeTable:  mutable.Map[RegisteredDLType, Option[DLEConcept]] = mutable.Map()

  // Similar to qtypeTable, but for types that request explicitly to infer DL type
  // via the `???` constant.
  val itypeTable: mutable.Map[InferredDLType, (Option[DLEConcept], Set[DLEConcept])] = mutable.Map()

  // TODO: Document
  val prefixes: mutable.Map[String, String] = mutable.Map()
  var ontology: String = ""
}
