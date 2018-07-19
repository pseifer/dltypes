package de.uni_koblenz.dltypes

import de.uni_koblenz.dltypes.backend.Globals

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import de.uni_koblenz.dltypes.components._
import org.semanticweb.owlapi.model.IRI


class DLTypes (override val global: Global) extends Plugin {
  override val name: String = "dltypes"
  override val description: String = "Typed integration of SPARQL queries into the Scala programming language."

  override def init(options: List[String], error: String => Unit): Boolean = {
    var success: Boolean = true
    // ONTOLOGY
    options.find(_.startsWith("ontology:")) match {
      case Some(option) =>
        Globals.ontology = IRI.create { option.drop("ontology:".length) }
        success = true
      case None =>
        error("-P:dltypes:ontology not specified")
        success = false
    }
    // TODO: Possible to set prefix from ontology, when not specified?
    // PREFIX
    options.find(_.startsWith("prefix:")) match {
      case Some(option) =>
        Globals.prefixes += ":" -> option.drop("prefix:".length)
        success = true
      case None =>
        Globals.prefixes += ":" -> (Globals.ontology.toString + "#")
        success = true
        //error("-P:dltypes:prefix not specified")
        //success = false }
    }
    success
  }

  // TODO:
  // Arguments to add more ontologies.
  // Possible to add 1 ontology for ':'
  // All further need prefix, which has to be used in queries and types.
  // Pass over to runtime as well.

  override val optionsHelp: Option[String] = Some(
    """-P:dltypes:ontology:s        use ontology 's' in resource folder.
      |-P:dltypes:prefix:p          use prefix 'p' as default.
      |                             if no ontology is given, use 'p' as ontology.
    """.stripMargin)

  override val components: List[PluginComponent] =
    new TypecaseTransformationPhase(global) ::
    new TransformerPhase(global) ::
    new CheckerPhase(global) ::
    Nil
}
