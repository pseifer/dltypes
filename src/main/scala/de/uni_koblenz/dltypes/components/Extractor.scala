package de.uni_koblenz.dltypes
package components


trait Extractor {
  import scala.reflect.internal.SymbolTable
  val global: SymbolTable
  import global._

  // Match DL type identifier against Tree and return String.
  object DLType {
    val h_char =
      List("$colon", "$hash",
        "$u22A4", "$bar",
        "$amp", "$bang",
        "$u2200", "$u2203",
        "$u00AC", "$u22A5")
    def isDLTypeHeuristic(s: String): Boolean = h_char.exists(s.contains)

    def unapply(tree: Tree): Option[String] = tree match {
      case Ident(n) if isDLTypeHeuristic(n.toString) => Some(n.toString)
      case _ => None
    }
  }

  object DLInference {
    def unapply(tree: Tree): Boolean = tree match {
      case Ident(n) => n.toString == "$qmark$qmark$qmark"
      case _ => false
    }
  }
}
