package de.uni_koblenz.dltypes.components

import scala.tools.nsc.Global
import scala.tools.nsc.Mode
import scala.util.{Failure => UtilFailure, Success => UtilSuccess, Try => UtilTry}
import java.io.File

import de.uni_koblenz.dltypes.backend.{MyGlobal, QueryTyper, ReasonerHermit}
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
    val prettyPrinter = new PrettyPrinter

    val redOn = "\u001b[31m"
    val greenOn = "\u001b[32m"
    val yellowOn = "\u001b[33m"
    val blueOn = "\u001b[34m"
    val colorOff = "\u001b[0m"

    // Lazy, since MyGlobal.ontologies is set by DLTypes.init (after
    // CheckerAnalyzerPlugin is initialized.
    lazy val fi = new File(MyGlobal.ontologies(":")) // This...
    // ... is in normal execution guaranteed to be != Nil, since DLTypes.init fails if
    // no ontology is provided. If the phase is instantiated by other means
    // (e.g., in testing), this has to be set explicitly!
    // TODO: It might still be an invalid file, handle error.
    lazy val reasoner = new ReasonerHermit(fi)
    lazy val qtyper = new QueryTyper(reasoner)

    // Extract the DL type name.
    def dlTypeName(tpe: Type): String = {
      if (tpe.kind == "NullaryMethodType")
        tpe.resultType.typeSymbolDirect.nameString
      else
        tpe.typeSymbolDirect.nameString
    }

    // Attempt to parse the DLEConcept representation of an DL type.
    def parseDL(tpe: Type): UtilTry[List[DLEConcept]] = {
      if (isQueryType(tpe))
        MyGlobal.qtypeTable.getOrElse(stripT(tpe), None) match {
          case Some(t) => UtilSuccess(t)
          case None => UtilFailure(new Exception(
            s"[DL] Internal Error: Encountered unseen internal query type $tpe"))
        }
        // TODO: else if (isAnnotatedQueryType) return List parsed from declared type
      else if (isInferredDL(tpe))
        MyGlobal.itypeTable.getOrElse(stripT(tpe), (None, Nil)) match {
          case (Some(t), _) => UtilSuccess(t)
          case (None, Nil) => UtilFailure(new Exception(
            s"[DL] Internal Error: Encountered unseen (explicit) inferred DL type $tpe"))
          case (None, lst) =>
            val us = DLEConcept.simplify(lst.foldLeft(Bottom: DLEConcept)(Union: (DLEConcept, DLEConcept) => DLEConcept))
            MyGlobal.itypeTable += stripT(tpe) -> (Some(List(us)), Nil)
            UtilSuccess(List(us))
        }
      else if (isRegisteredDLType(tpe))
        MyGlobal.dtypeTable.get(stripT(tpe)) match {
          case Some(t) => UtilSuccess(List(t))
          case None => UtilFailure(new Exception(
            s"[DL] Internal Error: Encountered unseen DL type $tpe"
          ))
        }
      else
        UtilFailure(new Exception(
          "[DL] Internal Error: DL type is not DL type."
        ))
      //else {
      //  val s = dlTypeName(tpe)
      //  parser.parse(parser.dlexpr, s) match {
      //    case parser.Success(m, _) => UtilSuccess(List(m.asInstanceOf[DLEConcept]))
      //    case parser.NoSuccess(msg, _) => UtilFailure(new Exception(msg))
      //  }
      //}
    }

    // Test whether tpe is indeed a DL type.
    def isDLType(tpe: Type): Boolean = {
      (tpe.typeSymbol.toString == "trait DLType" ||
      //tpe.typeSymbol.toString == "object SPARQL" ||
      tpe.typeSymbol.toString == "object IRI") &&
      tpe.toString != "de.uni_koblenz.dltypes.runtime.DLType"
    }

    def stripT(tpe: Type): String =
      tpe.toString.split('.').last

    def isSomeDLType(tpe: Type, specific: String): Boolean =
      isDLType(tpe) && stripT(tpe).startsWith(specific)

    def isQueryType(tpe: Type): Boolean = isSomeDLType(tpe, "SparqlQueryType")
    def isInferredDL(tpe: Type): Boolean = isSomeDLType(tpe, "InferredDLType")
    def isRegisteredDLType(tpe: Type): Boolean = isSomeDLType(tpe, "RegisteredDLType")

    def isQueryDef(tpe: Type): Boolean = {
      tpe.kind == "NullaryMethodType" &&
        (tpe.resultType match {
          case TypeRef(_, b, c) =>
            b.toString.startsWith("type List") &&
              c.nonEmpty &&
              c.head.typeSymbolDirect.toString.startsWith("type SparqlQueryType")
        })
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
        prettyPrinter.dleConcept(l) + " ⊏ " + prettyPrinter.dleConcept(r)
      case ConceptEquality(l, r) =>
        prettyPrinter.dleConcept(l) + " ≡ " + prettyPrinter.dleConcept(r)
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
      // In this case, build/update the inference constraints.
      if (tpe != pt
        && isInferredDL(pt)
        && isDLType(tpe)) {
        parseDL(tpe) match {
          case UtilFailure(e) =>
            reporter.error(tree.pos, "[DL] Type Parse Error: " + e.getMessage)
          case UtilSuccess(t) =>
            MyGlobal.itypeTable.getOrElse(stripT(pt), (None, Nil)) match {
              case (None, xs) => MyGlobal.itypeTable += stripT(pt) -> (None, xs ++ List(t.head)) // TODO: What if more than one? Query types? Impossible?
              case _ => Unit
            }
        }
      }
      // If both are DL types, do subsumed(tpe, pt) check.
      else if (isDLType(tpe) && isDLType(pt)) {
        parseDL(tpe).flatMap { dltpe =>
          parseDL(pt).map { dlpt =>
            if (dltpe.size != dlpt.size)
              reporter.error(tree.pos, "[DL] Wrong arity in query type. Expected: " + dltpe.size + " but found " + dlpt.size)
            if (warnH)
              reporter.warning(tree.pos, "[DL] Using argument matching heuristic.")

            dltpe.zip(dlpt).map { case (l, r) =>
              val test = Subsumed(l, r)
              if (l == r)
                (test, true)
              else {
                reportCheck(test)
                (test, reasoner.prove(test))
              }
            }
          }
        }
      } match {
        case UtilFailure(e) =>
          reporter.error(tree.pos, e.getMessage)
        case UtilSuccess(rs) =>
          rs.foreach { case (test, b) =>
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

        // Might be equality check.
        if (tpe == typeOf[Boolean]) {
          // Check if '==' and both sides are DL type.
          tree match {

            case Apply(Select(l, eqeq), List(r))
              if eqeq.decodedName.toString == "=="
                 && isDLType(l.tpe)
                 && isDLType(r.tpe) =>
              val lhs = l.symbol.tpe.resultType
              val rhs = r.symbol.tpe.resultType
              parseDL(lhs).flatMap { lhsDL =>
                parseDL(rhs).map { rhsDL =>
                  if (lhsDL.size != 1 || rhsDL.size != 1)
                    reporter.warning(tree.pos, "[DL] Can't check if query equality type is correct.")
                  else {
                    val test = ConceptEquality(Intersection(lhsDL.head, rhsDL.head), Bottom)
                    val result = reasoner.prove(test)
                    if (result)
                      reporter.warning(tree.pos, "[DL] comparing values of these DL types will always yield false.")
                  }
                }
              }
            case _ => Unit
          }
        }

        // Definition of SPARQL query.
        if (isQueryDef(tpe)) {

          //val t = q"??? : SparqlQueryType1"
          //val tt = newTypeName("SparqlQueryType1")
          //val selectedType = "SparqlQueryType1"
          //val tt = typer.typed(t)


          //reporter.echo(global.lookupTypeName("SparqlQueryType1".toCharArray).toString)
          //val zzz = global.lookupTypeName("SparqlQueryType1".toCharArray)
          //   val n = k.callsiteTyper.typed(q"??? : Foo").tpe.typeSymbol.fullName

          //reporter.echo("t tree " + t)
          //reporter.echo("tt tree " + tt + " " + tt.isTyped)
          //reporter.echo("type " + tt.tpe)

          tree match {
            // The case where SparqlQueryTypeX has to be extracted.
            case TypeApply(
                  Select(Apply(Select(Apply(r1, List(Apply(_/*apply*/, queryParts))), r3), sparqlArgs), _/*instanceOf*/),
                  List(sqt))
              if (r1.symbol.decodedName == "SparqlHelper"
                && (r3.toString == "sparql") || (r3.toString == "strictsparql")) =>

              val uSqt = sqt.toString.split('.').last.dropRight(1) // TODO: This too hacky.
              val isStrict = r3.toString == "strictsparql"

              // Calculate type for query.
              qtyper.run(
                queryParts.map(_.toString.stripPrefix("\"").stripSuffix("\"")),
                sparqlArgs.map { x =>
                  if (isDLType(x.tpe)) {
                    parseDL(x.tpe) match {
                      case UtilSuccess(t) if t.size == 1 => Right(t.head)
                      case UtilFailure(e) =>
                        reporter.error(tree.pos, "[DL] Type Parse error (in SPARQL argument): " + e.getMessage)
                        Right(Bottom)
                    }
                  }
                  else
                    Left(x.tpe.toString)
                },
                isStrict) match {
                // sparqlArgs.map(_.tpe.toString))
                case UtilFailure(e) => reporter.error(tree.pos, s"[DL] SPARQL error: ${e.getMessage} ($e)")
                case UtilSuccess(x) => MyGlobal.qtypeTable.update(uSqt, Some(x))
              }
            // In other cases, fall through and perform the type check.
            case _ => Unit
          }
        }

        // Cases were DLType is inferred (or explicitly used) need to be declared
        // explicitly with either a more specific type, or Top (⊤).
        if (tpeArgs.map { t =>
            mode == Mode.TYPEmode && t == typeOf[DLType] && t.toString.split('.').last == "DLType"
          }.exists(identity))
          reporter.error(tree.pos, "[DL] Explicit use or inference of 'DLType' violates type safety."
            + "\nPossible solution: Declare more specific type or use ⊤ explicitly.")

        // TODO: (pure) DL if conditions can't be inferred.
        // ...

        // TODO: This has to be done recursively, in case type args are poly
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
