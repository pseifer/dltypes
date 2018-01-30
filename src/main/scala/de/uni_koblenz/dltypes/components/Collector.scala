package de.uni_koblenz.dltypes
package components

import de.uni_koblenz.dltypes.runtime.DLType

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.ast.TreeDSL
import scala.collection.mutable.{Map => MutMap, Set => MutSet, Stack => MutStack}

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{ Transform, TypingTransformers }
import scala.tools.nsc.symtab.Flags
import scala.tools.nsc.transform.TypingTransformers

object MyGlobal {
  val symbolTable: MutMap[String, MutSet[String]] =
    MutMap[String, MutSet[String]]()
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

    var context: MutStack[String] = MutStack() // Status

    def addToContext(n: String): Unit = {
      MyGlobal.symbolTable.get(context.top) match {
        case Some(lst) =>
          MyGlobal.symbolTable.update(context.top, lst ++ MutSet(n))
        case _ =>
          MyGlobal.symbolTable += (context.top -> MutSet(n))
      }
    }

    override def transform(tree: Tree): Tree = tree match {
      case ClassDef(_, name, _, _) =>
        if (context.isEmpty) { // Top level class
          context.push(name.toString)
          val t = super.transform(tree)
          context.pop
          t
        }
        else super.transform(tree)
      case ModuleDef(_, name, _) =>
        if (context.isEmpty) { // Top level object
          context.push(name.toTypeName.toString)
          val t = super.transform(tree)
          context.pop // forget context (in case this is a local object)
          t
        }
        else super.transform(tree)
      case DLTypeSimple(n) =>
        addToContext(n)
        tree
      // Looking for the application of StringContext(<iri>).iri to List()
      case orig @ Apply(Select(Apply(obj, List(i)), m), List())
        if m.toString == "iri" && obj.toString == "StringContext" =>
        // Deduce type from iri.
        // TODO
        val newType: String = "{:" + i.toString.filter( _ != '"' ) + "}"
        addToContext(newType)
        atPos(tree.pos.makeTransparent)(
          q"$orig.asInstanceOf[${newTypeName(newType)}]"
        )
      case _ =>
        super.transform(tree)
    }
  }
}
