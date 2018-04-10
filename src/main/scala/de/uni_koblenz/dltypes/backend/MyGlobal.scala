package de.uni_koblenz.dltypes.backend

import de.uni_koblenz.dltypes.tools.DLEConcept
import scala.collection.mutable


// TODO move to errors
class NoDefaultOntologyError extends Exception


object MyGlobal {
  // Generator for query types.
  private object Gensym {
    private var c: Int = 0

    def freshQ(): String = {
      c += 1
      s"SparqlQueryType$c"
    }

    def freshI(): String = {
      c += 1
      s"InferredDLType$c"
    }

    def freshD(): String = {
      c += 1
      s"RegisteredDLType$c"
    }
  }

  // Generate and register a new SPARQL query type (placeholder).
  def newSparqlQueryType(): String = {
    val t = Gensym.freshQ()
    symbolTable += t
    qtypeTable += t -> None
    t
  }

  // Generate and register a new DL type.
  def newDLType(dl: DLEConcept): String = {
    val t = Gensym.freshD()
    symbolTable += t
    dtypeTable += t -> dl
    t
  }

  // Generate and register a new inferred DL type (placeholder).
  def newInferredDLType(): String = {
    val t = Gensym.freshI()
    symbolTable += t
    itypeTable += t -> (None, Nil)
    t
  }

  // Settings

  // This activates strict typing mode for queries,
  // where subsumption for constraints is checked vs the argument type.
  // This is not suitable for most practical applications.
  val strictQueryTyping = false

  // If strictQueryTyping is active, this setting regulates
  // whether warnings or errors are produced in (strict) error cases.
  // TODO: currently unused!
  val strictWarningsOnly = false

  // All names (DL types) that where encountered during the collector phase.
  val symbolTable: mutable.Set[String] = mutable.Set()

  // Mapping from SparqlQueryTypes to the respective DLEConcepts.
  // This gets initialized with None values when the type is generated and set
  // to the DLEConcept when the query is typed.
  val qtypeTable: mutable.Map[String, Option[List[DLEConcept]]] = mutable.Map()

  // TODO: Description
  val dtypeTable:  mutable.Map[String, DLEConcept] = mutable.Map()

  // Similar to qtypeTable, but for types that request explicitly to infer DL type
  // via the `???` constant.
  val itypeTable: mutable.Map[String, (Option[List[DLEConcept]], List[DLEConcept])] = mutable.Map()

  // Map of ontologies (Prefix -> IRI).
  // Currently only contains a single mapping from ':' to the ontology
  // specified on the command line.
  val ontologies: mutable.Map[String, String] = mutable.Map()
}
