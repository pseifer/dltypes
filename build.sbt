organization := "de.uni_koblenz"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.4"
name := "dltypes"

libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
libraryDependencies += "de.uni_koblenz" %% "dltypes-runtime" % "0.0.1-SNAPSHOT"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6"
libraryDependencies += "com.hermit-reasoner" % "org.semanticweb.hermit" % "1.3.8.1"

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.withClassifier(Some("assembly"))
}

addArtifact(artifact in (Compile, assembly), assembly)

assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}