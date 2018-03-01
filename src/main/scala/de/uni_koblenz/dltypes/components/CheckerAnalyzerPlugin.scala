package de.uni_koblenz.dltypes.components

import scala.tools.nsc.Global
import scala.tools.nsc.Mode
import scala.util.{Failure => UtilFailure, Success => UtilSuccess, Try => UtilTry}
import java.io.File

import de.uni_koblenz.dltypes.backend.ReasonerHermit
import de.uni_koblenz.dltypes.tools._
import de.uni_koblenz.dltypes.runtime.DLType


class CheckerAnalyzerPlugin(global: Global) {
  import global._
  import analyzer._

  // TODO: Separate TypeChecker & ErrorReporter classes
  // This should only contain the definition of the actual plugin.

  // warnings: Emit (compiler) warnings on non-critical failures.
  // printchecks: Print +++CHECKING+++<...> messages for all DL type checks.
  // debug: Print additional debug information on type errors and warnings.
  def add(warnings: Boolean = true,
          printchecks: Boolean = false,
          debug: Boolean = false): Unit = {

    // Instantiate the DL parser.
    val parser = new Parser

    val redOn = "\u001b[31m"
    val greenOn = "\u001b[32m"
    val yellowOn = "\u001b[33m"
    val blueOn = "\u001b[34m"
    val colorOff = "\u001b[0m"

    // Lazy, since MyGlobal.ontologies is set by DLTypes.init (after
    // CheckerAnalyzerPlugin is initialized.
    lazy val fi = new File(MyGlobal.ontologies.head) // This...
    // ... is in normal execution guaranteed to be != Nil, since DLTypes.init fails if
    // no ontology is provided. If the phase is instantiated by other means
    // (e.g., in testing), this has to be set explicitly!
    // TODO: It might still be an invalid file, handle error.
    lazy val reasoner = new ReasonerHermit(fi)

    // Extract the DL type name.
    def dlTypeName(tpe: Type): String = {
      if (tpe.kind == "NullaryMethodType")
        tpe.resultType.typeSymbolDirect.nameString
      else
        tpe.typeSymbolDirect.nameString
    }

    // Attempt to parse the DLEConcept representation of an DL type.
    def parseDL(tpe: Type): UtilTry[DLEConcept] = {
      val s = dlTypeName(tpe)
      parser.parse(parser.dlexpr, s) match {
        case parser.Success(m, _) => UtilSuccess(m.asInstanceOf[DLEConcept])
        case parser.NoSuccess(msg, _) => UtilFailure(new Exception(msg))
      }
    }

    // Test whether tpe is indeed a DL type.
    def isDLType(tpe: Type): Boolean = {
      (tpe.typeSymbol.toString == "trait DLType" ||
      //tpe.typeSymbol.toString == "object SPARQL" ||
      tpe.typeSymbol.toString == "object IRI") &&
      tpe.toString != "de.uni_koblenz.dltypes.runtime.DLType"
    }

    def stripQueryType(tpe: Type): String =
      tpe.toString.split('.').last

    // Test whether tpe is a sparql query type placeholder.
    def isQueryType(tpe: Type): Boolean = {
      isDLType(tpe) && stripQueryType(tpe).startsWith("SparqlQueryType")
    }

    // Report additional information, if debug mode is turned on.
    def debugError(tpe: Type, pt: Type, tree: Tree, mode: Mode): Unit = {
      if (debug) {
        reporter.echo(redOn + "===[  DEBUG  ]===")
        reporter.echo("Inferred type (tpe): " + tpe + " : " + tpe.typeSymbol)
        reporter.echo("Expected type (pt): " + pt + " : " + pt.typeSymbol)
        reporter.echo("Typed by " + typer + " in mode " + mode)
        reporter.echo("---------\n" + tree + "\n---------\n" + colorOff)
        reporter.flush()
      }
    }

    // Print subsumed relationship in a very fancy manner.
    def formatSubsumed(dle: DLE): String = dle match {
      case Subsumed(l, r) =>
        l.toString + " ⊏ " + r.toString
      case _ => ""
    }

    // Report what is checked by the reasoner.
    def reportCheck(dle: DLE): Unit = {
      if (printchecks) {
        reporter.echo(greenOn
          + "===[ CHECKED ]===  "
          + formatSubsumed(dle)
          + colorOff)
        reporter.flush()
      }
    }

    // Perform DL type checks.
    def typeCheck(tpe: Type, pt: Type, tree: Tree, mode: Mode, warnH: Boolean = false): Unit = {
      // If both are DL types, do subsumed(tpe, pt) check.
      if (isDLType(tpe) && isDLType(pt)) {
        parseDL(tpe).flatMap { dltpe =>
          parseDL(pt).map { dlpt =>
            val test = Subsumed(dltpe, dlpt)
            reportCheck(test)
            if (warnH) reporter.warning(tree.pos, "[DL] Using argument matching heuristic.")
            (reasoner.prove(test), test)
          }
        } match {
          case UtilFailure(msg) =>
            reporter.error(tree.pos, "[DL] Type Parse Error: <" + msg + ">")
          case UtilSuccess((b, test)) =>
            if (!b) {
              debugError(tpe, pt, tree, mode)
              reporter.error(tree.pos, "[DL] Type Error: <"
                + formatSubsumed(test) + "> was not true.")
            }
        }
      }
      else if (isDLType(tpe) && !isDLType(pt) && pt.isFinalType && !(pt == typeOf[Nothing]))
        reporter.error(tree.pos, "[DL] Type Error: Incompatible DLType " + tpe + " with " + pt)
      else if (isDLType(pt) && !isDLType(tpe) && tpe.isFinalType && !(tpe == typeOf[Nothing]))
        reporter.error(tree.pos, "[DL] Type Error: Incompatible " + tpe + " with DLType " + pt)
    }

    // Get the type arguments (if any).
    def getTypeArgs(tpe: Type): List[Type] = {
      // Variable are nullary methods, therefore we might
      // need to look at the result type here.
      if (tpe.kind == "NullaryMethodType")
        tpe.resultType match {
          case TypeRef(_, _, args) => args
          case _ => Nil
        }
      else
        tpe match {
          case TypeRef(_, _, args) => args
          case _ => Nil
        }
    }

    object ConcreteAnalyzerPlugin extends AnalyzerPlugin {
      // Run during 'typer' phase.
      override def isActive(): Boolean = global.phase.id <= global.currentRun.typerPhase.id

      override def pluginsTyped(tpe: Type, typer: Typer, tree: Tree, mode: Mode, pt: Type): Type = {
        val tpeArgs = getTypeArgs(tpe)
        val ptArgs = getTypeArgs(pt)

        // SPARQL queries ...
        if (tpe.kind == "NullaryMethodType" && pt.isWildcard &&
            (tpe.resultType match {
              case TypeRef(_, b, _) =>
                b.toString.startsWith("type SparqlQueryType")
              case _ => false })) {

            reporter.echo("TPE " + tpe.toString + tpe.kind)
            reporter.echo(tpe.resultType.toString)
            reporter.echo("TP " + pt.toString)
            reporter.echo(tree.toString + "\n\n\n")
            tree match {
              //                                       SparqlHelper  apply             sparql           instanceOf
              case TypeApply(Select(Apply(Select(Apply(_, List(Apply(_, queryParts))), _), sparqlArgs), _), List(sqt)) =>
                val uSqt = sqt.toString.split('.').last
                reporter.echo("queryParts " + queryParts)
                reporter.echo("sparqlArgs " + sparqlArgs)
                reporter.echo("  " + sparqlArgs.head)
                reporter.echo("  " + sparqlArgs.head.tpe)
                reporter.echo("SparqlQueryTypeX " + uSqt)
                reporter.echo("Registered as " + MyGlobal.qtypeTable(uSqt).toString)
                MyGlobal.qtypeTable.update(uSqt,
                  Some(QueryTyper.withArgs(
                    queryParts.map(_.toString),
                    sparqlArgs.map(_.toString))))
              case _ => reporter.error(tree.pos, "[DL] Internal Error: Malformed query type.")
            }
          }

          // IDEA
          // Match the case:
          // TPE => List[DLTypeDefs.SparqlQueryType1]
          // TP ?
          // Then tree is: de.uni_koblenz.dltypes.runtime.Sparql.SparqlHelper(scala.StringContext.apply("SELECT ?x WHERE { ?x a ", " }")).sparql(Main.this.s).asInstanceOf[List[DLTypeDefs.SparqlQueryType1]]

          // This should be earliest occurence of SparqlQueryTypeX TODO: Verify this
          // Then deduce type with QueryTyper and add to lookup table from SparqlQueryTypes => Option[DLEConcept]

        // TODO
        //if (isQueryType(tpe) && isQueryType((pt))) {
        //  val tpeQ = MyGlobal.qtypeTable.get(stripQueryType(tpe))
        //  val ptQ = MyGlobal.qtypeTable.get(stripQueryType(pt))
        //}

        // Cases were DLType is inferred (or explicitly used) need to be declared
        // explicitly with either a more specific type, or Top (⊤).
        if (tpeArgs.map { t =>
            mode == Mode.TYPEmode && t == typeOf[DLType] && t.toString.split('.').last == "DLType"
          }.exists(identity))
          reporter.error(tree.pos, "[DL] Explicit use or inference of 'DLType' violates type safety."
            + "\nPossible solution: Declare more specific type or use ⊤ explicitly.")

        // No type parameters (correct).
        if (tpeArgs.isEmpty && ptArgs.isEmpty)
          typeCheck(tpe, pt, tree, mode)
        // Exactly one (correct).
        else if (tpeArgs.size == 1 && ptArgs.size == 1)
          typeCheck(tpeArgs.head, ptArgs.head, tree, mode)
        // More than one type parameter (might be wrong)...
        else {
          // ...if not the same class. Issue warning in this case.
          for ((tpe1, pt1) <- tpeArgs.zip(ptArgs)) {
            typeCheck(tpe1, pt1, tree, mode, warnH = tpe.baseClasses != pt.baseClasses)
          }
        }
        tpe
      }
    }
    addAnalyzerPlugin(ConcreteAnalyzerPlugin)
  }
}
