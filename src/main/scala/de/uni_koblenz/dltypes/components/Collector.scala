package de.uni_koblenz.dltypes
package components

import scala.collection.mutable
import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform
import scala.tools.nsc.transform.TypingTransformers


object MyGlobal {
  val symbolTable: mutable.Set[String] = mutable.Set()
  var ontologies: mutable.MutableList[String] = mutable.MutableList()
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

    /*Rewrite:
    x match {
      case y: `:RedWine` if <guard> => <body0 with y>
      case y: `:RedWine` => <body1 with y>
      case _: `:RedWine` => <body2>
      case y: Int if <guard> => <body3 with y>
      case y: Int => <body4 with y>
      case _: Int => <body5>
      case _ => <body6>
    }
    To:
    x match {
      case y: IRI if y.isSubsumed(":RedWine") && <guard> =>
        lazy val y: `:RedWine` = y.asInstanceOf[`:RedWine`]
        <body0 with y>
      case y: IRI if y.isSubsumed(":RedWine") =>
        lazy val y: `:RedWine` = y.asInstanceOf[`:RedWine`]
        <body0 with y>
      case $fresh: IRI if $fresh.isSubsumed(":RedWine") =>
        <body2>
      case y: Int if <guard> => <body3 with y>
      case y: Int => <body4 with y>
      case _: Int => <body5>
      case _ => <body6>
    }*/
    def transformCases(c: CaseDef): Tree = {
      c match {
        // case x: `:RedWine` [if <guard>]* => <body>
        // (note that desugared ==)
        // case x @ (_: `:RedWine`) [if <guard>]* = <body>
        case CaseDef(Bind(name, Typed(Ident(termNames.WILDCARD), DLType(tpt))), guard, body) =>
          if (guard.isEmpty)
            cq"$name: IRI if ${name.toTermName}.isSubsumed($tpt) => $body"
          else
            cq"$name: IRI if ${name.toTermName}.isSubsumed($tpt) && ($guard) => $body"

        // case _: `:RedWine` [if <guard>]* => <body>
        // (note that desugared ==)
        // case (_: `:RedWine`) [if <guard>]* = <body>
        case CaseDef(Typed(Ident(termNames.WILDCARD), DLType(tpt)), guard, body) =>
          // Introduce fresh name, so isSubsumed check can be performed in guard.
          val name = currentUnit.freshTermName()
          if (guard.isEmpty)
            cq"$name: IRI if $name.isSubsumed($tpt) => $body"
          else
            cq"$name: IRI if $name.isSubsumed($tpt) && ($guard) => $body"

        // Otherwise, we don't care.
        case _ => c
      }
    }

    override def transform(tree: Tree): Tree = tree match {
      // Match ordinary DL types and add them to the global symbol table for Typedef phase.
      case DLType(n) =>
        MyGlobal.symbolTable += n
        tree

      // Transform type case expressions (see transformCases)
      case Match(l, cases) =>
        val newCs = cases.map(transformCases)
        super.transform(tree)
        atPos(tree.pos.makeTransparent)(
          q"$l match { case ..$newCs }"
        )

      // Match the application of isInstanceOf[<DLType>] and rewrite it to runtime DLType check:
      /*Rewrite:
          <expr>.isInstanceOf[<DLType>]
      To:
          { val t1 = <expr>    // avoid executing <expr> twice.
            t1.isInstanceOf[IRI] && { // check if is IRI
              val t2 = t1.asInstanceOf[IRI] // if so, cast to IRI
              t2.isSubsumed(<DLType) // the runtime DL type check
            }
      */
      case TypeApply(Select(q, name), List(DLType(tpt))) if name.toString == "isInstanceOf" =>
        // Generate two fresh names.
        val name1 = currentUnit.freshTermName()
        val name2 = currentUnit.freshTermName()
        atPos(tree.pos.makeTransparent)(
          q"""{ val $name1: Any = $q;
                $name1.isInstanceOf[IRI] && {
                  val $name2 = $name1.asInstanceOf[IRI];
                  $name2.isSubsumed($tpt)
                }
              }"""
        )

      // Match the application of StringContect(<iri>).iri to Nil (i.e., IRI"" literals)
      // and add asInstanceOf to declare the nominal type.
      case orig @ Apply(Select(Apply(obj, List(i)), m), Nil)
        if m.toString == "iri" && obj.toString == "StringContext" =>
        val newType: String = "{:" + i.toString.filter( _ != '"' ) + "}"
        MyGlobal.symbolTable += newType
        atPos(tree.pos.makeTransparent)(
          q"$orig.asInstanceOf[${newTypeName(newType)}]"
        )

      case _ => super.transform(tree)
    }
  }
}
