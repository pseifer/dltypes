package de.uni_koblenz.dltypes
package components

import de.uni_koblenz.dltypes.backend.{Extractor, MyGlobal}
import de.uni_koblenz.dltypes.tools._

import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform
import scala.tools.nsc.transform.TypingTransformers


class CollectorPhase(val global: Global)
  extends PluginComponent with Transform with TypingTransformers with TreeDSL {
  import global._

  override val phaseName: String = "dl-collect"
  override val runsAfter: List[String] = "dl-typecase" :: Nil
  override val runsRightAfter = Some("dl-typecase")

  override def newTransformer(unit: CompilationUnit): MyTransformer =
    new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) with Extractor {
    val global: CollectorPhase.this.global.type = CollectorPhase.this.global

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

    def isAsk(ss: List[String]): Boolean = {
      val re = """ASK\s+\{""".r
      if (re.findFirstMatchIn(ss.head).isEmpty)
        false
      else
        true
    }

    def countVars(ss: List[String]): Int = {
      val reVar = """\?[^\s]""".r // Variables
      val reStar = """SELECT\s+\*""".r // SELECT *
      val reS = """SELECT(.*)WHERE""".r

      // Is not SELECT *, so take vars from SELECT block.
      if (reStar.findFirstMatchIn(ss.head).isEmpty) {
        reporter.echo(ss.head)
        reS.findFirstMatchIn(ss.head) match {
          case Some(x) => reVar.findAllIn(x.group(1)).toList.distinct.size
          case None => 0
        }
      }
      else {
        val s = ss.mkString(" ")
        reVar.findAllIn(s).toList.distinct.size
      }
    }

    // Returns true, if the list contains a simple query.
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
      // Match explicit DL type inference.
      case DLInference() =>
        val tpe = MyGlobal.newInferredDLType()
        Ident(newTypeName(tpe))

      // Match ordinary DL types and add them to the global symbol table for Typedef phase.
      case DLType(n) =>
        val tpe = MyGlobal.newDLType(parseDL(n, tree))
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

        // Transform query tree to String.
        val strQuery = query.map(_.toString().stripSuffix("\"").stripPrefix("\""))

        // Ask queries always return boolean.
        if (isAsk(strQuery)) {
          atPos(tree.pos.makeTransparent)(
            q"$orig.asInstanceOf[Boolean]"
          )
        }
        else {
          // Test if this is a 'simple' query (i.e., needs to be wrapped in SELECT statement).
          val wasSimple = isSimpleQuery(strQuery)
          // Wrap the query, if it was simple.
          val newQuery =
            if (wasSimple) wrapSimpleQuery(strQuery)
            else strQuery
          // Find arity of query.
          val arity = countVars(newQuery)
          // Generate the new type for this query.
          val tpe = MyGlobal.newQueryType(arity) // TODO!
          val tp =
            if (arity == 1) tq"List[${newTypeName(tpe.head)}]"
            else tq"List[${newTypeName(s"Tuple$arity")}[..${tpe.map(newTypeName)}]]"
          if (wasSimple) {
            // Reconstruct the tree with extended query.
            if (m.toString == "sparql")
              q"SparqlHelper(StringContext.apply(..$newQuery)).sparql(..$r).asInstanceOf[$tp]"
            else
              q"SparqlHelper(StringContext.apply(..$newQuery)).strictsparql(..$r).asInstanceOf[$tp]"
          }
          // For normal queries, just declare the type:
          else
            atPos(tree.pos.makeTransparent)(
              q"$orig.asInstanceOf[$tp]"
            )
          }

      // Match role projections.
      case Select(Ident(t), DLSelect(n)) =>
        // Generate new type for this query
        val tpe = MyGlobal.newQueryType(1).head
        // Generate the projection query.
        val newQuery = List("SELECT ?a WHERE {", s"${Util.decode(n)} ?a}")
        // Generate SPARQL query.
        atPos(tree.pos.makeTransparent)(
          q"SparqlHelper(StringContext.apply(..$newQuery)).strictsparql(${t.toTermName}).asInstanceOf[List[${newTypeName(tpe)}]]"
        )

      case _ => super.transform(tree)
    }
  }
}
