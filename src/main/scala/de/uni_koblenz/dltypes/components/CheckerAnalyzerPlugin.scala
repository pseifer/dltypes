package de.uni_koblenz.dltypes.components

import scala.tools.nsc.Global
import scala.tools.nsc.Mode
import scala.util.{Failure => UtilFailure, Success => UtilSuccess, Try => UtilTry}
import java.io.File

import de.uni_koblenz.dltypes.backend._
import de.uni_koblenz.dltypes.runtime.DLType


class CheckerAnalyzerPlugin(global: Global) {
  import global._
  import analyzer._

  // warnings: Emit (compiler) warnings on non-critical failures.
  // checks: Print +++CHECKING+++<...> messages for all DL type checks.
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

    // Get ontology (temporary hard coded) and instantiate reasoner.
    val fi = new File(getClass.getResource("/wine.rdf").getFile)
    val reasoner = new ReasonerHermit(fi)

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

    // Perform type check (...).
    def typeCheck(tpe: Type, pt: Type, tree: Tree, mode: Mode): Unit = {
      if (isDLType(tpe) && isDLType(pt)) {
        parseDL(tpe).flatMap { dltpe =>
          parseDL(pt).map { dlpt =>
            val test = Subsumed(dltpe, dlpt)
            reportCheck(test)
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
      // Todo: !dl && dl and dl && !dl cases.
    }

    // Get the type arguments (if any).
    def getTypeArgs(tpe: Type): List[Type] = {
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
      override def isActive(): Boolean = global.phase.id <= global.currentRun.typerPhase.id

      override def pluginsTyped(tpe: Type, typer: Typer, tree: Tree, mode: Mode, pt: Type): Type = {

        val tpeArgs = getTypeArgs(tpe)
        val ptArgs = getTypeArgs(pt)

        // TODO: Fix this! Temp: Cases were DLType is inferred need to be annotated explicitly.
        if (tpeArgs.map { t =>
            mode == Mode.TYPEmode && t == typeOf[DLType] && t.toString.split('.').last == "DLType"
          }.exists(identity))
          reporter.error(tree.pos, "[DL] Explicit use or inference of 'DLType' violates type safety."
            + "\nPossible solution: Declare more specific type (or top) explicitly.")

        if (tpeArgs.isEmpty && ptArgs.isEmpty)
          typeCheck(tpe, pt, tree, mode)
        else if (tpeArgs.size == 1 && ptArgs.size == 1)
          typeCheck(tpeArgs.head, ptArgs.head, tree, mode)
        else
          for ((tpe1, pt1) <- tpeArgs.zip(ptArgs)) {
            reporter.warning(tree.pos, "[DL] Using argument matching heuristic.")
            typeCheck(tpe1, pt1, tree, mode)
          }
        tpe
      }
    }
    addAnalyzerPlugin(ConcreteAnalyzerPlugin)
  }
}
