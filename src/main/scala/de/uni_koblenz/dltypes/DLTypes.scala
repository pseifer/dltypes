package de.uni_koblenz.dltypes

import de.uni_koblenz.dltypes.backend.MyGlobal

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import de.uni_koblenz.dltypes.components._


class DLTypes (override val global: Global) extends Plugin {
  override val name: String = "dltypes"
  override val description: String = "Typed integration of SPARQL queries into the Scala programming language."

  override def init(options: List[String], error: String => Unit): Boolean = {
    var success: Boolean = true
    // ONTOLOGY
    options.find(_.startsWith("ontology:")) match {
      case Some(option) =>
        //MyGlobal.ontologies += ":" -> option.drop("ontology:".length)
        MyGlobal.ontology = option.drop("ontology:".length)
        success = true
      case _ =>
        error("-P:dltypes:ontology not specified")
        success = false
    }
    // PREFIX
    options.find(_.startsWith("prefix:")) match {
      case Some(option) =>
        MyGlobal.prefixes += ":" -> option.drop("prefix:".length)
        success = true
      case _ =>
        error("-P:dltypes:ontology not specified")
        success = false
    }
    success
  }

  // TODO:
  // Arguments to add more ontologies.
  // Possible to add 1 ontology for ':'
  // All further need prefix, which has to be used in queries and types.
  // Pass over to runtime as well.

  override val optionsHelp: Option[String] = Some(
    "-P:dltypes:ontology:s        use ontology 's' in resource folder.")

  // Plugin that can be used to report things happening to AnalyzerPlugin callbacks.
  //new EchoAnalyzerPlugin(global, List("pluginsTyped", "pluginsPt")).addToPhase("typer")
  //new EchoAnalyzerPlugin(global, List("pluginsNotifyImplicitSearch", "pluginsNotifyImplicitSearchResult")).addToPhase("typer")
  //new EchoAnalyzerPlugin(global, List("pluginsTypeSig")).addToPhase("typer")

  // Plugin that checks DL types in the typer phase.
  new CheckerAnalyzerPlugin(global).add(printchecks=true, debug=true)

  override val components: List[PluginComponent] =
    new TypecaseTransformationPhase(global) ::
    new CollectorPhase(global) ::
    new TypedefPhase(global) ::
    Nil
}
