package de.uni_koblenz.dltypes

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

import de.uni_koblenz.dltypes.components._


class DLTypes (override val global: Global) extends Plugin {
  override val name: String = "dltypes"
  override val description: String = "Typed integration of SPARQL queries into the Scala programming language."

  // Plugin that can be used to report things happening to AnalyzerPlugin callbacks.
  new EchoAnalyzerPlugin(global, List("pluginsTyped")).addToPhase("namer")
  new EchoAnalyzerPlugin(global, List("pluginsTyped")).addToPhase("typer")

  // Plugin that checks DL types in the typer phase.
  new CheckerAnalyzerPlugin(global).add()

  override val components: List[PluginComponent] =
    // Workaround: Collect everything that is a DL type.
    // Note: Also handles IRI transformations for now.
    new Collector(global) ::
    // Workaround: Add typedefs for types collected in Collector.
    new Typedef(global) ::
    Nil
}
