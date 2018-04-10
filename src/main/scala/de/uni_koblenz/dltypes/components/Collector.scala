package de.uni_koblenz.dltypes
package components

import de.uni_koblenz.dltypes.backend.MyGlobal
import de.uni_koblenz.dltypes.tools._

import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform
import scala.tools.nsc.transform.TypingTransformers


class Collector(val global: Global)
  extends PluginComponent with Transform with TypingTransformers with TreeDSL {
  import global._

  override val phaseName: String = "dl-collect"
  override val runsAfter: List[String] = "dl-typecase" :: Nil
  override val runsRightAfter = Some("dl-typecase")

  override def newTransformer(unit: CompilationUnit): MyTransformer =
    new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) with Extractor {
    val global: Collector.this.global.type = Collector.this.global

    val parser = new Parser
    val pp = new PrettyPrinter

    def parseDL(tpt: String, tree: Tree): DLEConcept = {
      parser.parse(parser.dlexpr, Util.decode(tpt)) match {
        case parser.Success(m, _) => m.asInstanceOf[DLEConcept]
        case parser.NoSuccess(s, msg) =>
          reporter.error(tree.pos, s"[DL] Can't parse DL type: $s")
          Bottom
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
        val tpe = MyGlobal.newDLType(parseDL(n, tree))
        Ident(newTypeName(tpe))

      // Match explicit DL type inference.
      case DLInference() =>
        val tpe = MyGlobal.newInferredDLType()
        Ident(newTypeName(tpe))

      // Match the application of StringContext(<iri>).iri to Nil (i.e., IRI"" literals)
      // and add asInstanceOf to declare the nominal type.
      case orig @ Apply(Select(Apply(obj, List(i)), m), Nil)
        if m.toString == "iri" && obj.toString == "StringContext" =>
        // TODO: Prefix is hard coded here, should be part of the IRI itself!
        val tpe = MyGlobal.newDLType(Nominal(":" + i.toString.filter(_ != '"')))
        atPos(tree.pos.makeTransparent)(
          q"$orig.asInstanceOf[${newTypeName(tpe)}]"
        )

      // Match the application of StringContext(<query>).sparql
      // (i.e., sparql"" literals).
      case orig @ Apply(Select(Apply(obj, query), m), r)
        if (m.toString == "sparql" || m.toString == "strictsparql")
          && obj.toString == "StringContext" =>
        // Generate the new type for this query.
        val tpe = MyGlobal.newSparqlQueryType()
        // Transform query tree to String.
        val strQuery = query.map(_.toString().stripSuffix("\"").stripPrefix("\""))
        // Test if this is a 'simple' query (i.e., needs to be wrapped in SELECT statement).
        if (isSimpleQuery(strQuery)) {
          val newQuery = wrapSimpleQuery(strQuery)
          atPos(tree.pos.makeTransparent)(
            // Reconstruct the tree with extended query.
            if (m.toString == "sparql")
              q"SparqlHelper(StringContext.apply(..$newQuery)).sparql(..$r).asInstanceOf[List[${newTypeName(tpe)}]]"
            else
              q"SparqlHelper(StringContext.apply(..$newQuery)).strictsparql(..$r).asInstanceOf[List[${newTypeName(tpe)}]]"
          )
        }
        // If not simple query extension was required, just set to generated query type and move on.
        else {
          atPos(tree.pos.makeTransparent)(
            q"$orig.asInstanceOf[List[${newTypeName(tpe)}]]"
          )
        }

      // Match role projections.
      case Select(Ident(t), DLSelect(n)) =>
        // Generate new type for this query
        val tpe = MyGlobal.newSparqlQueryType()
        // Generate the projection query.
        // TODO: Remove PREFIX part when internal prefix stuff is fixed!
        val newQuery = List("PREFIX : <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#> SELECT ?a WHERE {", s"${Util.decode(n)} ?a}")
        // Generate SPARQL query.
        atPos(tree.pos.makeTransparent)(
          q"SparqlHelper(StringContext.apply(..$newQuery)).strictsparql(${t.toTermName}).asInstanceOf[List[${newTypeName(tpe)}]]"
        )

      case _ => super.transform(tree)
    }
  }
}
