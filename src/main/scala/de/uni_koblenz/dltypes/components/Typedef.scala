package de.uni_koblenz.dltypes
package components

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.ast.TreeDSL
import scala.collection.mutable.{Set => MutSet}


class Typedef(val global: Global)
  extends PluginComponent with Transform with TypingTransformers with TreeDSL {
  import global._

  override val phaseName: String = "dl-typedef"
  override val runsAfter: List[String] = "dl-collect" :: Nil
  override val runsRightAfter = Some("dl-collect")

  override def newTransformer(unit: CompilationUnit): MyTransformer =
    new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
    val global: Typedef.this.global.type = Typedef.this.global

    def rowToCode(olst: Option[MutSet[String]]): List[global.Tree] = olst match {
      case Some(lst) =>
        lst.map { x => q"type ${newTypeName(x)} = DLType" }
           .toList
      case None => List()
    }

    override def transform(tree: Tree): Tree = tree match {
      case ClassDef(mods, name, tps, impl) =>
        val tdefs = rowToCode(MyGlobal.symbolTable.get(name.toString))
        atPos(tree.pos.makeTransparent)(
          ClassDef(mods, name, tps,
            Template(impl.parents, impl.self,
            List(impl.body.head) ++ tdefs ++ impl.body.tail))
        )
      case ModuleDef(mods, name, impl) =>
        val tdefs = rowToCode(MyGlobal.symbolTable.get(name.toString))
        atPos(tree.pos.makeTransparent)(
          ModuleDef(mods, name,
            Template(impl.parents, impl.self,
            List(impl.body.head) ++ tdefs ++ impl.body.tail))
        )
      case _ => super.transform(tree)
    }
  }
}
