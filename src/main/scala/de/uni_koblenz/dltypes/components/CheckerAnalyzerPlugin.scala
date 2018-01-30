package de.uni_koblenz.dltypes.components

import scala.tools.nsc.Global
import scala.tools.nsc.Mode
import scala.util.{Try => UtilTry, Success => UtilSuccess, Failure => UtilFailure}

import java.io.File

import de.uni_koblenz.dltypes.backend._


class CheckerAnalyzerPlugin(global: Global) {
  import global._
  import analyzer._

  def add(): Unit = {
    val parser = new Parser

    val fi = new File(getClass.getResource("/wine.rdf").getFile)
    val reasoner = new ReasonerHermit(fi)

    def parseDL(s: String): UtilTry[DLEConcept] = {
      parser.parse(parser.dlexpr, s.split('.').last) match {
        case parser.Success(m, _) => UtilSuccess(m.asInstanceOf[DLEConcept])
        case parser.NoSuccess(msg, _) => UtilFailure(new Exception(msg))
      }
    }

    object ConcreteAnalyzerPlugin extends AnalyzerPlugin {
      override def isActive(): Boolean = global.phase.id <= global.currentRun.typerPhase.id

      override def pluginsPt(pt: Type, typer: Typer, tree: Tree, mode: Mode): Type = {
        pt
      }

      override def pluginsTyped(tpe: Type, typer: Typer, tree: Tree, mode: Mode, pt: Type): Type = {
        // If DL type, perform type check.
        var error = false
        if ((tpe.typeSymbol.toString == "trait DLType" || tpe.typeSymbol.toString == "object IRI") && (tpe.toString != "de.uni_koblenz.dltypes.runtime.DLType")) {
          parseDL(tpe.toString) match {
            case UtilFailure(msg) =>
              reporter.error(tree.pos, "[DL] Type Error (1) " + msg)
              error = true
            case UtilSuccess(dltpe) =>
              if (!pt.isWildcard) {
                parseDL(pt.toString) match {
                  case UtilFailure(msg) =>
                    reporter.error(tree.pos, "[DL] Type Error (2) " + msg)
                    error = true
                  case UtilSuccess(dlpt) =>
                    if (reasoner.prove(Subsumed(dltpe, dlpt))) {
                      reporter.echo("\u001b[34m++CHECKED++ " + Subsumed(dltpe, dlpt).toString + "\u001b[0m")
                    } else {
                      reporter.error(tree.pos, "[DL] Type error (3) " + Subsumed(dltpe, dlpt).toString + " was not true.")
                      error = true
                    }
                }
              }
          }
        }

        if (error) {
          reporter.echo("\n\n=== [\u001b[31m")
          reporter.echo("Inferred type (tpe): " + tpe + " : " + tpe.typeSymbol.toString)
          reporter.echo("Expected type (pt): " + pt + " : " + pt.typeSymbol.toString)
          reporter.echo("Typed by " + typer.toString +" in mode " + mode.toString)
          reporter.echo("----------\n" + tree.toString + "\n---------\n")
          reporter.echo("\u001b[0m] ============================")
          reporter.flush()
        }
        //if (tpe.isError) {
        //  //reporter.echo("!! TYPE ERROR HAPPENED !!")
        //  //reporter.echo("was " + tree.toString.drop(1).dropRight(8))
        //  //reporter.echo("tree before" + tree.tpe.isError)
        //  tree.setType(typeOf[DLType])
        //  //reporter.echo("tree after" + tree.tpe.isError)
        //  //reporter.echo("----------\n" + tree.toString + "\n---------\n")
        //  // Check if DL type.
        //  // If so:
        //  typeOf[DLType]
        //}
        //else {
        //  //if (tpe.toString().split('.').last.contains(':'))
        //  //  reporter.echo("!!!!DL!!!!")
        //  //reporter.echo("Not subst DLType -> " + tpe.toString)
        //  tpe
        //}
        tpe
      }

      // override def pluginsTypeSig(tpe: Type, typer: Typer, defTree: Tree, pt: Type): Type = {
      //   tpe
      // }

      // override def pluginsTypeSigAccessor(tpe: Type, typer: Typer, tree: ValDef, sym: Symbol): Type = {
      //   tpe
      // }

      // override def canAdaptAnnotations(tree: Tree, typer: Typer, mode: Mode, pt: Type): Boolean = false

      // override def adaptAnnotations(tree: Tree, typer: Typer, mode: Mode, pt: Type): Tree = {
      //   tree
      // }

      // override def pluginsTypedReturn(tpe: Type, typer: Typer, tree: Return, pt: Type): Type = {
      //  tpe
      //}

      //override def pluginsNotifyImplicitSearch(search: ImplicitSearch): Unit = ()

      //override def pluginsNotifyImplicitSearchResult(result: SearchResult): Unit = ()
    }

    addAnalyzerPlugin(ConcreteAnalyzerPlugin)
  }
}
