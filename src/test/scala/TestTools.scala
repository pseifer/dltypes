import java.net.URLClassLoader

import scala.io.Source
import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.util.ClassPath
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.io.VirtualDirectory
import de.uni_koblenz.dltypes.DLTypes


object CompilationError extends Exception

// TODO: Clean this up.

object TestTools {
  var LOGLVL = 0
  private val testsep = "\n\n--------------------------------------------------------------------------------"

  // The compiler settings.
  val settings = new Settings

  // Add "scala-compiler.jar" and "scala-library.jar" to class path.
  // This is required for SBT.
  val loader: URLClassLoader = getClass.getClassLoader.asInstanceOf[URLClassLoader]
  val entries: Array[String] = loader.getURLs.map(_.getPath)
  val path: Option[String] = entries.find(_.endsWith("scala-compiler.jar"))
    .map(_.replaceAll("scala-compiler.jar", "scala-library.jar"))
  settings.classpath.value = ClassPath.join(entries ++ path : _*)

  // Use a virtual directory for compilation files (.class).
  val virtualDir = new VirtualDirectory("(memory)", None)
  settings.outputDirs.setSingleOutput(virtualDir)

  // Construct test case, wrap in requirements and App class.
  def mkTest(test: String): List[BatchSourceFile] = {
    val code =
      List("import de.uni_koblenz.dltypes.runtime._",
        "import de.uni_koblenz.dltypes.runtime.Sparql._",
        "class Main extends App {",
        test,
        "}")
    List(new BatchSourceFile("test", code.mkString("\n")))
  }

  // Run compiler on a single test case.
  def testCase(name: String, test: String): Unit = {
    // Construct the test case.
    val sources = mkTest(test)

    LOGLVL match {
      case 0 => Unit
      case 1 => println(testsep + "\n" + name)
      case 2 => println(testsep + "\n" + name + "\n" + test)
    }

    // Remove potential class files from previous test cases.
    virtualDir.clear

    // Prepare the compiler.
    // TODO: Is is strictly necessary to instantiate a new compiler
    // TODO: for each test case? Any way to reset the compiler after error?
    val compiler = new Global(settings, new ConsoleReporter(settings)) {
      override protected def computeInternalPhases () {
        super.computeInternalPhases
        // Load all the DLTypes phases.
        for (phase <- new DLTypes(this).components)
          phasesSet += phase
      }
    }
    val run = new compiler.Run()
    run.compileSources(sources)

    // No .class files were created -> compilation failed.
    if (virtualDir.toList.isEmpty) throw CompilationError
  }

  // Load all files for a given test case group (folder).
  // Returns tuple of file name and contents (test code).
  def loadFolder(key: String): List[(String, String)] = {
    Source
      .fromResource(key)
      .mkString
      .split('\n')
      .toList
      .map { f =>
        (key + ":" + f.split('.').head, // Remove file extension from name.
          Source.fromResource(key + "/" + f).getLines.mkString("\n"))
      }
  }
}
