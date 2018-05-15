package de.uni_koblenz.dltypes.components

import de.uni_koblenz.dltypes.backend.{Extractor, MyGlobal}
import de.uni_koblenz.dltypes.tools._

import scala.tools.nsc.Global
import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}


class TypecaseTransformationPhase(val global: Global)
  extends PluginComponent with Transform with TypingTransformers with TreeDSL {
  import global._

  override val phaseName: String = "dl-typecase"
  override val runsAfter: List[String] = "parser" :: Nil
  override val runsRightAfter = Some("parser")

  override def newTransformer(unit: CompilationUnit): MyTransformer =
    new MyTransformer(unit)

  // Provide DLEConcept and DLERole instances for the 'Liftable' type class.

  private def recRoleLiftImpl(r: DLERole): Tree = {
    r match {
      case Role(r) => q"_root_.de.uni_koblenz.dltypes.tools.Role($r)"
      case Inverse(r) =>
        val t = recRoleLiftImpl(r)
        q"_root_.de.uni_koblenz.dltypes.tools.Inverse($t)"
      case Data(s) => q"_root_.de.uni_koblenz.dltypes.tools.Data($s)"
    }
  }

  implicit val liftrole = Liftable[DLERole] { r =>
    recRoleLiftImpl(r)
  }

  private def recDleLiftImpl(v: DLEConcept): Tree = {
    v match {
      case Variable(v) => q"_root_.de.uni_koblenz.dltypes.tools.Variable($v)"
      case Type(t) => q"_root_.de.uni_koblenz.dltypes.tools.Type($t)"
      case Top => q"_root_.de.uni_koblenz.dltypes.tools.Top"
      case Bottom => q"_root_.de.uni_koblenz.dltypes.tools.Bottom"
      case Nominal(n) => q"_root_.de.uni_koblenz.dltypes.tools.Nominal($n)"
      case Concept(s) => q"_root_.de.uni_koblenz.dltypes.tools.Concept($s)"
      case Existential(r, e) =>
        val t = recDleLiftImpl(e)
        q"_root_.de.uni_koblenz.dltypes.tools.Existential($r, $t)"
      case Universal(r, e) =>
        val t = recDleLiftImpl(e)
        q"_root_.de.uni_koblenz.dltypes.tools.Universal($r, $t)"
      case Negation(e) =>
        val t = recDleLiftImpl(e)
        q"_root_.de.uni_koblenz.dltypes.tools.Negation($t)"
      case Intersection(l, r) =>
        val t1 = recDleLiftImpl(l)
        val t2 = recDleLiftImpl(r)
        q"_root_.de.uni_koblenz.dltypes.tools.Intersection($t1, $t2)"
      case Union(l, r) =>
        val t1 = recDleLiftImpl(l)
        val t2 = recDleLiftImpl(r)
        q"_root_.de.uni_koblenz.dltypes.tools.Union($t1, $t2)"
    }
  }

  implicit val liftdle = Liftable[DLEConcept] { v =>
    recDleLiftImpl(v)
  }

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) with Extractor {
    val global: TypecaseTransformationPhase.this.global.type = TypecaseTransformationPhase.this.global

    val parser = new Parser

    def parseDL(tpt: String, tree: Tree): DLEConcept = {
      parser.parse(parser.dlexpr, Util.decode(tpt)) match {
        case parser.Success(m, _) => m.asInstanceOf[DLEConcept]
        case parser.NoSuccess(s, msg) =>
          reporter.error(tree.pos, s"[DL] Can't parse DL type: $s")
          Bottom
      }
    }

    /* Find cases where matching is done on DLType and add isSubsumed runtime checks.
    Rewrite:
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
    def transformCases(tree: Tree, c: CaseDef): Tree = {
      c match {
        // case x: `:RedWine` [if <guard>]* => <body>
        // (note that desugared ==)
        // case x @ (_: `:RedWine`) [if <guard>]* = <body>
        case CaseDef(Bind(name, Typed(Ident(termNames.WILDCARD), t @ DLType(tpt))), guard, body) =>
          val fresh = currentUnit.freshTermName()
          val newBody = super.transform(body)
          if (guard.isEmpty)
            cq"""$fresh: IRI if ${fresh.toTermName}.isSubsumed(${parseDL(tpt, tree)}) => {
                   val ${name.toTermName}: $t = $fresh;
                   ..$newBody }"""
          else
            cq"""$fresh: IRI if ${fresh.toTermName}.isSubsumed(${parseDL(tpt, tree)}) && ($guard) => {
                   val ${name.toTermName}: $t = $fresh;
                   ..$newBody }"""

        // case _: `:RedWine` [if <guard>]* => <body>
        // (note that desugared ==)
        // case (_: `:RedWine`) [if <guard>]* = <body>
        case CaseDef(Typed(Ident(termNames.WILDCARD), DLType(tpt)), guard, body) =>
          val newBody = super.transform(body)
          // Introduce fresh name, so isSubsumed check can be performed in guard.
          val name = currentUnit.freshTermName()
          if (guard.isEmpty)
            cq"$name: IRI if $name.isSubsumed(${parseDL(tpt, tree)}) => $newBody"
          else
            cq"$name: IRI if $name.isSubsumed(${parseDL(tpt, tree)}) && ($guard) => $newBody"

        // Otherwise, we don't care.
        case _ => c
      }
    }

    // Returns true, if the cases matches on type, which is also DL type.
    def isDL(c: CaseDef): Boolean = {
      c match {
        case CaseDef(Bind(_, Typed(Ident(termNames.WILDCARD), DLType(_))), _, _) => true
        case CaseDef(Typed(Ident(termNames.WILDCARD), DLType(_)), _, _) => true
        case _ => false
      }
    }

    override def transform(tree: Tree): Tree = tree match {
      // Transform type case expressions (see transformCases)
      case Match(l, cases) =>
        if (l.isEmpty) {
          if (cases.exists(isDL))
            reporter.error(tree.pos, "[DL] Can't handle implicit match for DL type. Use <var> => <var> match { ... } instead.")
          super.transform(tree)
        }
        else {
          val newCs = cases.map(transformCases(tree, _))
          super.transform(tree)
          atPos(tree.pos.makeTransparent)(
            q"$l match { case ..$newCs }"
          )
         }

      /* Match the application of isInstanceOf[<DLType>] and rewrite it to runtime DLType check:
      Rewrite:
          <expr>.isInstanceOf[<DLType>]
      To:
          { val t1 = <expr>    // avoid executing <expr> twice.
            t1.isInstanceOf[IRI] && { // check if is IRI
              val t2 = t1.asInstanceOf[IRI] // if so, cast to IRI
              t2.isSubsumed(<DLType) // the runtime DL type check
            } */
      case TypeApply(Select(q, name), List(DLType(tpt))) if name.toString == "isInstanceOf" =>
        // Generate fresh name.
        val fresh1 = currentUnit.freshTermName()

        atPos(tree.pos.makeTransparent)(
          q"""{ val $fresh1: Any = $q;
                $fresh1.isInstanceOf[IRI] &&
                  $fresh1.asInstanceOf[IRI].isSubsumed(${parseDL(tpt, tree)});
              }"""
        )

      case _ => super.transform(tree)
    }
  }
}
