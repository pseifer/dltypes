import java.net.URLClassLoader

import scala.io.Source
import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.util.ClassPath
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.io.VirtualDirectory
import de.uni_koblenz.dltypes.DLTypes

import scala.collection.mutable.ListBuffer


object CompilationError extends Exception


object TestTools {
  var LOGLVL = 0
  private val testsep = "\n\n--------------------------------------------------------------------------------"

  // Internal storage for test cases.
  private var cases = ListBuffer[TestCase]()

  // Possible results for tests.
  sealed trait TestResult
  object Failure extends TestResult
  object Success extends TestResult

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

  // The compiler settings.
  private val settings = new Settings

  // Add "scala-compiler.jar" and "scala-library.jar" to class path.
  // This is required for SBT.
  private val loader: URLClassLoader = getClass.getClassLoader.asInstanceOf[URLClassLoader]
  private val entries: Array[String] = loader.getURLs.map(_.getPath)
  private val path: Option[String] = entries.find(_.endsWith("scala-compiler.jar"))
    .map(_.replaceAll("scala-compiler.jar", "scala-library.jar"))
  settings.classpath.value = ClassPath.join(entries ++ path : _*)

  // Use a virtual directory for compilation files (.class).
  private val virtualDir = new VirtualDirectory("(memory)", None)
  settings.outputDirs.setSingleOutput(virtualDir)

  // Construct test case, wrap in requirements and App class.
  private def mkTest(test: String): List[BatchSourceFile] = {
    val code =
      List("import de.uni_koblenz.dltypes.runtime._",
        "import de.uni_koblenz.dltypes.runtime.Sparql._",
        "class Main extends App {",
        test,
        "}")
    List(new BatchSourceFile("test", code.mkString("\n")))
  }

  // Run compiler on a single test case.
  def testCase(test: TestCase): Unit = test match {
    case TestCase(_, n, c, code) =>
      testCase(n, code)
  }

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

  def tests(): List[TestCase] = cases.toList

  def filterCategory(cat: String): this.type = {
    cases = cases.filter( x => x match {
      case TestCase(_, _, c, _) => c == cat
    })
    this
  }

  def filterName(name: String): this.type = {
    cases = cases.filter( x => x match {
      case TestCase(_, name1, _, _) => name == name1
    })
    this
  }

  def onlyFor(r: TestResult, f: (String, TestCase) => Unit): this.type = {
    cases.foreach { x => x match {
      case TestCase(`r`, name, _, _) => f(name, x)
      case _ => Unit
    }}
    this
  }
}
