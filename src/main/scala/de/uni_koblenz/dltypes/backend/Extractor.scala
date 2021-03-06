package de.uni_koblenz.dltypes.backend

trait Extractor {
  import scala.reflect.internal.SymbolTable
  val global: SymbolTable
  import global._

  private val h_char =
    List("$colon", "$hash",
      "$u22A4", "$bar",
      "$amp", "$bang",
      "$u2200", "$u2203",
      "$u00AC", "$u22A5")
  private def isDLTypeHeuristic(s: String): Boolean =
    h_char.exists(s.takeWhile(c => c != '_').contains)

  // Match DL type identifier against Tree and return String.
  object DLType {
    def unapply(tree: Tree): Option[String] = tree match {
      case Ident(n) if isDLTypeHeuristic(n.toString) => Some(n.toString)
      case _ => None
    }
  }

  // Match role projections.
  object DLSelect {
    def unapply(n: global.Name): Option[String] =
      if (isDLTypeHeuristic(n.toString))
        Some(n.toString)
      else
        None
  }

  // Match explicit DL inference.
  object DLInference {
    def unapply(tree: Tree): Boolean = tree match {
      case Ident(n) => n.toString == "$qmark$qmark$qmark"
      case _ => false
    }
  }
}
