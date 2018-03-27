package de.uni_koblenz.dltypes
package components

import de.uni_koblenz.dltypes.backend.MyGlobal
import de.uni_koblenz.dltypes.tools.PrettyPrinter

import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform
import scala.tools.nsc.transform.TypingTransformers
import scala.util.matching.Regex


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

    val pp = new PrettyPrinter

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
    def transformCases(c: CaseDef): Tree = {
      c match {
        // case x: `:RedWine` [if <guard>]* => <body>
        // (note that desugared ==)
        // case x @ (_: `:RedWine`) [if <guard>]* = <body>
        case CaseDef(Bind(name, Typed(Ident(termNames.WILDCARD), t @ DLType(tpt))), guard, body) =>
          val fresh = currentUnit.freshTermName()
          if (guard.isEmpty)
            cq"""$fresh: IRI if ${fresh.toTermName}.isSubsumed($tpt) => {
                   val ${name.toTermName}: $t = $fresh;
                   ..$body }"""
          else
            cq"""$fresh: IRI if ${fresh.toTermName}.isSubsumed($tpt) && ($guard) => {
                   val ${name.toTermName}: $t = $fresh;
                   ..$body }"""

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

    // Returns true, if the cases matches on type, which is also DL type.
    def isDL(c: CaseDef): Boolean = {
      c match {
        case CaseDef(Bind(_, Typed(Ident(termNames.WILDCARD), DLType(_))), _, _) => true
        case CaseDef(Typed(Ident(termNames.WILDCARD), DLType(_)), _, _) => true
        case _ => false
      }
    }

    // Returns true, if the list
    def isSimpleQuery(parts: List[String]): Boolean =
      """^\s*(?i)(PREFIX|SELECT|ASK|CONSTRUCT|DESCRIBE|BASE).*""".r
        .findPrefixMatchOf(parts.head)
        .isEmpty

    // Wraps a query (separated in parts) in such a way, that the result is
    // equivalent to the query: SELECT * WHERE { <q> }
    def wrapSimpleQuery(parts: List[String]): List[String] = {
      val t1 = ("SELECT * WHERE {" + parts.head) :: parts.tail
      t1.init ++ List(t1.last + "}")
    }

    override def transform(tree: Tree): Tree = tree match {
      // Match ordinary DL types and add them to the global symbol table for Typedef phase.
      case DLType(n) =>
        MyGlobal.symbolTable += n
        tree

      // Match explicit DL type inference.
      case DLInference() =>
        val tpe = MyGlobal.newInferredDLType()
        Ident(newTypeName(tpe))

      // Transform type case expressions (see transformCases)
      case Match(l, cases) =>
        if (l.isEmpty) {
          if (cases.exists(isDL))
            reporter.error(tree.pos, "[DL] Can't handle implicit match for DL type. Use <var> => <var> match { ... } instead.")
          super.transform(tree)
        }
        else {
          reporter.echo("IS EMPTY")
          val newCs = cases.map(transformCases)
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
        // TODO: Check if tpt can be parsed, raise error if not.
        atPos(tree.pos.makeTransparent)(
          q"""{ val $fresh1: Any = $q;
                $fresh1.isInstanceOf[IRI] &&
                  $fresh1.asInstanceOf[IRI].isSubsumed($tpt);
              }"""
        )

      // Match the application of StringContext(<iri>).iri to Nil (i.e., IRI"" literals)
      // and add asInstanceOf to declare the nominal type.
      case orig @ Apply(Select(Apply(obj, List(i)), m), Nil)
        if m.toString == "iri" && obj.toString == "StringContext" =>
        val newType: String = "{:" + i.toString.filter( _ != '"' ) + "}"
        MyGlobal.symbolTable += newType
        atPos(tree.pos.makeTransparent)(
          q"$orig.asInstanceOf[${newTypeName(newType)}]"
        )

      // Match the application of StringContext(<query>).sparql
      // (i.e., sparql"" literals).
      case orig @ Apply(Select(Apply(obj, query), m), r)
        if m.toString == "sparql" && obj.toString == "StringContext" =>
        // Generate the new type for this query.
        val tpe = MyGlobal.newSparqlQueryType()
        // Transform query tree to String.
        val strQuery = query.map(_.toString().stripSuffix("\"").stripPrefix("\""))
        // Test if this is a 'simple' query (i.e., needs to be wrapped in SELECT statement).
        if (isSimpleQuery(strQuery)) {
          val newQuery = wrapSimpleQuery(strQuery)
          atPos(tree.pos.makeTransparent)(
            // Reconstruct the tree with extended query.
            q"SparqlHelper(StringContext.apply(..$newQuery)).sparql(..$r).asInstanceOf[List[${newTypeName(tpe)}]]"
          )
        }
        // If not simple query extension was required, just set to generated query type and move on.
        else {
          atPos(tree.pos.makeTransparent)(
            q"$orig.asInstanceOf[List[${newTypeName(tpe)}]]"
          )
        }

      case _ => super.transform(tree)
    }
  }
}
