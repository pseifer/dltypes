package de.uni_koblenz.dltypes.components

import scala.tools.nsc.Global
import scala.tools.nsc.Mode
import scala.util.{Try => UtilTry, Success => UtilSuccess, Failure => UtilFailure}

import java.io.File

import de.uni_koblenz.dltypes.backend._


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

    // Attempt to parse the DLEConcept representation of an DL type.
    def parseDL(s: String): UtilTry[DLEConcept] = {
      parser.parse(parser.dlexpr, s.split('.').last) match {
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
        reporter.echo("Inferred type (tpe): " + tpe + " : " + tpe.typeSymbol.toString)
        reporter.echo("Expected type (pt): " + pt + " : " + pt.typeSymbol.toString)
        reporter.echo("Typed by " + typer.toString + " in mode " + mode.toString)
        reporter.echo("---------\n" + tree.toString + "\n---------\n" + colorOff)
        reporter.flush()
      }
    }

    // Report what is checked by the reasoner.
    def reportCheck(dle: DLE): Unit = {
      if (printchecks) {
        reporter.echo(greenOn
          + "===[ CHECKED ]===  "
          + dle.toString
          + colorOff)
        reporter.flush()
      }
    }

    // Perform type check (...).
    def typeCheck(tpe: Type, pt: Type, tree: Tree, mode: Mode): Unit = {
      if (isDLType(tpe) && isDLType(pt)) {
        parseDL(tpe.toString).flatMap { dltpe =>
          parseDL(pt.toString).map { dlpt =>
            val test = Subsumed(dltpe, dlpt)
            reportCheck(test)
            (reasoner.prove(test), test)
          }
        } match {
          case UtilFailure(msg) =>
            reporter.error(tree.pos, "[DL] Type Parse Error: < "
              + msg + " >")
          case UtilSuccess((b, test)) =>
            if (!b) {
              debugError(tpe, pt, tree, mode)
              reporter.error(tree.pos, "[DL] Type Error: < "
                + test.toString + " > was not true.")
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
