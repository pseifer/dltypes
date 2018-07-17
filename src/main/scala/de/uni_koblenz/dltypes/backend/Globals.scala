package de.uni_koblenz.dltypes.backend

import org.semanticweb.owlapi.model.IRI

import scala.collection.mutable


object Globals {
  // Reporter settings.
  type LogFlag = Int

  val PRINT_CHECKS: LogFlag  =  1 // 00 0001  DL type checks
  val PRINT_SIGS: LogFlag    =  2 // 00 0010  Handled tree nodes
  val PRINT_SUBTYPE: LogFlag =  4 // 00 0100  Subtype checks
  val PRINT_INFER: LogFlag   =  8 // 00 1000  Type inference
  val PRINT_UPDATE: LogFlag  = 16 // 01 0000  Updated type information
  val PRINT_EXTRA: LogFlag   = 32 // 10 0000  Additional (debug) info
  val ALL: LogFlag           = 63 // 11 1111  All of the above
  val NONE: LogFlag          =  0 // 00 0000  None of the above

  var logSettings = NONE

  // Test if particular logging flag is set.
  // Used by the CompilationReporter.
  def flag(f: LogFlag): Boolean = {
    if ((logSettings & f) == f)
      true
    else
      false
  }

  // Type checker instantiation takes time, because the reasoner
  // has to be instantiated. Cache it in global, access same instance everywhere.
  // Note: Is not object, must be instantiated with delay.
  lazy val typeChecker = new TypeChecker

  // This activates strict typing mode for queries,
  // where subsumption for constraints is checked vs the argument type.
  // This is not suitable for most practical applications.
  var strictQueryTyping = false

  // Regulates whether ABox reasoning is enabled at compile time.
  var doABoxReasoning = true

  // If strictQueryTyping is active, this setting regulates
  // whether warnings or errors are produced in (strict) error cases.
  // TODO: currently unused!
  var strictWarningsOnly = false

  // If this is false, do not issue error when 'broken' DL types
  // (i.e., raw DL types where annotated DL types are expected)
  // are encountered. Settings this to false disables type safety.
  // Use only for testing or debugging, otherwise should be true.
  var brokenIsError: Boolean = true

  // If this is set, test all annotated types for satisfiability.
  val warnIfNotDefinedOrSatisfiable = true

  // Store the bound prefixes and the ontology source.
  val prefixes: mutable.Map[String, String] = mutable.Map()
  var ontology: IRI = IRI.create("")

  // Get all prefixes as SPARQL prefix expression(s).
  def getPrefixes: String = {
    val t = prefixes map { pre =>
      s"PREFIX ${pre._1} <${pre._2}>"
    }
    t.tail.fold(t.head)(_ + " " + _)
  }
}
