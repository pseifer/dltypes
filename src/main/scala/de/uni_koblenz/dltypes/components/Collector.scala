package de.uni_koblenz.dltypes
package components

import scala.tools.nsc.ast.TreeDSL
import scala.collection.mutable.{Set => MutSet}

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform
import scala.tools.nsc.transform.TypingTransformers


// Global symbol table that is shared between the
// collector and Typedef phases.
object MyGlobal {
  val symbolTable: MutSet[String] = MutSet()
}


class Collector(val global: Global)
  extends PluginComponent with Transform with TypingTransformers with TreeDSL {
  import global._

  override val phaseName: String = "dl-collect"
  override val runsAfter: List[String] = "parser" :: Nil
  override val runsRightAfter = Some("parser")

  override def newTransformer(unit: CompilationUnit): MyTransformer =
    new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) with Extractor {
    val global: Collector.this.global.type = Collector.this.global

    override def transform(tree: Tree): Tree = tree match {
      // Looking for ordinary DL types.
      case DLTypeSimple(n) =>
        MyGlobal.symbolTable += n
        tree
      // Looking for the application of StringContext(<iri>).iri to List().
      case orig @ Apply(Select(Apply(obj, List(i)), m), List())
        if m.toString == "iri" && obj.toString == "StringContext" =>
        // Deduce type from iri.
        // TODO
        val newType: String = "{:" + i.toString.filter( _ != '"' ) + "}"
        MyGlobal.symbolTable += newType
        atPos(tree.pos.makeTransparent)(
          q"$orig.asInstanceOf[${newTypeName(newType)}]"
        )
      case _ =>
        super.transform(tree)
    }
  }
}
