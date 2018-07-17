package de.uni_koblenz.dltypes.backend

import de.uni_koblenz.dltypes.backend.Globals.LogFlag
import de.uni_koblenz.dltypes.tools.DLETruthy

import scala.tools.nsc.Global

class Logger(g: Global,
             val enabled: List[String] = Nil,
             val flags: LogFlag = Globals.logSettings) {

  import g._
  import Globals.{flag, PRINT_CHECKS, PRINT_EXTRA,
    PRINT_INFER, PRINT_SIGS,
    PRINT_SUBTYPE, PRINT_UPDATE}

  // ***** REPORTING, DEBUGGING, IO

  private val redOn = "\u001b[31m"
  private val greenOn = "\u001b[32m"
  private val yellowOn = "\u001b[33m"
  private val blueOn = "\u001b[34m"

  private val colorOff = "\u001b[0m"

  private val lineLength = 200

  private def makeLine(s: String): String =
    "-" * (lineLength - s.length)

  private def echoInLine(msg: String): Unit = {
    reporter.echo(s"$msg${makeLine(msg)}\n\n")
    reporter.flush()
  }

  private val ident = 4

  private val bar = " " * ident + "| "
  private val stick = "-" * ident + "- "
  private val tpeIdent = " " * (ident - 1) + ":: "
  private val cross = "-" * ident + "+ "
  private val skip = " " * ident + "  "
  private val note = blueOn + ">" * ident + "> " + colorOff

  private def isEnabled(thing: String) =
    enabled.isEmpty || enabled.contains(thing)

  // Format and echo a message for a DLETruthy check.
  def echo(t: DLETruthy): Unit = {
    val uri = t.pretty(Globals.prefixes.get(":"))
    if (flag(PRINT_CHECKS))
      reporter.echo(s"$greenOn$stick[CHECK] $uri$colorOff")
  }

  // Report error or warning at the given Position.
  def report(pos: Position, e: DLTypesError): Unit = e match {
    case w @ Warning(_) => reporter.warning(pos, w.show)
    case e => reporter.error(pos, e.show)
  }

  // Log message at desired log level (default: PRINT_EXTRA),
  // proceeded by special mark.
  def log(thing: String, mark: String = "", logLevel: LogFlag = PRINT_EXTRA): Unit =
    if (flag(logLevel)) {
      if (mark != "")
        reporter.echo(s"$note[$mark] $thing")
      else
        reporter.echo(s"$note$thing")
    }

  // Display subtyping of 'left' and 'right'.
  def subtype(left: String, right: String): Unit = {
    if (flag(PRINT_SUBTYPE))
      reporter.echo(s"$skip[<:<] $right  [subtype of]  $left")
  }

  // Display inference between 'left' and 'right'.
  def infer(left: String, right: String): Unit = {
    if (flag(PRINT_INFER))
      reporter.echo(s"$skip[<<<] $left  [inference from]  $right")
  }

  // Display update from 'from' to 'to'
  def update(from: String, to: String): Unit = {
    if (flag(PRINT_UPDATE))
      reporter.echo(s"$skip[-->] $from  [update to]  $to")
  }

  // Format a 'done' statement for tree, if the particular
  // case is enabled in this reporter instance.
  def done(thing: String, name: String): Unit =
    if (flag(PRINT_SIGS) && isEnabled(thing))
      echoInLine(s"$stick[$greenOn$thing$colorOff] $name ")

  // Print signature (of tree node) if signature printing is enabled.
  def sig(pre: String, lines: List[String] = Nil, post: String = "", tpe: String = ""): Unit = {
    if (flag(PRINT_SIGS)) {
      if (lines.isEmpty)
        reporter.echo(s"$stick$pre$post")
      else {
        reporter.echo(s"$cross$pre")
        lines.foreach { l =>
          reporter.echo(s"$bar$skip$l")
        }
        reporter.echo(s"$bar$post")
      }
      if (tpe != "")
        reporter.echo(s"$tpeIdent$tpe")
    }
  }
}
