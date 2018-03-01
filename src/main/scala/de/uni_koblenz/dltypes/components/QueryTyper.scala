package de.uni_koblenz.dltypes.components

import de.uni_koblenz.dltypes.tools._


object QueryTyper {
  def noArgs(query: String): DLEConcept = {
    // TODO
    val newType: String = ":RedWine"
    Concept(newType)
  }

  def withArgs(queryParts: List[String], arguments: List[String]): DLEConcept = {
    // TODO
    val newType: String = ":RedWine"
    Concept(newType)
  }
}
