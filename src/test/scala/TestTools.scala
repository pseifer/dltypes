import java.net.URLClassLoader

import scala.io.Source
import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.util.ClassPath
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.io.VirtualDirectory
import de.uni_koblenz.dltypes.DLTypes
import de.uni_koblenz.dltypes.backend.Globals
import org.semanticweb.owlapi.model.IRI

import scala.collection.mutable.ListBuffer


class TestTools(val loglvl: Int = 0, val debug: Boolean = false) {

  // Public API

  // 0 => No logging
  // 1 => Print test names
  // 2 => Above + code
  // 3 => Above + enable dumps after 'typer' and 'superaccessor'

  // Run compiler on a single test case.
  def testCase(test: TestCase): (Boolean, Boolean) = test match {
    case TestCase(_, n, _, code) =>
      testCase(n, code)
  }

  def customReport(before: Long, after: Long, size: Int) = {
    println(
      s"""
         |---
         |TIME ${after - before} ms
         |SIZE $size
         |---
       """.stripMargin)
  }

  def testCase(name: String, test: String): (Boolean, Boolean) = {
    // Construct the test case.
    val sources = mkTest(test)

    loglvl match {
      case 0 => Unit
      case 1 => println(80 * '-' + "\n" + name)
      case _ => println(80 * '-' + "\n" + name + "\n" + test)
    }

    // Reset the reporter.
    reporter.reset()

    val run = new compiler.Run()

    val before = System.currentTimeMillis()
    run.compileSources(sources)
    val after = System.currentTimeMillis()

    customReport(before, after, virtualDir.size)

    (reporter.hasErrors, reporter.hasWarnings)
  }

  def freeTestCase(name: String, test: String): (Boolean, Boolean) = {
    val sources = mkTestNormal(test)

    defaultReporter.reset()
    val run = new defaultCompiler.Run()

    val before = System.currentTimeMillis()
    run.compileSources(sources)
    val after = System.currentTimeMillis()

    customReport(before, after, virtualDir.size)

    (reporter.hasErrors, reporter.hasWarnings)
  }

  // Parse test case Markdown file.
  def parse(s: String): this.type  = {
    cases.clear()
    val lines = Source
      .fromResource(s)
      .getLines
      .map(classifyLine)
      .filterNot(x => x == SomeLine(""))
      .toList
    doParse(lines)
    this
  }

  // Filter out single category.
  def filterCategory(cat: String): this.type = {
    cases = cases.filter( x => x match {
      case TestCase(_, _, c, _) => c == cat
    })
    this
  }

  // Remove single category from test cases.
  def filterNotCategory(cat: String): this.type = {
    cases = cases.filterNot( x => x match {
      case TestCase(_, _, c, _) => c == cat
    })
    this
  }

  def warning(t: (Boolean, Boolean)): Unit = assert(t._2 == true)
  def failure(t: (Boolean, Boolean)): Unit = assert(t._1 == true)
  def success(t: (Boolean, Boolean)): Unit = assert(t._1 == false)

  // Apply f for all cases matching TestResult r.
  def onlyFor(r: TestResult, f: (String, TestCase) => Unit): this.type = {
    cases.foreach { x => x match {
      case TestCase(`r`, name, _, _) => f(name, x)
      case _ => Unit
    }}
    this
  }

  // Internal (Here be dragons)

  // Internal storage for test cases.
  private var cases = ListBuffer[TestCase]()

  // Possible results for tests.
  sealed trait TestResult
  object Failure extends TestResult
  object Success extends TestResult
  object Warning extends TestResult

  // Individual test case (in .md file).
  case class TestCase(result: TestResult,
                      name: String,
                      category: String,
                      code: String)

  // Parse markdown-ish test cases file.
  sealed trait Line
  case class Result(result: TestResult) extends Line
  case class Category(name: String) extends Line
  case class TestCaseName(name: String) extends Line
  object CodeBegin extends Line
  object CodeEnd extends Line
  case class SomeLine(line: String) extends Line

  // Tokenize lines.
  private def classifyLine(line: String): Line = {
    line match {
      case "## Failure" => Result(Failure)
      case "## Success" => Result(Success)
      case "## Warning" => Result(Warning)
      case name if name.startsWith("# ") => Category(name.drop(2))
      case name if name.startsWith("#### ") => TestCaseName(name.drop(5))
      case "```scala" => CodeBegin
      case "```" => CodeEnd
      case s => SomeLine(s)
    }
  }

  private def parseTestCase(r: TestResult,
                            c: String,
                            n: String,
                            code: String,
                            lines: List[Line]): List[Line] = lines match {
    case x :: xs => x match {
      case CodeEnd =>
        r match {
          case Success => cases += TestCase(r, "[S] " + c + " > " + n, c, code)
          case Failure => cases += TestCase(r, "[F] " + c + " > " + n, c, code)
          case Warning => cases += TestCase(r, "[W] " + c + " > " + n, c, code)
        }
        xs
      case SomeLine(l) => parseTestCase(r, c, n, code + "\n" + l, xs)
      case _ => xs
    }
    case x => x
  }

  private def parseIn(r: TestResult, c: String, n: String, lines: List[Line]): Unit = lines match {
    case Nil => Unit
    case x :: xs => x match {
      case Result(Failure) => parseIn(Failure, c, n, xs)
      case Result(Success) => parseIn(Success, c, n, xs)
      case Result(Warning) => parseIn(Warning, c, n, xs)
      case Category(c1) => parseIn(r, c1, n, xs)
      case TestCaseName(n1) => parseIn(r, c, n1, xs)
      case CodeBegin =>
        parseIn(r, c, "<nocase>", parseTestCase(r, c, n, "", xs))
      case _ => parseIn(r, c, n, xs)
    }
  }

  private def doParse(lines: List[Line]): Unit = {
    parseIn(Success, "<nocategory>", "<nocase>", lines)
  }

  // Construct test case, wrap in requirements and App class.
  private def mkTest(test: String): List[BatchSourceFile] = {
    val code =
      List("import de.uni_koblenz.dltypes.runtime._",
        "import de.uni_koblenz.dltypes.runtime.Sparql._",
        "class Main extends App {",
        test,
        "}")
    mkTestNormal(code.mkString("\n"))
  }

  // Construct a test case without wrapping it.
  private def mkTestNormal(test: String): List[BatchSourceFile] = {
    List(new BatchSourceFile("test", test))
  }

  // The compiler settings.
  private val settings = new Settings
  if (loglvl >= 3) {
    settings.processArgumentString("-Xprint:typer")
    settings.processArgumentString("-Xprint:superaccessors")
    settings.processArgumentString("-Xprint:jvm")
  }

  private val reporter = new ConsoleReporter(settings)

  private val compiler = new Global(settings, reporter) {
    override protected def computeInternalPhases() = {
      // Settings for tests (command line options not available,
      // since phases are added directly.
      //MyGlobal.prefixes += ":" -> "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#"
      Globals.ontology = IRI.create { "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine" }
      Globals.prefixes += ":" -> (Globals.ontology.toString + "#")
      Globals.doABoxReasoning = true
      if (debug)
        Globals.logSettings = Globals.ALL
      else
        Globals.logSettings = Globals.PRINT_CHECKS
      Globals.brokenIsError = false

      super.computeInternalPhases
      // Load all the DLTypes phases.
      for (phase <- new DLTypes(this).components)
        phasesSet += phase
    }
  }

  private val defaultSettings = new Settings
  private val defaultReporter = new ConsoleReporter(defaultSettings)
  private val defaultCompiler = new Global(defaultSettings, defaultReporter)

  // Add "scala-compiler.jar" and "scala-library.jar" to class path.
  // This is required for SBT.
  private val loader: URLClassLoader = getClass.getClassLoader.asInstanceOf[URLClassLoader]
  private val entries: Array[String] = loader.getURLs.map(_.getPath)
  private val path: Option[String] = entries.find(_.endsWith("scala-compiler.jar"))
    .map(_.replaceAll("scala-compiler.jar", "scala-library.jar"))
  settings.classpath.value = ClassPath.join(entries ++ path : _*)

  defaultSettings.classpath.value = ClassPath.join(entries ++ path : _*)

  // Use a virtual directory for compilation files (.class).
  private val virtualDir = new VirtualDirectory("(memory)", None)
  settings.outputDirs.setSingleOutput(virtualDir)
  defaultSettings.outputDirs.setSingleOutput(virtualDir)
}
