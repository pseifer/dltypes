package de.uni_koblenz.dltypes.backend

import de.uni_koblenz.dltypes.tools._

import scala.util.Try


class QueryTyper(val reasoner: Reasoner) {
  val parser = new SPARQLParser
  val evaluator = new Evaluator(reasoner)

  // Create 'placeholder' variable that doesn't occur in query.
  private class Gensym(val queryParts: List[String]) {
    val vs = queryParts.foldLeft("")(_ + _)
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

  private def getMapping(tpe: String): String = {
    if (typeMap.contains(tpe))
      "\"\"^^" + typeMap(tpe)
    else
      throw new NoSuchXSDTypeError(tpe)
  }

  private val typeMap: Map[String, String] =
    Map(
      "Int" -> "xsd:integer",
      "Double" -> "xsd:double",
      "Boolean" -> "xsd:boolean",
      "String" -> "xsd:string"
    )

  def run(queryParts: List[String], arguments: List[Either[String, DLEConcept]], strict: Boolean): Try[List[DLEConcept]] = {
    var placeholders: Map[Variable, DLEConcept] = Map()
    val query =
      if (arguments.isEmpty && !queryParts.isEmpty)
        // If no arguments, queryPats.head contains entire query.
        queryParts.head
      else {
        val gensym = new Gensym(queryParts)
        val inner = arguments.map {
          case Right(t) =>
            val v = gensym.fresh()
            placeholders += Variable(v) -> t
            v
          case Left(t) => getMapping(t) // TODO: Proper error message if this fail.
        }

        // Build query with placeholders/xsd types for arguments.
        queryParts
          .drop(1)
          .zip(inner)
          .map { case (s1, s2) => s2 + s1 }
          .foldLeft(queryParts.head)(_+_)
      }
    val parsed = parser.parse(query)
    parsed.flatMap {
      case AskQuery(_, _) => throw new NotImplementedError // TODO: boolean
      case SelectQuery(_, vs, qe) => evaluator.eval(vs, qe, placeholders, strict)
    }
  }
}
