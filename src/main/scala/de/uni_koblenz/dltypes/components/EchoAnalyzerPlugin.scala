package de.uni_koblenz.dltypes.components

import scala.tools.nsc.Global
import scala.tools.nsc.Mode

// AnalyzerPlugin
// --------------
// https://github.com/scala/scala/blob/2.13.x/src/compiler/scala/tools/nsc/typechecker/AnalyzerPlugins.scala
// https://github.com/scala/scala/blob/2.13.x/src/compiler/scala/tools/nsc/typechecker/Analyzer.scala
// https://github.com/scala/scala/blob/07d61ecf134dbba143531f5a1bd3c1289c437296/src/compiler/scala/tools/nsc/typechecker/Namers.scala
// https://github.com/scala/scala/blob/2.13.x/src/compiler/scala/tools/nsc/typechecker/Typers.scala ! 786 (Type checking / adapt)
// https://github.com/scala/scala/blob/07d61ecf134dbba143531f5a1bd3c1289c437296/test/files/run/analyzerPlugins.scala

// List AnalyzerPlugin function names in 'log' to activate logging
// for the named function. Use empty list to log them all.

class EchoAnalyzerPlugin(global: Global, log: List[String]) {
  import global._
  import analyzer._

  private def treeClass(t: Tree) = t.getClass.toString.split('.').last

  private def loglvl(s: String): Boolean = {
    log.isEmpty || log.contains(s)
  }

  def addToPhase(phase: String): Unit = {
    // Full documentation:
    // https://github.com/scala/scala/blob/2.13.x/src/compiler/scala/tools/nsc/typechecker/AnalyzerPlugins.scala
    object ConcreteAnalyzerPlugin extends AnalyzerPlugin {

      // Selectively activate this analyzer plugin, e.g. according to the compiler phase.

      override def isActive(): Boolean = {
        if (phase == "typer")
          global.phase.id <= global.currentRun.typerPhase.id
        else if (phase == "namer")
          global.phase.id <= global.currentRun.namerPhase.id
        else
          false
      }

      // Let analyzer plugins change the expected type before type checking a tree.

      override def pluginsPt(pt: Type, typer: Typer, tree: Tree, mode: Mode): Type = {
        if (loglvl("pluginsPt")) {
          reporter.echo ("\n\n=== [\u001b[36mpluginsPt\u001b[0m] ===============================")
          reporter.echo ("Expected type (pt): " + pt)
          reporter.echo ("Typed by " + typer.toString + " in mode " + mode.toString)
          reporter.echo ("Tree (of class) " + treeClass (tree) )
          reporter.echo ("----------\n" + tree.toString + "\n---------\n")
          reporter.echo ("W " + pt.isWildcard + " E " + pt.isError)
          reporter.flush ()
        }
        pt
      }

      // Let analyzer plugins modify the type that has been computed for a tree.

      override def pluginsTyped(tpe: Type, typer: Typer, tree: Tree, mode: Mode, pt: Type): Type = {
        if (loglvl("pluginsTyped")) {
          // Test: Get the type arguments.
          if (tpe.kind == "NullaryMethodType") {
            tpe.resultType match {
              case TypeRef(pre, sym, args) =>
                reporter.echo("=> tpe")
                reporter.echo(pre.toString)
                reporter.echo(sym.toString)
                reporter.echo(args.toString)
              case _ => Unit
            }

          }
          tpe match {
            case TypeRef(pre, sym, args) =>
              reporter.echo("tpe")
              reporter.echo(pre.toString)
              reporter.echo(sym.toString)
              reporter.echo(args.toString)
            case _ => Unit
          }
          pt match {
            case TypeRef(pre, sym, args) =>
              reporter.echo("pt")
              reporter.echo(pre.toString)
              reporter.echo(sym.toString)
              reporter.echo(args.toString)
            case _ => Unit
          }
          reporter.echo("\n\n=== [\u001b[33mpluginsTyped\u001b[0m] ============================")
          reporter.echo("Inferred type (tpe): " + tpe + " : " + tpe.typeSymbol.toString + " of kind " + tpe.kind)
          reporter.echo("    of kind " + tpe.kind + " is higher kinded " + tpe.isHigherKinded)
          reporter.echo("Expected type (pt): " + pt + " : " + pt.typeSymbol.toString)
          reporter.echo("Typed by " + typer.toString +" in mode " + mode.toString)
          reporter.echo("Tree (of class) " + treeClass(tree))
          reporter.echo("----------\n" + tree.toString + "\n---------\n")
          //if (tpe.resultType <:< pt) {
          //  reporter.echo(tpe.resultType + " is subtype " + pt)
          //} else {
          //  reporter.error(tree.pos, "wat??")
          //  reporter.echo(tpe.resultType + " is NOT subtype " + pt)
          //}
          reporter.flush()
        }
        tpe
      }

      // Let analyzer plugins change the types assigned to definitions. For definitions that have
      // an annotated type, the assigned type is obtained by typing that type tree. Otherwise, the
      // type is inferred by typing the definition's right hand side.

      override def pluginsTypeSig(tpe: Type, typer: Typer, defTree: Tree, pt: Type): Type = {
        if (loglvl("pluginsTypeSig")) {
          reporter.echo("\n\n=== [\u001b[35mpluginsTypeSig\u001b[0m] ==========================")
          reporter.echo("Inferred type (tpe): " + tpe.toString) // querying this may cause type errors
          reporter.echo("Expected type (pt): " + pt.toString)
          reporter.echo("Typed by " + typer.toString + "\n----------")

          defTree match {
            case Template(parents, self, body) =>
              reporter.echo("Template ( `extends` parents { self => body } )")
              reporter.echo("parents " + parents.toString)
              reporter.echo("self " + self.toString)
              reporter.echo("body " + body.toString)
            case ClassDef(mods, name, tparams, impl) =>
              reporter.echo("ClassDef ( mods `class` name [tparams] impl )")
              reporter.echo("mods " + mods.toString)
              reporter.echo("name " + name.toString)
              reporter.echo("tparams " + tparams.toString)
              reporter.echo("impl " + impl.toString)
            case ModuleDef(mods, name, impl) =>
              reporter.echo("ModuleDef ( mods `object` name impl )")
              reporter.echo("mods " + mods.toString)
              reporter.echo("name " + name.toString)
              reporter.echo("impl " + impl.toString)
            case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
              reporter.echo("DefDef ( mods `def` name[tparams](vparams_1)...(vparams_n): tpt = rhs )")
              reporter.echo("mods " + mods.toString)
              reporter.echo("name " + name.toString)
              reporter.echo("tparams " + tparams.toString)
              reporter.echo("vparamss " + vparamss.toString)
              reporter.echo("tpt " + tpt.toString)
              reporter.echo("rhs " + rhs.toString)
            case ValDef(mods, name, tpt, rhs) =>
              reporter.echo("ValDef ( mods `val` name: tpt = rhs )")
              reporter.echo("mods " + mods.toString)
              reporter.echo("name " + name.toString)
              reporter.echo("tpt " + tpt.toString)
              reporter.echo("rhs " + rhs.toString)
            case TypeDef(mods, name, tparams, rhs) =>
              reporter.echo("TypeDef ( mods `type` name[tparams] = rhs )")
              reporter.echo("mods " + mods.toString)
              reporter.echo("name " + name.toString)
              reporter.echo("tparams " + tparams.toString)
              reporter.echo("rhs " + rhs.toString)
          }
          reporter.echo("----------\n" + defTree.toString + "\n---------\n")
          reporter.flush()
        }
        tpe
      }

      // Modify the types of field accessors. The namer phase creates method types for getters and
      // setters based on the type of the corresponding field.

      override def pluginsTypeSigAccessor(tpe: Type, typer: Typer, tree: ValDef, sym: Symbol): Type = {
        if (loglvl("pluginsTypeSigAccessor")) {
          // Test: Get the type arguments.
          tpe match {
            case TypeRef(_, _, args) => reporter.echo(args.toString)
            case _ => Unit
          }
          reporter.echo("\n\n=== [\u001b[34mpluginsTypeSigAccessor\u001b[0m] ============================")
          reporter.echo("Inferred type (tpe): " + tpe + " : " + tpe.typeSymbol.toString + " of kind " + tpe.kind)
          reporter.echo("    of kind " + tpe.kind + " is higher kinded " + tpe.isHigherKinded)
          reporter.echo("Symbol: " + sym.toString)
          reporter.echo("Tree (of class) " + treeClass(tree))
          reporter.echo("----------\n" + tree.toString + "\n---------\n")
          reporter.flush()
        }
        tpe
      }

      // Decide whether this analyzer plugin can adapt a tree that has an annotated type to the
      // given type tp, taking into account the given mode (see method adapt in trait Typers).

      override def canAdaptAnnotations(tree: Tree, typer: Typer, mode: Mode, pt: Type): Boolean = true

      // Adapt a tree that has an annotated type to the given type tp, taking into account the given
      // mode (see method adapt in trait Typers).

      override def adaptAnnotations(tree: Tree, typer: Typer, mode: Mode, pt: Type): Tree = {
        if (loglvl("adaptAnnotations")) {
          reporter.echo("\n\n=== [\u001b[31madaptAnnotations\u001b[0m] ============================")
          reporter.echo("Expected type (pt): " + pt + " : " + pt.typeSymbol.toString)
          reporter.echo("Typed by " + typer.toString +" in mode " + mode.toString)
          reporter.echo("Tree (of class) " + treeClass(tree))
          reporter.echo("----------\n" + tree.toString + "\n---------\n")
          reporter.flush()
        }
        tree
      }

      // Modify the type of a return expression. By default, return expressions have type
      // NothingTpe.

      override def pluginsTypedReturn(tpe: Type, typer: Typer, tree: Return, pt: Type): Type = {
        if (loglvl("pluginsTypedReturn")) {
          reporter.echo("\n\n=== [\u001b[31mpluginsTypedReturn\u001b[0m] ============================")
          reporter.echo("Type of return expression (tpe): " + tpe + " : " + tpe.typeSymbol.toString)
          reporter.echo("Return type of enclosing method (pt): " + pt + " : " + pt.typeSymbol.toString)
          reporter.echo("Typed by " + typer.toString)
          reporter.echo("----------\n" + tree.toString + "\n---------\n")
          reporter.flush()
        }
        tpe
      }

      // Access the search instance that will be used for the implicit search.

      override def pluginsNotifyImplicitSearch(search: ImplicitSearch): Unit = ()

      // Access the implicit search result from Scalac's type checker.

      override def pluginsNotifyImplicitSearchResult(result: SearchResult): Unit = ()
    }

    addAnalyzerPlugin(ConcreteAnalyzerPlugin)
  }
}

