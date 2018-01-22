package de.uni_koblenz.dltypes
package components

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.TypingTransformers
import scala.tools.nsc.Phase
import scala.tools.nsc.typechecker.Analyzer
import scala.tools.nsc.Mode
import scala.util.{Success => UtilSuccess, Failure => UtilFailure}

import java.io.File


class Checker(val global: Global)
  extends PluginComponent with TypingTransformers {
  import global._

  override val phaseName: String = "dl-check-types"
  override val runsAfter: List[String] = "typer" :: Nil
  override val runsRightAfter = Some("typer")

  def newPhase(_prev: Phase) = new CheckDLTypesPhase(_prev)

  class DLAdapter extends Analyzer {
    override lazy val global: Checker.this.global.type = Checker.this.global
    def adapt(unit: CompilationUnit): Tree = {
      val context = rootContext(unit)
      val checker = new TreeAdapter(context)
      unit.body = checker.typed(unit.body)
      unit.body
    }

    import backend._
    // TODO Note: wine.rdf is hardcoded (as resource) - needs to be updated!
    val fi = new File(getClass.getResource("/wine.rdf").getFile)
    val reasoner = new ReasonerHermit(fi)

    override def newTyper(context: Context): Typer =
      new TreeAdapter(context)

    class TreeAdapter(context0: Context) extends Typer(context0) with Extractor {
      lazy val global: Checker.this.global.type = Checker.this.global
      //override protected def adapt(tree: Tree, mode: Mode, pt: Type, original: Tree = EmptyTree): Tree = {
      //  super.adapt(tree, mode, pt, original)
      //  //if (tree.tpe <:< pt)
      //  //  tree
      //  //else {
      //  //  super.adapt(tree, mode, pt, original)
      //  //}
      //}

      override def typed(tree: Tree, mode: Mode, pt: Type): Tree = {
        import backend._
        // Perform DL type checks.
        tree match {
          case AppliedDLTypeTree(tryDleTpe) =>
            pt match {
              case AppliedDLType(tryDlePt) =>
                val check = tryDleTpe.flatMap { dle => tryDlePt.map( pt => Subsumed(dle, pt)) }
                check match {
                  case UtilFailure(msg) => reporter.error(tree.pos, msg.toString)
                  case UtilSuccess(c) =>
                    if (reasoner.prove(c)) {
                      reporter.echo("++CHECKED++ " + c.toString)
                    } else {
                      reporter.error(tree.pos, "[DL] Type error " + c.toString + " was not true.")
                    }
                }
              case WildcardType => Unit
              case _ => reporter.error(tree.pos, "[DL] No DL type on right hand side.")
            }
          case DLTypeTree(tryDleTpe) =>
            pt match {
              case DLType(tryDlePt) =>
                val check = tryDleTpe.flatMap { dle => tryDlePt.map( pt => Subsumed(dle, pt)) }
                check match {
                  case UtilFailure(msg) => reporter.error(tree.pos, msg.toString)
                  case UtilSuccess(c) =>
                    if (reasoner.prove(c)) {
                     reporter.echo("++CHECKED++ " + c.toString)
                    } else {
                     reporter.error(tree.pos, "[DL] Type error " + c.toString + " was not true.")
                    }
                }
              //case DLIndividual(tryDlePt) => {
              //  val check = tryDleTpe.flatMap { dle => tryDlePt.map( pt => MemberOf(pt, dle)) }
              //  check match {
              //    case UtilFailure(msg) => reporter.error(tree.pos, msg.toString)
              //    case UtilSuccess(check) => reasoner.prove(check) match {
              //      case true => reporter.echo("++CHECKED++ " + check.toString)
              //      case false => reporter.error(tree.pos, "[DL] Type error " + check.toString + " was not true.")
              //    }
              //  }
              //}
              case WildcardType => Unit
              case _ => reporter.error(tree.pos, "[DL] No DL type on right hand side.")
            }
          case Apply(fun, args) =>
            // TODO: Check if tree.tpe is DLType, if so, try to infer more concrete type...
            // TODO: ... and check that one against pt.
            // tpe.typeArgs, tpe.typeParams, type.paramType
            //reporter.echo("++DLCHECK-APPLY++")
            //reporter.echo("++APPLY++ in " + fun.toString)
            //reporter.echo(" > with argument types " + args.map{ _.tpe }.toString)
            //reporter.echo(tree.tpe.toString + " is subtype of ")
            //reporter.echo(pt.toString)
          case _ =>
            //reporter.echo("++")
            //reporter.echo(tree.toString)
            //reporter.echo(tree.tpe.toString + " is subtype of ") //Unit // Do nothing.
            //reporter.echo(pt.toString)
        }

        tree match {
          // Don't need to change anything for those.
          case EmptyTree | TypeTree() => Unit
          // In general, setTyped to null to force further execution of typed.
          case _ => tree.setType(null)
        }
        super.typed(tree, mode, pt)
      }
    }
  }

  class CheckDLTypesPhase(prev: Phase) extends StdPhase(prev) {
    override def name: String = Checker.this.phaseName
    def apply(unit: CompilationUnit): Unit = (new DLAdapter).adapt(unit)
  }
}
