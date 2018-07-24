# ScaSpa -- Typesafe Integration of SPARQL into Scala
This is the main repository of the ScaSpa compiler plugin. The runtime library can
be found over at [```dltypes-lib```](https://github.com/pseifer/dltypes-lib). A minimal example demonstrating setup 
and usage of the plugin can be found in the [```dltypes-example```](https://github.com/pseifer/dltypes-example) repository.

## Building ScaSpa
1. Clone the [```dltypes-lib```](https://github.com/pseifer/dltypes-lib) repository.
2. Compile and use ```sbt publishLocal``` to provide the library locally via Ivy.
3. Clone this repository.
4. Build it (using ```sbt compile```).
5. Tests can be executed using ```sbt test```.

## Using the plugin
In order to use the plugin, assemble the file using ```sbt assemble``` and provide the ```dltypes.jar``` 
to the compiler. Using the local Ivy repository for the plugin itself is not recommended. Instead, the
JAR should be provided directly, see also the [```dltypes-example```](https://github.com/pseifer/dltypes-example)  repository for an example.

The following is a minimal sbt configuration file required to use the plugin. In order to use
the plugin, it has to be added to the compiler (1). Via the ```-P:dltypes``` flag, arguments
can be forwarded to the plugin. ScaSpa requires an ontology (2). This may either be a local
file or an URI. Additionally, the default prefix can be explicitly defined (3). It may be omitted,
in which case it is set to the ontology.

```sbt
name := "cool app name"
version := "the version"
scalaVersion := "2.12.4"

libraryDependencies += "de.uni_koblenz" %% "dltypes-lib" % "0.0.1-SNAPSHOT"
libraryDependencies += "com.complexible.stardog" % "client-http" % "5.2.1"


scalacOptions += "-P:dltypes:ontology:http://swat.cse.lehigh.edu/onto/univ-bench.owl"

// (1) Load ScaSpa.
scalacOptions += "-Xplugin:lib/dltypes.jar"
// (2) Ontology.
scalacOptions += "-P:dltypes:ontology:http://swat.cse.lehigh.edu/onto/univ-bench.owl"
// (3) Prefix (may sometimes be omitted).
// scalacOptions += "-P:dltypes:prefix:http://swat.cse.lehigh.edu/onto/univ-bench.owl#"
```

## 
