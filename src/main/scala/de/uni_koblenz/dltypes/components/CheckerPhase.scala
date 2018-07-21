package de.uni_koblenz.dltypes
package components

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.Phase

import de.uni_koblenz.dltypes.backend.DLTypesError.EitherDL
import de.uni_koblenz.dltypes.backend._
import de.uni_koblenz.dltypes.tools._
import de.uni_koblenz.dltypes.runtime.{DLType, dl}


// The 'dl-typer' phase.
// Implements the ScaSpa typer.
// This is a bit of a 'HC SVNT DRACONES' situation, to be honest.
class CheckerPhase(val global: Global) extends PluginComponent {
  import global._

  override val phaseName: String = "dl-typer"
  override val runsAfter: List[String] = "typer" :: Nil
  override val runsRightAfter = Some("typer")

  def newPhase(prev: Phase): Phase = new StdPhase(prev) {
    override def name: String = phaseName

    override def apply(unit: CompilationUnit): Unit = {
      currentRun.units foreach { unit =>
        (new MyTraverser).traverse(unit.body)
      }
    }
  }

  lazy val typeChecker: TypeChecker = Globals.typeChecker

  val log = new Logger(global, List())

  sealed trait Variance
  case object Covariant extends Variance
  case object Invariant extends Variance
  case object Contravariant extends Variance

  // Find variance for type.
  private def getVariances(tpe: Type): List[Variance] = {
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

  // Apply 'fn' to all matching type(argument)s in left and right.
  // Recursively check all arguments to (the same) higher-order types,
  // or match the fitting base class first.
  // Return 'empty' for types which are not Scala subtypes.
  // Merge results for all arguments with 'concat'.
  private def mapType[A](left: Type, right: Type,
                         fn: (Type, Type, Variance) => A,
                         empty: (Type, Type) => A,
                         concat: (Type, Type, List[A]) => A,
                         initVariance: Variance,
                         pos: Position = NoPosition): A = {
    def recur(left: Type, right: Type, variance: Variance): A = {
      // If neither have arguments, apply 'fn'.
      if (!hasArgs(left) && !hasArgs(right))
        fn(left, right, variance)
      // If both are the same class, recur on all arguments.
      else if (sameClassAs(left, right)) {
        val cases = getTypeArgs(left)
          .zip(getVariances(left))
          .zip(getTypeArgs(right))
          .map { case ((l: Type, v: Variance), r: Type) =>
              recur(l, r, v)
          }
        concat(left, right, cases)
      }
      // Otherwise, fetch the appropriate base class for 'tpe' and recur.
      else {
        val bts = right.baseTypeSeq.toList
        val matching = bts.filter( x => x.typeSymbol == left.typeSymbol)
        if (matching.nonEmpty)
          recur(left, matching.head, variance)
        else
          empty(left, right)
      }
    }

    recur(left, right, initVariance)
  }

  // Check is all bases classes are identical.
  private def sameClassAs(tpe: Type, pt: Type): Boolean =
    tpe.baseClasses == pt.baseClasses

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

  private def paramOrSkolem(tpe: Type): Boolean = {
    val t = tpe.typeArgs
    if (t.isEmpty)
      tpe.typeSymbol.isTypeParameterOrSkolem
    else
      t.exists(paramOrSkolem)
  }

  // True if the tpe has any type arguments.
  private def hasArgs(tpe: Type): Boolean = getTypeArgs(tpe).nonEmpty

  // True if tpe is typeOf[DLType].
  private def isDLType(tpe: Type): Boolean = tpe =:= typeOf[DLType]

  // True if tpe is exactly typeOf[DLType], no annotations.
  private def isBasicDLType(tpe: Type): Boolean = tpe == typeOf[DLType]

  // True if name is any variation of sparql builder.
  private def isSPARQLBuilder(n: Name): Boolean =
    List("sparql", "sparql1", "strictsparql", "strictsparql1")
      .contains(n.toString)

  // True if name is strict sparql builder.
  private def isStrictSPARQLBuilder(n: Name): Boolean =
    List("strictsparql", "strictsparql1").contains(n.toString)

  // True if tree is Sparql(1)Helper.
  private def isSPARQLHelper(t: Tree): Boolean =
    List("SparqlHelper", "Sparql1Helper").contains(t.symbol.decodedName)

  // True if this is sparql1 builder.
  private def isSPARQL1Builder(n: Name): Boolean =
    List("sparql1", "strictsparql1").contains(n.toString)

  // Get the DL type annotation, if present.
  private def getDLAnnotation(tpe: Type): Option[AnnotationInfo] =
    tpe.resultType.staticAnnotations.find {
      case AnnotationInfo(atp, _, _) if atp == typeOf[dl] => isDLType(tpe.resultType)
      case _ => false
    }

  // Test if tree is equality on DL types.
  def isDLEquality(tpe: Type, tree: Tree): Boolean =
    tpe == typeOf[Boolean] && {
      tree match {
        case Apply(Select(l, eqeq), List(r)) =>
          if ((eqeq.decodedName.toString == "=="
             || eqeq.decodedName.toString == "sameAs")
            && isDLType(l.symbol.tpe.resultType)
            && isDLType(r.tpe)) true
          else false
        case _ => false
      }
    }

  def warnEquality(tree: Tree): Unit = {
    tree match {
      case Apply(Select(l, _), List(r)) =>
        val lhs = getDLType(l.symbol.tpe.resultType)
        val rhs = getDLType(r.tpe.resultType)
        val res = typeChecker.check(CheckEquality(lhs.get, rhs.get))
        res match {
          case Result(test, warn) =>
            log.echo(test)
            if (warn.nonEmpty)
              log.report(tree.pos, warn.get)
          case OtherFailure(e) => log.report(tree.pos, e)
          case EmptyResult => Unit
        }
    }
  }

  // Get the DLEConcept pertaining to the @dl annotation (if present).
  // If this fails, the type did not have an annotation. If parsing
  // the type fails this is detected earlier (unless the parser is broken).
  private def getDLType(tpe: Type): Option[DLEConcept] = {
    getDLAnnotation(tpe).flatMap(getDLType)  match {
      case None =>
        val dea = tpe.dealias
        if (dea == tpe)
          None
        else
          getDLType(dea)
      case Some(x) => Some(x)
    }
  }

  private def getDLType(ann: AnnotationInfo): Option[DLEConcept] = {
    ann match {
      case AnnotationInfo(_, List(Literal(Constant(sdl))), _) =>
        DLE.parse(sdl.toString).toOption
      case _ => None
    }
  }

  // Recursively apply lubDL to all DLType arguments of n arbitrary types.
  private def lubDLRecur(types: List[Type]): Option[Type] = {
    def lubPrime(left: Type, right: Option[Type]): Option[Type] = {
      if (right.isEmpty)
        None
      else
        lubDLRecur(left, right.get)
    }

    types.tail.foldRight(Some(types.head) : Option[Type])(lubPrime)
  }

  // Recursively apply lubDL to all DLType arguments of two arbitrary types.
  private def lubDLRecur(left: Type, right: Type): Option[Type] = {
    def lub(left: Type, right: Type, variance: Variance): Type = {
      val t = lubDL(List(left, right))
      if (t.isEmpty)
        NoType
      else
        t.get
    }

    def mkRef(l: Type, r: Type, args: List[Type]): Type =
      TypeRef(l.prefix, l.typeSymbol, args)

    val t = mapType(left, right, lub, (_, _) => NoType, mkRef, Covariant)
    if (t == NoType)
      None
    else
      Some(t)
  }

  // Get the least upper bound of all @dl annotated types in 'types'.
  // Returns None if any are not annotated DL types.
  private def lubDL(types: List[Type]): Option[Type] = {
    val parsed = types.flatMap(getDLType)

    if (parsed.nonEmpty) {
      val tpe = Globals.typeChecker.lub(parsed)
      Some(newDL(tpe))
    }
    else
      None
  }

  private def newDL(concept: DLEConcept): Type = {
    val newT = typer.typed(Literal(Constant(concept.pretty())))
    val ann = List(AnnotationInfo(typeOf[dl], List(newT), Nil))
    typeOf[DLType].withAnnotations(ann)
  }

  // Infer the return type of methods (with type parameters) and check
  // argument types for cases which are not type parameters.
  private def inferAndCheckMethodDL(pos: Position, paramTypes: List[Type], args: List[Type],
                                    formalReturnTpe: Type, returnTpe: Type): Type = {
    // HC SVNT DRACONES

    val both = paramTypes.lastOption match {
      // If this (has to be the last) is repeated parameter...
      case Some(ti) if ti.baseClasses.contains(definitions.RepeatedParamClass) =>
        val t = ti.typeArgs.head // ...get the actual type
        val n = paramTypes.length - 1
        // The normal parameters
        val part1 = paramTypes.take(n).zip(args.take(n))
        val remaining = args.drop(n)
        val m = remaining.size
        // The repeated parameter
        val part2 = List.fill(m)(t).zip(remaining)
        part1 ++ part2
      case Some(t) =>
        paramTypes.zip(args)
      case None =>
        Nil
    }

    val m = scala.collection.mutable.Map[Type, List[Type]]()

    def collect(left: Type, right: Type, variance: Variance): Unit = {
      if (left.typeSymbol.isTypeParameterOrSkolem && isDLType(right)) {
        if (m.contains(left)) {
          val others = m.getOrElse(left, Nil)
          m += left -> (right :: others)
        }
        else
          m += left -> List(right)
      }
      else
        checkType(pos, left, right, variance)
    }

    both.foreach { case (l, r) =>
      mapType(l, r, collect, (_, _) => Unit, (_, _, _: List[Any]) => Unit, Covariant)
    }

    def recur(left: Type, right: Type): Type = {
      if (!hasArgs(left) && !hasArgs(right)) {
        (left, right) match {
          case (MethodType(lP, lR), MethodType(rP, rR)) =>
            val newP = lP.zip(rP).map { case (l, r) => l.updateInfo(recur(l.tpe, r.tpe)) }
            MethodType(newP, recur(lR, rR))
          case _ =>
            if (!right.typeSymbol.isTypeParameterOrSkolem)
              right
            else if (isBasicDLType(left) && m.contains(right)) {
              val bRight = lubDL(m(right))
              if (bRight.nonEmpty) {
                log.infer(s"@ ${left.staticAnnotations}", s"@ ${bRight.get.staticAnnotations}")
                val ann = getDLAnnotation(bRight.get)
                if (ann.nonEmpty)
                  left.withAnnotations(ann.head :: left.annotations)
                else
                  left
              }
              else
                left
            }
            // Cases where the type is explicitly declared, check lub against declared type.
            else if (isDLType(left) && m.contains(right)) {
              val bRight = lubDL(m(right))
              if (bRight.nonEmpty) {
                checkType(pos, left, bRight.get)
              }
              left
            }
            else
              left
        }
      }
      // If both are the same class, recur on all arguments.
      else if (sameClassAs(left, right)) {
        val cases = getTypeArgs(left)
          .zip(getVariances(left))
          .zip(getTypeArgs(right))
          .map { case ((l: Type, v: Variance), r: Type) =>
              recur(l, r)
          }
        val ref = TypeRef(left.prefix, right.typeSymbol, cases)
        ref
      }
      // Otherwise, fetch the appropriate base class for 'tpe' and recur.
      else {
        val bts = right.baseTypeSeq.toList
        val matching = bts.filter( x => x.typeSymbol == left.typeSymbol)
        if (matching.nonEmpty)
          recur(left, matching.head)
        else
          left
      }
    }

    recur(returnTpe, formalReturnTpe)
  }

  // Infer the type of a SPARQL query.
  // This is may only be called in the controlled context of
  // initial query inference. It assumes the structure of List[TupleN] of DLTypes
  // annotated to queries and does not check for correctness in this regard!
  private def inferQueryDL(pos: Position,
                           left: Type,
                           right: List[DLEConcept],
                           sparql1mode: Boolean = false): Type = {

    def doInfer(left: Type, right: DLEConcept): Type = {
      val newT = typer.typed(Literal(Constant(right.pretty())))
      val newAnn = AnnotationInfo(typeOf[dl], List(newT), Nil)

      left match {
        // Option[DLType]
        case TypeRef(p, s, a) if left =:= typeOf[Option[DLType]] =>
          TypeRef(p, s, List(a.head.withAnnotations(newAnn :: left.annotations)))
        // DLType
        case _ =>
          left.withAnnotations(newAnn :: left.annotations)
      }
    }

    if (sparql1mode) {
      left match {
        case TypeRef(p, s, arg) =>
          val newArg = doInfer(arg.head, right.head)
          TypeRef(p, s, List(newArg))
        case _ =>
          log.report(pos, InternalError("Annotated SPARQL query type was wrong."))
          NoType
      }
    }
    else
      left match {
        // List
        case TypeRef(p, s, arg) =>
          val newArg = arg.head match {
            // TupleN
            case TypeRef(pre, sym, args) =>
              val newArgs = args.zip(right).map {
                case (l, r) => doInfer(l, r)
              }
              TypeRef(pre, sym, newArgs)
          }
          TypeRef(p, s, List(newArg))
        case _ =>
          log.report(pos, InternalError("Annotated SPARQL query type was wrong."))
          NoType
      }
  }

  // Infer DL type.
  private def inferDL(left: Type, right: Type): Type = {
    (left,right) match {
      case (NullaryMethodType(l), NullaryMethodType(r)) =>
        NullaryMethodType(inferDL(l, r))
      case (MethodType(lp, l), MethodType(rp, r)) =>
        lp.zip(rp).foreach { case (li, ri) => updateSymbol(li, inferrecur(li.tpe, ri.tpe)) }
        MethodType(lp, inferDL(l, r))
      case (PolyType(ltp, ml@MethodType(_, _)), PolyType(_, mr@MethodType(_, _))) =>
        PolyType(ltp, inferDL(ml, mr))
      case (l, r) => inferrecur(l, r)
    }
  }

  private def dropResultType(target: Type, tpe: Type): Type = {
    if (target.paramLists.lengthCompare(tpe.paramLists.size) < 0)
      dropResultType(target, tpe.resultType)
    else
      tpe
  }

  // If 'left' (or one of its type parameters) is DL type and has no @dl annotation,
  // return with the fitting @dl annotation of 'right'.
  // Otherwise, return 'left' unchanged.
  private def inferrecur(left: Type, right: Type): Type = {
    def infer(left: Type, right: Type, variance: Variance): Type = {
      // This uses == instead of =:= on purpose, because we want exactly DLType (no annotations)
      if (isBasicDLType(left) && right.resultType.staticAnnotations.nonEmpty) {
        log.infer(s"@ ${left.staticAnnotations}", s"@ ${right.resultType.staticAnnotations}")
        val ann = getDLAnnotation(right)
        if (ann.nonEmpty)
          left.withAnnotations(ann.head :: left.annotations)
        else
          left
      }
      else
        left
    }

    def mkRef(l: Type, r: Type, args: List[Type]): Type =
      TypeRef(l.prefix, l.typeSymbol, args)

    mapType(left, right, infer, (l, _) => l, mkRef, Covariant)
  }

  private def updateSymbol(sym: Symbol, tpe: Type): Unit = {
    def updaterecur(left: Type, right: Type): Type = {
      left match {
        case NullaryMethodType(_) =>
          NullaryMethodType(tpe)
        case MethodType(params, r) =>
          MethodType(params, updaterecur(r, tpe))
        case PolyType(tparams, m@MethodType(_, _)) =>
          PolyType(tparams, updaterecur(m, tpe))
        case _ => tpe
      }
    }

    val newT = updaterecur(sym.tpe, tpe)

    if (sym.tpe != newT)
      log.update(sym.tpe.toString, newT.toString)

    sym.updateInfo(newT)
  }

  // Infer type from rhs and set trees type to the inferred type.
  private def inferAndUpdate(tree: Tree, rhs: Type): Unit = tree.setType(inferDL(tree.tpe, rhs))

  // Build up type checks, according to variance,
  // if left & right are both of DLType.
  private def buildCheck(left: Type, right: Type, tpeV: Variance):
  Option[EitherDL[CheckEntity]] = {

    if (isDLType(left) && isDLType(right)) {
      val lhs = getDLType(left)
      val rhs = getDLType(right)
      // If either is empty, this tries to type check DLType
      // without annotations. This can only happen due to
      // some internal error.
      if (lhs.isEmpty || rhs.isEmpty) {
        if (Globals.brokenIsError)
          Some(Left(InternalError(s"Broken DL type")))
        else
          None
      }
      else
        tpeV match {
          case Contravariant => Some(Right(CheckContravariant(lhs.get, rhs.get)))
          case Covariant => Some(Right(CheckCovariant(lhs.get, rhs.get)))
          case Invariant => Some(Right(CheckInvariant(lhs.get, rhs.get)))
        }
    }
    // This should already be caught in Scala's typer.
    else if (isDLType(left) && !isDLType(right) && right.isFinalType && !(right =:= typeOf[Nothing]))
      Some(Left(TypeError(s"Incompatible DLType $left with $right")))
    else if (isDLType(right) && !isDLType(left) && left.isFinalType && !(left =:= typeOf[Nothing]))
      Some(Left(TypeError(s"Incompatible $left with DLType $right")))
    else
      None
  }

  // Perform DL type check on left & right.
  private def checkType(pos: Position,
                        left: Type,
                        right: Type,
                        variance: Variance = Covariant): Unit = {

    val checks = checkrecur(pos, left, right, variance)
    for (r <- checks.map(typeChecker.check)) {
      r match {
        case Result(test, error) =>
          log.echo(test)
          if (error.nonEmpty)
            log.report(pos, error.get)
        case OtherFailure(e) => log.report(pos, e)
        case EmptyResult => Unit
      }
    }
  }

  // Perform DL type check on type bounds.
  private def checkBounds(pos: Position, b: TypeBounds, t: Type): Unit = {
    // Don't check bounds, if the bounds are DLType or Nothing.
    if (!(b.lo =:= typeOf[Nothing]) && !isBasicDLType(b.lo))
      checkType(pos, b.lo, t, Contravariant)
    if (!(b.hi =:= typeOf[Nothing]) && !isBasicDLType(b.hi))
      checkType(pos, b.hi, t, Covariant)
  }

  // Recursively build up all CheckEntities for (possibly nested)
  // DL types.
  private def checkrecur(pos: Position,
                         left: Type,
                         right: Type,
                         variance: Variance):
    List[CheckEntity] = {

    def check(left: Type, right: Type, vari: Variance) = {
      val b = buildCheck(left, right, vari)
      if (b.isDefined)
        b.get match {
          case Left(e) => // DL type check, but something went wrong.
            log.report(pos, e)
            Nil
          case Right(c) => // DL type check to be performed.
            log.subtype(left.toString, right.toString)
            List(c)
        }
      else
        Nil // Not a DL type check.
    }

    def flat(l: Type, r: Type, lst: List[List[CheckEntity]]) = lst.flatten

    mapType(left, right, check, (_, _) => Nil, flat, variance)
  }

  private def checkrecur_old(pos: Position,
                        left: Type,
                        right: Type,
                        variance: Variance):
      List[CheckEntity] = {

    // If no type arguments, build simple check.
    if (!hasArgs(left) && !hasArgs(right)) {
      val b = buildCheck(left, right, variance)
      if (b.isDefined)
        b.get match {
          case Left(e) => // DL type check, but something went wrong.
            log.report(pos, e)
            Nil
          case Right(c) => // DL type check to be performed.
            log.subtype(left.toString, right.toString)
            List(c)
        }
      else
        Nil // Not a DL type check.
    }
    else {
      // If both are the same class, perform type check.
      if (sameClassAs(left, right)) {
        getTypeArgs(left)
          .zip(getVariances(left))
          .zip(getTypeArgs(right))
          .flatMap { case ((l: Type, v: Variance), r: Type) =>
            checkrecur_old(pos, l, r, v)
          }
      }
      // Otherwise, fetch the appropriate base class for 'tpe'.
      else {
        val bts = left.baseTypeSeq.toList
        val matching = bts.filter(x => x.typeSymbol == right.typeSymbol)
        // If non, tpe or pt are Wildcard, Nothing, etc.
        // ...and no type check is required. If pt is not in the base classes,
        // but an ordinary class, this would haven been caught by the type checker.
        if (matching.nonEmpty)
          checkrecur_old(pos, matching.head, right, variance)
        else
          Nil
      }
    }
  }




  class MyTraverser extends Traverser {
    override def traverse(tree: Tree): Unit = {

      super.traverse(tree) // Traverse tree depth first.

      // If tree is equality check on DL types, emit Warning.
      if (isDLEquality(tree.tpe, tree)) warnEquality(tree)

      tree match {
        case EmptyTree => Unit



        // Trivial (top level)

        case ClassDef(_/*mods*/, name, tparams, _/*impl*/) =>
          log.sig(s"class $name [ ${tparams.map(_.toString).mkString(",")} ] = _")
          log.done("Class", name.toString)

        case PackageDef(pid, _/*stats*/) =>
          log.sig(s"package $pid = _")
          log.done("Package", pid.toString)

        case ModuleDef(mods, name, _/*impl*/) =>
          log.sig(s"module $mods $name = _")
          log.done("Module", name.toString)



        // Symbol definitions (ValDef, DefDef & TypeDef)

        case ValDef(_/*mods*/, name, tpt, rhs) =>
          updateSymbol(tree.symbol, inferDL(tree.symbol.tpe, rhs.tpe))
          checkType(tree.pos, tree.symbol.tpe.resultType, rhs.tpe)
          log.sig(s"val $name: $tpt = _: ${rhs.tpe}", tpe = tree.symbol.tpe.toString)
          log.done("ValDef", name.toString)

        case DefDef(mods@Modifiers(f, n, ao), name, tpar, vparm, tpt, rhs) =>
          if (rhs != EmptyTree) { // or rhs.symbol != null?
            val inferred = inferDL(tpt.tpe, rhs.tpe)
            updateSymbol(tree.symbol, inferred)
            checkType(tree.pos, tree.symbol.tpe.resultType, rhs.tpe)
          }
          log.sig(s"def $name[${tpar.map(_.toString).mkString(", ")}](${vparm.map(_.toString).mkString(", ")}): $tpt = _: ${rhs.tpe}", tpe = tree.symbol.tpe.toString)
          log.done("DefDef", name.toString)

        case TypeDef(mods, name, tpar, rhs) =>
          // Nothing needs to be done here.
          log.sig(s"type $name[${tpar.map(_.toString).mkString(", ")}] = _: ${rhs.tpe}", tpe = tree.symbol.tpe.toString)
          log.done("TypeDef", name.toString)



        // Other definitions

        case Function(vparams, body) =>
          log.sig(s" $vparams = $body", tpe = tree.tpe.toString)
          val (mParams, mRes) = body.tpe match {
            case MethodType(params, res) => (params.map(_.tpe), res)
            case _ =>
              (List(body.tpe), body.tpe)
          }

          val newT = tree.tpe match {
            case TypeRef(pre, sym, args) =>
              val t = inferDL(args.reverse.head, mRes)
              val newArgs =
                mParams.zip(args.dropRight(1)).map { case (m,a) =>
                    inferDL(a, m)
                }
              TypeRef(pre, sym, newArgs ++ List(t))
          }
          tree.setType(newT)
          log.sig(s" $vparams = $body", tpe = tree.tpe.toString)
          log.done("Function", "?")



        // Simple structures

        case Block(stats, expr) =>
          inferAndUpdate(tree, expr.tpe)
          log.sig("{", stats.map(_.toString) ++ List(s"$expr"), s"}", tree.tpe.toString)
          log.done("Block", expr.toString)

        case Assign(lhs, rhs) =>
          checkType(tree.pos, lhs.tpe, rhs.tpe)
          log.sig(s"$lhs = $rhs", tpe = tree.tpe.toString)
          log.done("Assign", lhs + " " + rhs)

        case Ident(name) =>
          inferAndUpdate(tree, tree.symbol.tpe)
          log.sig(s"$name", tpe = tree.tpe.toString)
          log.done("Ident", name.toString)



        // Complex structures

        case If(cond, thenp, elsep) =>
          log.sig(s"if $cond {", List(thenp.toString, elsep.toString), "}", tree.tpe.toString)
          var t = lubDLRecur(thenp.tpe, elsep.tpe)
          if (t.isEmpty) t = lubDLRecur(elsep.tpe, thenp.tpe)
          if (t.isDefined) {
            inferAndUpdate(tree, t.get)
            checkType(tree.pos, tree.tpe, t.get)
          }
          log.done("If", "")

        case Match(selector, cases) =>
          log.sig(s"match $selector", cases.map(_.toString), "", tree.tpe.toString)
          val t = lubDLRecur(cases.map(_.tpe))
          if (t.isDefined) {
            inferAndUpdate(tree, t.get)
            checkType(tree.pos, tree.tpe, t.get)
          }
          log.done("Match", selector.toString)

        case CaseDef(pat, guard, body) =>
          log.sig(s"case $pat $guard => _: ${body.tpe}", tpe = tree.tpe.toString)
          inferAndUpdate(tree, body.tpe)
          log.done("Case", pat.toString)

        case Bind(name, body) =>
          log.sig(s"$name @ ${body.tpe}", tpe = tree.tpe.toString)
          inferAndUpdate(tree, body.tpe)
          log.done("Bind", name.toString)

        case Return(expr) =>
          inferAndUpdate(tree, expr.tpe)
          checkType(tree.pos, tree.symbol.tpe.resultType, expr.tpe)
          log.sig(s"return _ : ${expr.tpe}", tpe = tree.tpe.toString)
          log.done("Return", expr.tpe.toString)

        case Try(block, catches, _) =>
          val t = lubDLRecur(block.tpe :: catches.map(_.tpe))
          if (t.isDefined) {
            inferAndUpdate(tree, t.get)
            checkType(tree.pos, tree.tpe, t.get)
          }
          log.sig(s"try _ : ${block.tpe} catch ", catches.map("_ : " + _.tpe.toString), "", tree.tpe.toString)
          log.done("Try", "")

        case Throw(expr) =>
          inferAndUpdate(tree, expr.tpe)
          log.sig(s"throw _ : ${expr.tpe}", tpe = tree.tpe.toString)
          log.done("Throw", "")

        case Typed(expr, tpt) =>
          inferAndUpdate(tree, tpt.tpe)
          checkType(tree.pos, tpt.tpe, expr.tpe)
          log.sig(s"(_ : ${expr.tpe}) : $tpt", tpe = tree.tpe.toString)
          log.done("Typed", "")



        // Application

        case New(tpt) =>
          log.done("New", tpt.toString)

        case Apply(f@Select(New(t), _), args) =>
          val newT = inferAndCheckMethodDL(
            tree.pos,
            f.symbol.tpe.typeConstructor.paramTypes,
            args.map(_.tpe),
            f.symbol.tpe.typeConstructor.finalResultType,
            tree.tpe)
          tree.setType(newT)

          // Check type bounds.
          val bounds = f.symbol.tpe.typeConstructor.paramTypes.map(_.bounds)
          bounds.zip(args.map(_.tpe)).foreach { case (b, tpe) =>
            checkBounds(tree.pos, b, tpe)
          }

          log.sig(s"new $t ( ", args.map(_.toString), " )", tree.tpe.toString)
          log.done("Apply", " (new) " + t)

        // Ordinary method call on object 'obj'
        case Apply(fun@Select(obj, name), args) =>
          inferAndUpdate(tree, fun.tpe.resultType)
          fun.tpe.paramTypes.zip(args.map(_.tpe)).foreach {
            case (l , r) =>
              checkType(tree.pos, l, r)
          }
          log.sig(s" $obj . $name ( ", args.map(_.toString), " )", tree.tpe.toString)
          log.done("Apply", " (select) " + name)

        // Method call with type parameters on object 'obj'
        case Apply(tfun@TypeApply(fun@Select(o, m), targs), args) =>

          if (fun.tpe.paramLists.lengthCompare(1) > 0) {
            val allParams = fun.tpe.paramLists.flatten.map(_.tpe)

            val argSize = args.size
            val paramSize = allParams.size

            val argT =
              if (argSize < paramSize) {
                args.map(_.tpe) ++ List.fill(paramSize - argSize)(newDL(Top))
              }
              else
                args.map(_.tpe)

            val newT = inferAndCheckMethodDL(
              tree.pos,
              allParams,
              argT,
              fun.tpe.resultType.resultType, // First removes only the tfun
              tree.tpe)
            tree.setType(newT)
          }
          else {
            // Example:
            // m[A](a: A, as: List[A]): A
            // m(w, List(w, w)) with w: `:Wine`
            val newT = inferAndCheckMethodDL(
              tree.pos,
              fun.tpe.paramTypes, // (A, List[A])
              args.map(_.tpe), // (`:Wine`, List[`:Wine`])
              fun.tpe.finalResultType, // (A) (formal result type)
              tree.tpe) // (`:Wine`) (actual result type)
            tree.setType(newT)
          }

          // Check type bounds
          val bounds = fun.tpe.paramTypes.map(_.bounds)
          bounds.foreach { case TypeBounds(lo, hi) =>
              if (isDLType(lo) || isDLType(hi)) {
                log.report(tree.pos, Warning("Type bounds for method currently not implemented."))
              }
          }
          //bounds.zip(args.map(_.tpe)).foreach { case (b, tpe) =>
          //  checkBounds(tree.pos, b, tpe)
          //}

          log.sig(s"$fun [${targs.mkString(",")}] (", args.map(_.toString), ")", tree.tpe.toString)
          log.done("Apply", " (with type params) " + fun)

        // Method call, no explicit object (e.g., nested method)
        case Apply(fun, args) =>
          inferAndUpdate(tree, dropResultType(tree.tpe, fun.tpe))
          fun.tpe.paramTypes.zip(args.map(_.tpe)).foreach {
            case (l , r) => checkType(tree.pos, l, r)
          }
          log.sig(s"$fun (", args.map(_.toString), ")", tree.tpe.toString)
          log.done("Apply", " (no type params) " + fun)

        // Extract the type of SPARQL queries.
        case TypeApply(Select(Apply(Select(Apply(r1, List(Apply(_/*apply*/, queryParts))), r3), sparqlArgs), _/*instanceOf*/), List(sqt))
          if isSPARQLHelper(r1) && isSPARQLBuilder(r3) =>

          // The (substring) parts of the query.
          val temp = queryParts.map(_.toString.stripPrefix("\"").stripSuffix("\""))
          val parts = temp.head.dropWhile(_ != '|').tail :: temp.tail

          // The arguments to the query.
          val args =
            sparqlArgs.map { a =>
              val x = a.tpe

              // Set to DL type or other type.
              if (isDLType(x)) {
                val tpeDL = getDLType(x)
                if (tpeDL.isEmpty)
                  None
                else
                  Some(Right(tpeDL.get))
              }
              else
                Some(Left(x.toString))
            }

          // If any DL type without annotation was found, report error.
          if (args.exists(_.isEmpty)) {
            log.report(tree.pos, InternalError("Encountered undefined DL type in query argument."))
          }
          // Infer and set targets to query type.
          else {
            typeChecker.typeQuery(parts, args.map(_.get), isStrictSPARQLBuilder(r3)) match {
              case Left(errs) =>
                errs.foreach(log.report(tree.pos, _))
              case Right(targets) =>
                tree.setType(inferQueryDL(tree.pos, tree.tpe, targets,
                  sparql1mode = isSPARQL1Builder(r3)))
            }
          }

          log.sig("SPARQL query")
          log.done("TypeApply", "SPARQL")

        case TypeApply(fun@(Select(q, n)), args) =>
          log.sig(s"$fun [", args.map(_.toString), "]", tree.tpe.toString)
          log.done("TypedApply", fun.toString)


        // Selection

        case Super(q, m) =>
          log.sig(s"$q . $m", tpe = tree.tpe.toString)
          log.done("Super", q.toString)

        case This(q) =>
          inferAndUpdate(tree, tree.symbol.tpe)
          log.sig(s"$q . this", tpe = tree.tpe.toString)
          log.done("This", q.toString)

        case Select(q, name) =>
          if (tree.symbol.isMethod) {
            if (paramOrSkolem(tree.symbol.tpe.resultType) && q.symbol != null) {

              val temp = q.tpe.baseTypeSeq.toList.find(x => x.typeSymbol == tree.symbol.enclClass.tpe.typeSymbol)
              val aBaseT = if(temp.isDefined) temp.get else q.tpe

              val left = tree.symbol.enclClass.tpe.typeArgs.map(_.typeSymbol)
              val right = aBaseT.typeArgs.map(_.typeSymbol)

              if (left.nonEmpty && right.nonEmpty) {
                val t = tree.symbol.tpe.resultType.substSym(left, right)

                val newResult = inferAndCheckMethodDL(
                  tree.pos,
                  q.tpe.typeParams.map(_.tpe),
                  q.symbol.tpe.resultType.typeArgs,
                  t,
                  tree.tpe.resultType)

                val newT = tree.tpe match {
                  case MethodType(p, r) =>
                    MethodType(p, newResult)
                  case _ => newResult
                }

                tree.setType(newT)
              }
            }
            else {
              inferAndUpdate(tree, tree.symbol.tpe)
            }
          }
          else {
            inferAndUpdate(tree, tree.symbol.tpe)
          }

          log.sig(s"$q . $name", tpe = tree.tpe.toString)
          log.done("Select", q + " " + name)

        case _ => Unit
      }
    }
  }
}
