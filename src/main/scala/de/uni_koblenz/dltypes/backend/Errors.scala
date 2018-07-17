package de.uni_koblenz.dltypes.backend

object DLTypesError {
  type EitherDL[T] = Either[DLTypesError, T]
}

sealed trait DLTypesError {
  def show: String
  val prefix = "[DL]"
}

case class InternalError(info: String) extends DLTypesError {
  def show: String = s"$prefix Internal error: $info"
}

case class TypeError(info: String) extends DLTypesError {
  def show: String = s"$prefix Type error: $info"
}

case class Warning(info: String) extends DLTypesError {
  def show: String = s"$prefix - $info"
}

case class InferenceError(info: String) extends DLTypesError {
  def show: String = s"$prefix Inference error: $info"
}

case class SPARQLError(info: String) extends DLTypesError {
  def show: String = s"$prefix SPARQL error: $info"
}
