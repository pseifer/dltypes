package de.uni_koblenz.dltypes
package backend

import de.uni_koblenz.dltypes.backend.DLTypesError.EitherDL
import org.openrdf.query.algebra.{Difference, LeftJoin, Projection}
import org.openrdf.query.algebra.helpers.AbstractQueryModelVisitor
import org.openrdf.query.parser.sparql.{SPARQLParser => ORDFParser}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


class SPARQLParser {

  private val ordfp = new ORDFParser

  // Internal query type representation.
  sealed trait QType
  case object Select extends QType
  case object Ask extends QType

  // InvalidSPARQLError   if the query can not be parsed.
  // NotTypeableError     if the query is valid, but can't be typed.
  def parse(query: String): EitherDL[SPARQLQuery] = {
    val qs = Try(ordfp.parseQuery(query, Globals.prefixes(":"))) match {
      case Success(s) => Right(s)
      case Failure(e) => Left(SPARQLError("Unable to parse query, because: " + e.getMessage))
    }
    val visitor = new ParseVisitor

    qs.flatMap { q =>
      q.getTupleExpr.visit(visitor)
      val vs = q.getTupleExpr.getBindingNames.iterator.asScala.toList.map(Var)
      visitor.getQueryExpression.flatMap { qe =>
        visitor.getQType.flatMap {
          case Ask => Right(AskQuery(Nil, qe))
          // TODO: vs returns a list of all variables. However, if SELECT * is used,
          // this should be empty list instead. At some point, need to check if
          // vs contains all the variables.
          // Also contains vars in order of occurrence (for * cases) which is different
          // from evaluator, which used alphabetical order.
          case Select => Right(SelectQuery(Nil, vs, qe))
        }
      }
    }
  }

  class ParseVisitor extends AbstractQueryModelVisitor[Exception] { // TODO: Exception
    import org.openrdf.query.algebra.{Join, Union, Var => OVar, StatementPattern}
    import scala.collection.mutable.Stack

    private val qstack = new Stack[QueryExpression]
    private val TYPEOF = List(Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))

    // Validity flag, might unset by any meet for certain nodes.
    private var errorFlag: Option[String] = None
    private var qtype: Option[QType] = None

    def getQueryExpression: EitherDL[QueryExpression] = {
      // If not exactly one, something is wrong.
      if (errorFlag.isDefined)
        Left(InferenceError(s"Query can not be typed: ${errorFlag.get}"))
      else if (qstack.size != 1)
        Left(InferenceError("Query can not be typed: Error in syntax tree construction."))
      else
        Right(qstack.head)
    }

    def getQType: EitherDL[QType] = {
      qtype match {
        case None =>
          Left(InferenceError("Can only infer types for SELECT and ASK queries."))
        case Some(q) =>
          Right(q)
      }
    }

    override def meet(node: Union): Unit = {
      super.meet(node)
      val r = qstack.pop()
      val l = qstack.pop()
      qstack.push(Disjunction(l, r))
    }

    override def meet(node: Join): Unit = {
      super.meet(node)
      val r = qstack.pop()
      val l = qstack.pop()
      qstack.push(Conjunction(l, r))
    }

    override def meet(node: LeftJoin): Unit = {
      super.meet(node)
      val r = qstack.pop()
      val l = qstack.pop()
      qstack.push(Optional(l, r))
    }

    override def meet(node: Difference): Unit = {
      super.meet(node)
      val r = qstack.pop()
      val l = qstack.pop()
      qstack.push(Minus(l, r))
    }

    // Triple pattern.
    override def meet(node: StatementPattern): Unit = {
      super.meet(node)
      val s = varHelper(node.getSubjectVar)
      val p = varHelper(node.getPredicateVar)
      val o = varHelper(node.getObjectVar)

      if (TYPEOF.contains(p)) // subject 'a' object
        qstack.push(ConceptAssertion(s, o))
      else
        p match {
          case Var(_) =>
            errorFlag = Some("Variable in predicate position.")
          case pi @ Iri(_) => qstack.push(RoleAssertion(s, o, pi))
        }
    }

    // Projection: SELECT <ProjectionElemList: <ProjectionElem1> <...>> WHERE { ... }
    override def meet(node: Projection): Unit = {
      super.meet(node)
      qtype = Some(Select)
    }

    private def varHelper(v: OVar): Name = {
      // If the node has a value, it might be...
      if (v.hasValue)
        // ...either a plain IRI or a typed literal.
        Name.parse(v.getValue.toString)
      else
        // Otherwise, it's a variable.
        Var(v.getName)
    }
  }
}

/*
class BasicVisitor extends org.openrdf.query.algebra.QueryModelVisitor[Exception] {
  import org.openrdf.query.algebra._
  def meet(node: Add): Unit = node.visitChildren(this)
  def meet(node: And): Unit = node.visitChildren(this)
  def meet(node: ArbitraryLengthPath): Unit = node.visitChildren(this)
  def meet(node: Avg): Unit = node.visitChildren(this)
  def meet(node: BindingSetAssignment): Unit = node.visitChildren(this)
  def meet(node: BNodeGenerator): Unit = node.visitChildren(this)
  def meet(node: Bound): Unit = node.visitChildren(this)
  def meet(node: Clear): Unit = node.visitChildren(this)
  def meet(node: Coalesce): Unit = node.visitChildren(this)
  def meet(node: Compare): Unit = node.visitChildren(this)
  def meet(node: CompareAll): Unit = node.visitChildren(this)
  def meet(node: CompareAny): Unit = node.visitChildren(this)
  def meet(node: Copy): Unit = node.visitChildren(this)
  def meet(node: Count): Unit = node.visitChildren(this)
  def meet(node: Create): Unit = node.visitChildren(this)
  def meet(node: Datatype): Unit = node.visitChildren(this)
  def meet(node: DeleteData): Unit = node.visitChildren(this)
  def meet(node: DescribeOperator): Unit = node.visitChildren(this)
  def meet(node: Difference): Unit = node.visitChildren(this)
  def meet(node: Distinct): Unit = node.visitChildren(this)
  def meet(node: EmptySet): Unit = node.visitChildren(this)
  def meet(node: Exists): Unit = node.visitChildren(this)
  def meet(node: Extension): Unit = node.visitChildren(this)
  def meet(node: ExtensionElem): Unit = node.visitChildren(this)
  def meet(node: Filter): Unit = node.visitChildren(this)
  def meet(node: FunctionCall): Unit = node.visitChildren(this)
  def meet(node: Group): Unit = node.visitChildren(this)
  def meet(node: GroupConcat): Unit = node.visitChildren(this)
  def meet(node: GroupElem): Unit = node.visitChildren(this)
  def meet(node: If): Unit = node.visitChildren(this)
  def meet(node: In): Unit = node.visitChildren(this)
  def meet(node: InsertData): Unit = node.visitChildren(this)
  def meet(node: Intersection): Unit = node.visitChildren(this)
  def meet(node: IRIFunction): Unit = node.visitChildren(this)
  def meet(node: IsBNode): Unit = node.visitChildren(this)
  def meet(node: IsLiteral): Unit = node.visitChildren(this)
  def meet(node: IsResource): Unit = node.visitChildren(this)
  def meet(node: IsNumeric): Unit = node.visitChildren(this)
  def meet(node: IsURI): Unit = node.visitChildren(this)
  def meet(node: Join): Unit = node.visitChildren(this)
  def meet(node: Label): Unit = node.visitChildren(this)
  def meet(node: Lang): Unit = node.visitChildren(this)
  def meet(node: LangMatches): Unit = node.visitChildren(this)
  def meet(node: LeftJoin): Unit = node.visitChildren(this)
  def meet(node: Like): Unit = node.visitChildren(this)
  def meet(node: ListMemberOperator): Unit = node.visitChildren(this)
  def meet(node: Load): Unit = node.visitChildren(this)
  def meet(node: LocalName): Unit = node.visitChildren(this)
  def meet(node: MathExpr): Unit = node.visitChildren(this)
  def meet(node: Max): Unit = node.visitChildren(this)
  def meet(node: Min): Unit = node.visitChildren(this)
  def meet(node: Modify): Unit = node.visitChildren(this)
  def meet(node: Move): Unit = node.visitChildren(this)
  def meet(node: MultiProjection): Unit = node.visitChildren(this)
  def meet(node: Namespace): Unit = node.visitChildren(this)
  def meet(node: Not): Unit = node.visitChildren(this)
  def meet(node: Or): Unit = node.visitChildren(this)
  def meet(node: Order): Unit = node.visitChildren(this)
  def meet(node: OrderElem): Unit = node.visitChildren(this)
  def meet(node: Projection): Unit = node.visitChildren(this)
  def meet(node: ProjectionElem): Unit = node.visitChildren(this)
  def meet(node: ProjectionElemList): Unit = node.visitChildren(this)
  def meet(node: QueryRoot): Unit = node.visitChildren(this)
  def meet(node: Reduced): Unit = node.visitChildren(this)
  def meet(node: Regex): Unit = node.visitChildren(this)
  def meet(node: SameTerm): Unit = node.visitChildren(this)
  def meet(node: Sample): Unit = node.visitChildren(this)
  def meet(node: Service): Unit = node.visitChildren(this)
  def meet(node: SingletonSet): Unit = node.visitChildren(this)
  def meet(node: Slice): Unit = node.visitChildren(this)
  def meet(node: StatementPattern): Unit = node.visitChildren(this)
  def meet(node: Str): Unit = node.visitChildren(this)
  def meet(node: Sum): Unit = node.visitChildren(this)
  def meet(node: Union): Unit = node.visitChildren(this)
  def meet(node: ValueConstant): Unit = node.visitChildren(this)
  def meet(node: Var): Unit = node.visitChildren(this)
  def meet(node: ZeroLengthPath): Unit = node.visitChildren(this)
  def meetOther(node: QueryModelNode): Unit = node.visitChildren(this)
}
*/

