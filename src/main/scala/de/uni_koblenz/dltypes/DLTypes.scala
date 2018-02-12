package de.uni_koblenz.dltypes

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

import de.uni_koblenz.dltypes.components._


class DLTypes (override val global: Global) extends Plugin {
  override val name: String = "dltypes"
  override val description: String = "Typed integration of SPARQL queries into the Scala programming language."

  override def init(options: List[String], error: String => Unit): Boolean = {
    options.find(_.startsWith("ontology:")) match {
      case Some(option) =>
        MyGlobal.ontologies += option.drop("ontology:".length)
        true
      case _ =>
        error("-P:dltypes:ontology not specified")
        false
    }
  }

  override val optionsHelp: Option[String] = Some(
    "-P:dltypes:ontology:s        use ontology 's' in resource folder.")

  // Plugin that can be used to report things happening to AnalyzerPlugin callbacks.
  //new EchoAnalyzerPlugin(global, List("pluginsTyped", "pluginsPt")).addToPhase("typer")

  // Plugin that checks DL types in the typer phase.
  new CheckerAnalyzerPlugin(global).add(printchecks=true, debug=true)

  override val components: List[PluginComponent] =
    new Collector(global) ::
    new Typedef(global) ::
    Nil
}
