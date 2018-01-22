package de.uni_koblenz.dltypes

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

import de.uni_koblenz.dltypes.components._


class DLTypes (override val global: Global) extends Plugin {
  override val name: String = "dltypes"
  override val description: String = "Typed integration of SPARQL queries into the Scala programming language."

  override val components: List[PluginComponent] =
    new Collector(global) ::
    new Typedef(global) ::
    new Checker(global) ::
    Nil
}




