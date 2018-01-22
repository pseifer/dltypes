package de.uni_koblenz.dltypes
package components

import scala.util.{Try => UtilTry, Success => UtilSuccess, Failure => UtilFailure}


trait Extractor {
  import scala.reflect.internal.SymbolTable
  val global: SymbolTable
  import global._
  import backend._

  val parser = new Parser()

  def parseDL(s: String): UtilTry[DLEConcept] = {
    parser.parse(parser.dlexpr, s.split('.').last) match {
      case parser.Success(m, _) => UtilSuccess(m.asInstanceOf[DLEConcept])
      case parser.NoSuccess(msg, _) => UtilFailure(new Exception(msg))
    }
  }

  //def parseIndividual(s: String): UtilTry[DLEIndividual] = {
  //  parser.parse(parser.individual, s.split('.').last) match {
  //    case parser.Success(m, _) => UtilSuccess(m.asInstanceOf[DLEIndividual])
  //    case parser.NoSuccess(msg, _) => UtilFailure(new Exception(msg))
  //  }
  //}

  // Match DL types against Type.
  object DLType {
    def unapply(tpe: Type): Option[UtilTry[DLEConcept]] =
      if (tpe.typeSymbol.toString == "trait DLType")
        Some(parseDL(tpe.toString))
      else if (tpe.typeSymbol.toString() == "object IRI")
        Some(parseDL(tpe.toString()))
      else
        None
  }

  // Individual (IRI)
  //object DLIndividual {
  //  def unapply(tpe: Type): Option[UtilTry[DLEIndividual]] =
  //    if (tpe.typeSymbol.toString == "object IRI")
  //      Some(parseIndividual(tpe.toString))
  //    else
  //      None
  //}

  // Match DL types against Tree.
  object DLTypeTree {
    def unapply(tree: Tree): Option[UtilTry[DLEConcept]] =
      tree.tpe match {
        case DLType(tpe) => Some(tpe)
        case _ => None
      }
  }

  // Match applied types containing DL type against Tree.
  object AppliedDLTypeTree {
    def unapply(tree: Tree): Option[UtilTry[DLEConcept]] =
      tree.tpe match {
        case AppliedDLType(dle) => Some(dle)
        case _ => None
      }
  }

  // Match applied types containing DL type against Type.
  object AppliedDLType {
    def unapply(tpe: Type): Option[UtilTry[DLEConcept]] =
      tpe.dealias.typeArgs match {
        case List(DLType(dle)) => Some(dle)
        case List(AppliedDLType(dle)) => Some(dle)
        case _ => None
      }
  }

  // Match DL type identifier against Tree and return String.
  object DLTypeSimple {
    def unapply(tree: Tree): Option[String] = tree match {
      case Ident(n) if n.toString.contains("$colon") =>
        Some(n.toString)
      case _ => None
    }
  }
}
