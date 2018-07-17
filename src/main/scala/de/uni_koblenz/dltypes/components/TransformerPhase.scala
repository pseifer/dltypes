package de.uni_koblenz.dltypes
package components

import de.uni_koblenz.dltypes.backend.{TypeChecker, Extractor, Globals, TypeError}
import de.uni_koblenz.dltypes.runtime.DLType
import de.uni_koblenz.dltypes.tools._

import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform
import scala.tools.nsc.transform.TypingTransformers


class TransformerPhase(val global: Global)
  extends PluginComponent with Transform with TypingTransformers with TreeDSL {
  import global._

  override val phaseName: String = "dl-transform"
  override val runsAfter: List[String] = "dl-typecase" :: Nil
  override val runsRightAfter = Some("dl-typecase")

  override def newTransformer(unit: CompilationUnit): MyTransformer =
    new MyTransformer(unit)


  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) with Extractor {
    val global: TransformerPhase.this.global.type = TransformerPhase.this.global

    val parser = new Parser
    val pp = new PrettyPrinter

    lazy val typeChecker = Globals.typeChecker

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

    def isAsk(pos: Position, s: List[String]): Boolean = {
      typeChecker.isAsk(s) match {
        case Left(errs) =>
          errs.foreach(e => reporter.error(pos, e.show))
          false
        case Right(b) => b
      }
    }

    // Returns true, if the list contains a simple query.
    def isSimpleQuery(parts: List[String]): Boolean = // TODO: Not ideal, fails e.g. on new lines
      """^\s*(?i)(PREFIX|SELECT|ASK|CONSTRUCT|DESCRIBE|BASE).*""".r
        .findPrefixMatchOf(parts.head)
        .isEmpty

    // Wraps a query (separated in parts) in such a way, that the result is
    // equivalent to the query: SELECT * WHERE { <q> }
    def wrapSimpleQuery(parts: List[String]): List[String] = {
      val t1 = ("SELECT * WHERE {" + parts.head) :: parts.tail
      t1.init ++ List(t1.last + "}")
    }

    def makeFinalQuery(q: List[String]): List[String] = {
      val t = Globals.getPrefixes ++ q.head :: q.tail
      t.map(_.replaceAll("\\\\n", " ")).map(_.replaceAll("\\n", " "))
    }

    def addTypeHint(q: List[String], opts: List[Boolean]): List[String] = {
      val encoded = opts.map {
        case true => "1"
        case false => "0"
      }.mkString("")
      if (q.nonEmpty)
        (encoded + "|" + q.head) :: q.tail
      else
        q
    }

    val dlModuleName = "DLTypeDefs" + this.unit.source.file.name

    override def transform(tree: Tree): Tree = tree match {

      // Add @dl annotations to explicitly annotated DL types
      case DLType(n) =>
        val dl = DLE.parse(n) match {
          case Right(c) =>
            if (Globals.typeChecker.notDefinedOrSatisfiable(c)
              && Globals.warnIfNotDefinedOrSatisfiable)
                reporter.warning(tree.pos, s"[DL] Annotated DL type is not satisfiable.")
            c
          case Left(err) =>
            reporter.error(tree.pos, s"[DL] Parser error in concept: $err")
            Bottom
        }
        tq"${typeOf[de.uni_koblenz.dltypes.runtime.DLType]} @dl(${dl.pretty()})"

      // Match the application of StringContext(<iri>).iri, i.e. iri"" literals
      // with type annotations iri"" : `:Type`
      case Typed(orig @ Apply(Select(Apply(obj, List(i)), m), Nil), tpe)
          if m.toString == "iri" && obj.toString == "StringContext" =>
        val pt = transform(tpe)
        if (Globals.doABoxReasoning) {
          val origP = transform(orig)
          reporter.echo("this it " + tpe + " " + pt)
          atPos(tree.pos.makeTransparent)(
            q"$origP : $pt"
          )
        }
        else {
          reporter.echo("this it " + tpe + " " + pt)
          atPos(tree.pos.makeTransparent)(
            q"$orig : $pt"
          )
        }

      // Match the application of StringContext(<iri>).iri, i.e. iri"" literals
      case orig @ Apply(Select(Apply(obj, List(i)), m), Nil)
          if m.toString == "iri" && obj.toString == "StringContext" =>
        if (Globals.doABoxReasoning) {
          val dl: DLEConcept = Nominal(":" + i.toString.filter(_ != '"')) // TODO: Properly parse the iri!
          atPos(tree.pos.makeTransparent)(
            q"$orig.asInstanceOf[${tq"${typeOf[de.uni_koblenz.dltypes.runtime.DLType]} @dl(${dl.pretty()})"}]"
          )
        }
        else {
          atPos(tree.pos.makeTransparent)(
            q"$orig.asInstanceOf[${tq"${typeOf[de.uni_koblenz.dltypes.runtime.DLType]} @dl(${Top.pretty()})"}]"
          )
        }

      // Match the application of StringContext(<query>).sparql
      // (i.e., sparql"" literals).
      case orig @ Apply(Select(Apply(obj, query), m), r)
        if (m.toString == "sparql" || m.toString == "strictsparql")
          && obj.toString == "StringContext" =>

        // Transform query tree to String.
        val temp = query.map(_.toString().stripSuffix("\"").stripPrefix("\""))

        // Test if this is a 'simple' query (i.e., needs to be wrapped in SELECT statement)
        val strQuery =
          if (isSimpleQuery(temp)) wrapSimpleQuery(temp)
          else temp

        // Finalize formatting and add prefixes.
        val finalQ = makeFinalQuery(strQuery)

        // Ask queries always return boolean.
        if (isAsk(tree.pos, finalQ)) {
          atPos(tree.pos.makeTransparent)(
            q"SparqlHelper(StringContext.apply(..${finalQ})).sparql(..$r).asInstanceOf[Boolean]"
          )
        }
        else {
          // Find arity of query.
          val arity = typeChecker.arity(finalQ)

          val opts = typeChecker.whichIsOptional(finalQ) match {
            case Left(es) =>
              es.foreach { e => reporter.error(tree.pos, e.show) }
              Nil
            case Right(lst) => lst
          }

          // Generate the new type for this query.
          val tpe = opts.map {
            case true => typeOf[Option[DLType]]
            case false => typeOf[DLType]
          }

          // Add encoded type (optional) annotation
          val hintedQ = addTypeHint(finalQ, opts)

          // If arity is too large for tupleN, report the error.
          if (arity > 22)
            reporter.error(tree.pos, TypeError(s"Maximal arity for SPARQL is 22, this query has arity $arity").show)
          // For arity one, use special sparql1 builder, as it needs to return List[IRI]
          if (arity == 1) {
            val tp = tq"List[${tpe.head}]"
            if (m.toString == "sparql")
              q"Sparql1Helper(StringContext.apply(..$hintedQ)).sparql1(..$r).asInstanceOf[$tp]"
            else
              q"Sparql1Helper(StringContext.apply(..$hintedQ)).strictsparql1(..$r).asInstanceOf[$tp]"
          }
          // Otherwise, use the normal sparql builder
          else {
            val tp = tq"List[${newTypeName(s"Tuple$arity")}[..${tpe}]]"
            if (m.toString == "sparql")
              q"SparqlHelper(StringContext.apply(..$hintedQ)).sparql(..$r).asInstanceOf[$tp]"
            else
              q"SparqlHelper(StringContext.apply(..$hintedQ)).strictsparql(..$r).asInstanceOf[$tp]"
          }
        }

      // Transform role projection to queries
      case Select(Ident(t), DLSelect(n)) =>
        // Generate the projection query.
        val newQuery = List("SELECT ?a WHERE {", s"${Util.decode(n)} ?a}")
        val finalQ = makeFinalQuery(newQuery)
        val hintedQ = addTypeHint(finalQ, List(false))
        // Generate SPARQL query.
        atPos(tree.pos.makeTransparent)(
          q"Sparql1Helper(StringContext.apply(..$hintedQ)).strictsparql1(${t.toTermName}).asInstanceOf[List[DLType]]"
        )

      case _ => super.transform(tree)
    }
  }
}
