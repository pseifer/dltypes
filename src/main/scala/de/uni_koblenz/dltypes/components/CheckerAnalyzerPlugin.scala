package de.uni_koblenz.dltypes.components

import scala.tools.nsc.Global
import scala.tools.nsc.Mode
import java.io.File

import de.uni_koblenz.dltypes.backend.DLTypesError.EitherDL
import de.uni_koblenz.dltypes.backend.MyGlobal.{AnyDLType, InferredDLType, RegisteredDLType, UnionDLType}
import de.uni_koblenz.dltypes.backend._
import de.uni_koblenz.dltypes.backend.{CheckContravariant, CheckCovariant, CheckInvariant}
import de.uni_koblenz.dltypes.tools._
import de.uni_koblenz.dltypes.runtime.DLType


class CheckerAnalyzerPlugin(global: Global) {
  import global._
  import analyzer._

  // warnings: Emit (compiler) warnings on non-critical failures.
  // printchecks: Print +++CHECKING+++<...> messages for all DL type checks.
  // debug: Print additional debug information on type errors and warnings.
  def add(warnings: Boolean = true,
          printchecks: Boolean = false,
          debug: Boolean = false): Unit = {


    // ***** REPORTING, DEBUGGING, IO

    val redOn = "\u001b[31m"
    val greenOn = "\u001b[32m"
    //val yellowOn = "\u001b[33m"
    //val blueOn = "\u001b[34m"
    val colorOff = "\u001b[0m"

    // Format and echo a message for a DLETruthy check.
    def echo(t: DLETruthy): Unit =
      if (printchecks)
        reporter.echo(greenOn
          + "===[ CHECK ]===  "
          + t.pretty
          + colorOff)
    reporter.flush()

    // Report error or warning at the given Position.
    def report(pos: Position, e: DLTypesError): Unit = e match {
      case w @ Warning(_) => reporter.warning(pos, w.show)
      case e => reporter.error(pos, e.show)
    }

    // Report additional information, if debug mode is turned on.
    // This exposes internal information (developer tool), not
    // more detailed type errors!
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


    // ***** TYPE CHECKING, MATCHING, UTILITY

    lazy val typeChecker = new DLTypeChecker

    // Test whether tpe is indeed a DL type.
    def isDLType(tpe: Type): Boolean = {
      tpe.resultType match {
        case RefinedType(ts, _) =>
          ts.forall(isDLType)
        case _ =>
          (tpe.typeSymbol.toString == "trait DLType" ||
          tpe.typeSymbol.toString == "object IRI") &&
          tpe.toString != "de.uni_koblenz.dltypes.runtime.DLType"
      }
    }

    // Test is tpe or its type arguments are DL type(s).
    def hasDLType(tpe: Type): Boolean =
      isDLType(tpe) || tpe.typeArgs.exists(isDLType)

    // Strip DL type from its full path.
    def stripT(tpe: Type): String = tpe.toString.split('.').last

    // Helper: Test if type is some 'specific' DL type.
    def isSomeDLType(tpe: Type, specific: String): Boolean =
      isDLType(tpe) && stripT(tpe).startsWith(specific)

    // Test if tpe is inferred DL type.
    def isInferredDL(tpe: Type): Boolean =
      isSomeDLType(tpe, MyGlobal.inferred_type_name)

    // Test if tpe is ordinary (registered) DL type.
    def isRegisteredDLType(tpe: Type): Boolean =
      isSomeDLType(tpe, MyGlobal.registered_type_name)

    // Convert tpe to RegisteredDLType.
    def toDLType(tpe: Type): Option[AnyDLType] = {
      def toInternal(tpe: Type): Option[AnyDLType] = {
        if (isRegisteredDLType(tpe))
          Some(RegisteredDLType(stripT(tpe)))
        else if (isInferredDL(tpe))
          Some(InferredDLType(stripT(tpe)))
        else
          None
      }

      tpe.resultType match {
        case RefinedType(ts, _) =>
          val z = ts.map(toInternal)
          if (z.exists(_.isEmpty))
            None
          else
            Some(UnionDLType(z.map(_.get)))
        case _ => toInternal(tpe)
      }
    }

    // Test if type is DLType explicitly.
    def isExplicitDLType(tpeArgs: List[Type], mode: Mode): Boolean =
      tpeArgs.map { t =>
        mode == Mode.TYPEmode && t == typeOf[DLType] && t.toString.split('.').last == "DLType"
      }.exists(identity)

    // Test if tree is equality on DL types.
    def isDLEquality(tpe: Type, tree: Tree): Boolean =
      tpe == typeOf[Boolean] && {
        tree match {
          case Apply(Select(l, eqeq), List(r))
            if eqeq.decodedName.toString == "=="
              && isDLType(l.tpe)
              && isDLType(r.tpe) => true
          case _ => false
        }
      }

    // Check is all bases classes are identical.
    def sameClassAs(tpe: Type, pt: Type): Boolean =
      tpe.baseClasses == pt.baseClasses

    // Check if tpe has type arguments.
    def hasArgs(tpe: Type): Boolean = getTypeArgs(tpe).nonEmpty

    // Get the type arguments (if any).
    def getTypeArgs(tpe: Type): List[Type] = {
      // Variables are nullary methods, therefore we might
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

    sealed trait Variance
    case object Covariant extends Variance
    case object Invariant extends Variance
    case object Contravariant extends Variance

    // Find variance for type.
    def getVariances(tpe: Type): List[Variance] = {
      def parse(s: String): List[Variance] = {
        val block = """^\[(.*)\] extends.*""".r
        val invariant = """\s*\w.*""".r
        val covariant = """\s*[+]\w.*""".r
        val contravariant = """\s*[-]\w.*""".r

        s match {
          case block(x) =>
            x.split(',').map {
              case invariant() => Invariant
              case covariant() => Covariant
              case contravariant() => Contravariant
            }.toList
          case _ => Nil
        }
      }

      if (tpe.baseClasses.nonEmpty)
        parse(tpe.baseClasses.head.signatureString)
      else
        Nil
    }

    // Build up type checks, according to variance.
    def buildCheck(tpe: Type, pt: Type, tpeV: Variance):
    Option[EitherDL[CheckEntity]] = {
      if (isDLType(tpe) && isDLType(pt)) {
        val tpeDL = toDLType(tpe)
        val ptDL = toDLType(pt)
        if (tpeDL.isEmpty)
          Some(Left(InternalError(s"Broken DL type $tpe")))
        else if (ptDL.isEmpty)
          Some(Left(InternalError(s"Broken DL type $pt")))
        else {
          val lhs = tpeDL.get
          val rhs = ptDL.get

          tpeV match {
            case Contravariant => Some(Right(CheckContravariant(lhs, rhs)))
            case Covariant => Some(Right(CheckCovariant(lhs, rhs)))
            case Invariant => Some(Right(CheckInvariant(lhs, rhs)))
          }
        }
      }
      else if (isDLType(tpe) && !isDLType(pt) && pt.isFinalType && !(pt == typeOf[Nothing]))
        Some(Left(TypeError(s"Incompatible DLType $tpe with $pt")))
      else if (isDLType(pt) && !isDLType(tpe) && tpe.isFinalType && !(tpe == typeOf[Nothing]))
        Some(Left(TypeError(s"Incompatible $tpe with DLType $pt")))
      else
        None
    }

    // Collect the DL type checks for tpe and pt.
    def checksRecur(tpe: Type, pt: Type, variance: Variance, pos: Position): List[CheckEntity] = {
      // If no type arguments, build simple check.
      if (!hasArgs(tpe) && !hasArgs(pt)) {
        val b = buildCheck(tpe, pt, variance)
        if (b.isDefined)
          b.get match {
            case Left(e) => // DL type check, but something went wrong.
              report(pos, e)
              Nil
            case Right(c) => // DL type check to be performed.
              List(c)
          }
        else
          Nil // Not a DL type check.
      }
      else {
        // If both are the same class, perform type check.
        if (sameClassAs(tpe, pt)) {
          getTypeArgs(tpe)
            .zip(getVariances(tpe))
            .zip(getTypeArgs(pt))
            .flatMap { case ((l: Type, v: Variance), r: Type) =>
              checksRecur(l, r, v, pos)
            }
        }
        // Otherwise, fetch the appropriate base class for 'tpe'.
        else {
          val bts = tpe.baseTypeSeq.toList
          val matching = bts.filter(x => x.typeSymbol == pt.typeSymbol)
          // If non, tpe or pt are Wildcard, Nothing, etc.
          // ...and no type check is required. If pt is not in the base classes,
          // but an ordinary class, this would haven been caught by the type checker.
          if (matching.nonEmpty)
            checksRecur(matching.head, pt, variance, pos)
          else
            Nil
        }
      }
    }

    // Build up tests for both higher and lower bounds, if existent.
    def makeBoundChecks(v: List[(Type, (Option[Type], Option[Type]))], pos: Position): List[CheckEntity] = {
      val (l, h) = v.map { case (t, (lo, hi)) =>
          val loC = if (lo.nonEmpty) Some(checksRecur(lo.get, t, Covariant, pos)) else None
          val hiC = if (hi.nonEmpty) Some(checksRecur(t, hi.get, Covariant, pos)) else None
          (loC, hiC)
      }.unzip
      (l ++ h).filter(_.isDefined).flatMap { x => x.get }
    }

    // Build the query type for SPARQL tree.
    def extractQueryType(tpe: Type, tree: Tree): Unit = {
      tree match {
        // The case where SparqlQueryTypeX has to be extracted.
        case TypeApply(
        Select(Apply(Select(Apply(r1, List(Apply(_/*apply*/, queryParts))), r3), sparqlArgs), _/*instanceOf*/),
        List(sqt)) if (r1.symbol.decodedName == "SparqlHelper"
            && (r3.toString == "sparql") || (r3.toString == "strictsparql")) =>

          // The DL types that need to be defined.
          val targets: List[AnyDLType] = {
            val temp = sqt.tpe.typeArgs.head
            // Single type List[DL]
            if (isDLType(temp))
              List(RegisteredDLType(stripT(temp)))
            // List of tuple List[(DL1, ..., DLn)]
            else
              temp.typeArgs.map(stripT).map(RegisteredDLType)
          }
          // Set strictness mode.
          val isStrict = r3.toString == "strictsparql"

          // The substring parts of the query.
          val parts = queryParts.map(_.toString.stripPrefix("\"").stripSuffix("\""))

          // The arguments to the query.
          val args =
            sparqlArgs.map { a =>
              val x =
                // Normal argument.
                if (a.tpe.bounds.isEmptyBounds)
                  a.tpe
                // Type parameter, but with higher bound -> use it
                else
                  a.tpe.bounds.hi // TODO: Experimental - test this!

              // Set to DL type or Scala type.
              if (isDLType(x)) {
                val tpeDL = toDLType(x)
                if (tpeDL.isEmpty)
                  None
                else
                  Some(Right(tpeDL.get))
              }
              else
                Some(Left(x.toString))
            }
          // If any DL types was not found (None), report error.
          if (args.exists(_.isEmpty))
            report(tree.pos, InternalError("Encountered undefined DL type in query argument."))
          // Infer and set targets to query type.
          else {
            val errs = typeChecker.typeQuery(parts, args.map(_.get), targets, isStrict)
            errs.foreach(report(tree.pos, _))
          }
        // In other cases, fall through and perform the type check.
        case _ => Unit
      }
    }


    object ConcreteAnalyzerPlugin extends AnalyzerPlugin {
      // This analyzer plugin is active during the 'typer' phase.
      override def isActive(): Boolean = global.phase.id <= global.currentRun.typerPhase.id

      // TODO: Document!
      override def pluginsTyped(tpe: Type, typer: Typer, tree: Tree, mode: Mode, pt: Type): Type = {
        var toCheck: List[CheckEntity] = Nil

        if (tpe != pt && isInferredDL(pt) && isDLType(tpe)) {
          val tpeDL = toDLType(tpe)
          val ptDL = toDLType(pt)
          if (tpeDL.isEmpty)
            Some(Left(InternalError(s"Broken DL type $tpe")))
          else if (ptDL.isEmpty)
            Some(Left(InternalError(s"Broken DL type $pt")))
          else
            MyGlobal.updateConstraints(ptDL.get.asInstanceOf[InferredDLType], tpeDL.get)
        }
        else {
          // Check type bounds...
          tree match {
            // ...for poly methods or class constructors.
            case Apply(f, _) =>
              // Get the parameter type(s) for this instance.
              val types = f.tpe.paramTypes
              // Get the type parameters of the method or class.
              val params =
                if (f.symbol.isClassConstructor) f.symbol.enclClass.typeParams
                else f.symbol.typeParams
              // Find all upper and lower bounds.
              val bounds = params.map { typeParamsSymbol =>
                typeParamsSymbol.typeSignature match {
                  case TypeBounds(lo, hi) =>
                    val rlo = if (lo != typeOf[Nothing]) Some(lo) else None
                    val rhi = if (hi != typeOf[Nothing]) Some(hi) else None
                    (rlo, rhi)
                  case _ => (None, None)
                }
              }
              // Generate the type checks for all bounds.
              toCheck = toCheck ++ makeBoundChecks(types.zip(bounds), tree.pos)
            case _ => Unit
          }

          // If this is an equality check (with DL types),
          // warn in case it can't succeed (intersection is bottom).
          if (isDLEquality(tpe, tree))
            tree match {
              case Apply(Select(l, _), List(r)) =>
                val lhs = toDLType(l.symbol.tpe.resultType)
                val rhs = toDLType(r.symbol.tpe.resultType)
                // Note: Is guarded by isDLEquality check.
                if (lhs.isEmpty || rhs.isEmpty)
                  reporter.error(tree.pos, "[DL] BRUTAL INTERNAL ERROR")
                else
                  toCheck = CheckEquality(lhs.get, rhs.get) :: toCheck
              case _ =>
                reporter.error(tree.pos, "[DL] BRUTAL INTERNAL ERROR")
            }

          // If tree is the definition of a SPARQL query, extract the type.
          extractQueryType(tpe, tree)

          // Cases were DLType is inferred (or explicitly used) need to be declared
          // explicitly with either a more specific type, or Top (⊤).
          if (isExplicitDLType(getTypeArgs(tpe), mode))
            reporter.error(tree.pos, "[DL] Explicit use or inference of 'DLType' violates type safety."
              + "\nPossible solution: Declare more specific type or use ⊤ explicitly.")

          // Add checks to toCheck.
          toCheck = toCheck ++ checksRecur(tpe, pt, Covariant, tree.pos)

          // Perform all checks and handle the results.
          for (r <- toCheck.reverse.map(typeChecker.check))
            r match {
              case Result(test, error) =>
                echo(test)
                if (error.nonEmpty) {
                  report(tree.pos, error.get)
                  debugError(tpe, pt, tree, mode)
                }
              // Some other failure occurred.
              case OtherFailure(e) => report(tree.pos, e)
              // Nothing had to be checked (i.e., exactly the same DLEConcept).
              case EmptyResult => Unit
            }
        }

        // Return the type.
        tpe
      }


      // Find the type of all branches (if DL types).
      private def findIfType(tree: Tree): List[Type] = {
        tree match {
          // If it is a block, look at last expression.
          case Block(_, expr) =>
            findIfType(expr)

          // At least one extra branch left.
          case If(_, thenP, elseP @ If(_, _, _)) =>
            val elseT = findIfType(elseP)
            // If else branch was nil, something below was no DL type.
            if (elseT.isEmpty)
              Nil
            // If thenP is also DL type, append to list.
            else if (hasDLType(thenP.tpe))
                thenP.tpe :: elseT
            // Otherwise, this is the point where it is no longer DL type.
            else
              Nil

          // Else branch is not another if.
          case If(_, thenP, elseP) =>
            // If both are DL types, we are good to go.
            if (hasDLType(thenP.tpe) && hasDLType(elseP.tpe))
              List(thenP.tpe, elseP.tpe)
            // Otherwise, we won't find a type for this if expr.
            else
              Nil

          // We only type IF - other expressions are ignored.
          case _ => Nil
        }
      }


      private def intersect(tps: List[Type]): EitherDL[Type] = {
        // All are DL types, just intersect them.
        if (tps.forall(isDLType))
          Right(rootMirror.universe.intersectionType(tps.distinct))
        // DL type exists, but not all are DL types - can not intersect.
        else if (tps.exists(isDLType))
          Left(TypeError("Can not intersect DL type with non DL type."))
        // TODO: Intersection of DL type arguments for all types.
        else
          Left(InternalError("NOT YET IMPLEMENETD"))
      }


      // TODO: Doc
      override def pluginsTypeSig(tpe: Type, typer: Typer, defTree: Tree, pt: Type): Type = {
        var res: Type = tpe

        // TODO: MATCH expressions
        // TODO: Currently only works for simple DL type
        defTree match {
          // Method definition.
          case DefDef(_, _, _, _, tpt, rhs)
            if tpt.tpe != null && hasDLType(tpt.tpe) => {
              // Note: Invoking the typer here can't lead to cyclic issues,
              //       since recursive methods need declared return type.
              val potential = findIfType(typer.typed(rhs))
              if (potential.nonEmpty)
                intersect(potential) match {
                  case Left(e) => report(defTree.pos, e)
                  case Right(r) =>
                    res = tpe match {
                      case NullaryMethodType(_) => NullaryMethodType(r)
                      case PolyType(p1, MethodType(p2, _)) => PolyType(p1, MethodType(p2, r))
                      case MethodType(p, _) => MethodType(p, r)
                    }
                }
            }
          // Variable definition.
          case ValDef(_, _, tpt, rhs) if tpt.tpe != null && hasDLType(tpt.tpe) => {
            reporter.echo(s"TPE $tpe TPT.TPE ${tpt.tpe} \nin $defTree")
            val potential = findIfType(typer.typed(rhs))
            reporter.echo(s"pot $potential")
            if (potential.nonEmpty)
              intersect(potential) match {
                case Left(e) => report(defTree.pos, e)
                case Right(r) => res = r
              }
            }
          case _ => Unit
        }
        res
      }
    }

    // Register this plugin with the compiler.
    addAnalyzerPlugin(ConcreteAnalyzerPlugin)
  }
}
