package de.uni_koblenz.dltypes
package components

import de.uni_koblenz.dltypes.backend.MyGlobal

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.ast.TreeDSL


class TypedefPhase(val global: Global)
  extends PluginComponent with Transform with TypingTransformers with TreeDSL {
  import global._

  override val phaseName: String = "dl-typedef"
  override val runsAfter: List[String] = "dl-collect" :: Nil
  override val runsRightAfter = Some("dl-collect")

  override def newTransformer(unit: CompilationUnit): MyTransformer =
    new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
    val global: TypedefPhase.this.global.type = TypedefPhase.this.global

    val dlPackageInsert =
      q"import de.uni_koblenz.dltypes.runtime._" ::
      q"import de.uni_koblenz.dltypes.runtime.Sparql._" ::
      MyGlobal.symbolTable.map { x =>
        q"type ${newTypeName(x)} = DLType"
        //q"class ${newTypeName(x)} extends DLType"
      }.toList

    val dlModuleName = "DLTypeDefs"

    override def transform(tree: Tree): Tree = tree match {
      // Attach the DLTypes module to the empty (top level) package.
      case PackageDef(n @ Ident(nme.EMPTY_PACKAGE_NAME), stats) =>
        val stats1 = atOwner(tree.symbol.moduleClass)(transformStats(stats, currentOwner))
        val moduleTree = ModuleDef(NoMods, newTermName(dlModuleName), Template(List(), noSelfType, dlPackageInsert))
        PackageDef(n, stats1 ++ Seq(moduleTree))
      // Import the DLTypes module (including all type defs) into all Classes...
      case ClassDef(mods, name, tps, impl) =>
        atPos(tree.pos.makeTransparent)(
          ClassDef(mods, name, tps,
            Template(impl.parents, impl.self,
              List(impl.body.head) ++ List(q"import DLTypeDefs._") ++ impl.body.tail))
        )
      // ...and modules.
      case ModuleDef(mods, name, impl) =>
        atPos(tree.pos.makeTransparent)(
          ModuleDef(mods, name,
            Template(impl.parents, impl.self,
            List(impl.body.head) ++ List(q"import DLTypeDefs._") ++ impl.body.tail))
        )
      case _ => super.transform(tree)
    }
  }
}
