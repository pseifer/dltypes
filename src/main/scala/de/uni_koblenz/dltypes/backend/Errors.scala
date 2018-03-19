package de.uni_koblenz.dltypes.backend



// Is used when Evaluator encounters a query expression,
// for which no type can be deduced. Usually, this should
// be detected earlier and result in a NotTypeableError.
class FaultyQueryExpressionError extends Exception {
  override def getMessage(): String = "Impossible to type query (FaultyQueryExpression)."
}


// Is used when any DL expression for a variable in a
// SPARQL query can't be shown to be satisfiable in Evaluator.
class UnsatisfiableQueryError extends Exception {
  override def getMessage(): String = "The query is not satisfiable."
}


// Is used in cases where openrdf fails to parse
// a query (i.e., the query itself invalid) in SPARQLParser.
class InvalidSPARQLError(msg: String) extends Exception {
  override def getMessage(): String = msg
}


// Is used in cases where it is already known
// that the query can't be typed by SPARQLParser.
class NotTypeableError(msg: String) extends Exception {
  override def getMessage(): String = msg
}


// Is used when QueryTyper encounters a scala type argument
// to a query that is unknown.
class NoSuchXSDTypeError(s: String) extends Exception {
  override def getMessage(): String = s"No mapping to XSD types for '$s' exists."
}
