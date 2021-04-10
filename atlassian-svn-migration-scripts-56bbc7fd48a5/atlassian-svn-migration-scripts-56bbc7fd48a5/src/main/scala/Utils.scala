/**
 * Copyright 2012 Atlassian
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.atlassian.svn2git

import java.io._
import java.net.URLDecoder
import sys.process._
import org.apache.commons.io.{ FileUtils, IOUtils }
import scala.Console

object `package` {
  def safeURLAppend(a: String, b: String) = a.stripSuffix("/") + "/" + b.stripPrefix("/")

  def returning[T](t: T)(f: T => Unit) = {
    f(t); t
  }

  def writeExceptionToDisk(t: Throwable) {
    val f = File.createTempFile("error-script-", ".txt")
    val s = new PrintWriter(new BufferedWriter(new FileWriter(f)))
    try {
      t.printStackTrace(s)
      println("Error written to " + f.getAbsolutePath)
    } catch {
      case e: IOException =>
        System.err.println("Could not write the error to a temp file. Here is the complete stacktrace:")
        e.printStackTrace(System.err)
        f.deleteOnExit()
    } finally {
      IOUtils.closeQuietly(s)
    }
  }

  def readPassword() = Option(System.console())
    .map(console => new String(console.readPassword("Password: ")))
    .getOrElse(readLine())
}

class Svn(logger: Logger = new NoopLogger) {

  def findItems(svnUrls: Array[String]): Array[String] = {
    val strippedUrls = svnUrls.map(_ stripSuffix "/")
    val allBranchUrls = strippedUrls.flatMap {
      url =>
        val cmd = Seq("svn", "ls", url)
        logger.logWith(cmd.mkString(" "))(cmd.lines_!.map(logger.logReturn).map(url + "/" + _.stripSuffix("/")))
    }
    (allBranchUrls diff strippedUrls).map(new File(_).getName)
  }

}

class Git(cwd: File, logger: Logger = new NoopLogger) {
  import logger._

  def isIntermediateRef(ref: String) = {
    "^.+@\\d+$".r.findAllIn(ref).hasNext
  }

  def decodeRef(ref: String) = {
    URLDecoder.decode(ref, "UTF-8")
  }

  def cleanRef(ref: String) = {
    // reference: http://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html
    decodeRef(ref).filterNot(Character.isISOControl _)
      .trim()
      .stripSuffix("?")
      .stripSuffix(".lock")
      .replaceAll("\\.+$", "")
      .replaceAll("\\.{2,}", ".")
      .replaceAll("""(?:\s+|\@\{|[~^:*?/]+|\[+|\]+)""", "-")
  }

  def dir: File = new File(sys.env.getOrElse("GIT_DIR", ".git"))

  def forEachRefFull(pattern: String) = lines("git", "for-each-ref", pattern, "--format=%(refname)")

  def forEachRef(pattern: String) = forEachRefFull(pattern).map(_ stripPrefix pattern)

  def gc() = this("git", "gc", "--prune=now").lines_!

  def getRootGitDir() = {
    try {
      Some(new File($("git", "rev-parse", "--show-toplevel")))
    } catch {
      case _ => None
    }
  }

  def ensureRootGitDirExists() = {
    if (getRootGitDir().isEmpty) {
      System.err.println("Command must be run from within a Git repository")
      sys.exit(1)
    }
  }

  def warnIfLargeRepository(f: Unit => Unit = _ => ()) = {
    val size = FileUtils.sizeOfDirectory(dir)
    log("Repository size: " + size)
    if (size > (1 << 30)) {
      println()
      println("### Warning: your repository is larger than 1GB (" + size / (1 << 20) + " Mb)")
      println("### See http://go-dvcs.atlassian.com/x/GQAW on how to reduce the size of your repository.")
      println()
      f()
      false
    } else true
  }

  def lines(s: String*): Stream[String] = this(s: _*).lines.map(logReturn)

  def $(s: String*): String = logReturn(this(s: _*).!!.stripLineEnd)

  def apply(cmd: String*) = logWith(cmd.mkString(" "))(Process(cmd, cwd))

  def apply(cmd: Seq[String], extraEnv: (String, String)*) = logWith(cmd.mkString(" "))(Process(cmd, cwd, extraEnv: _*))

}

class Cmd(cwd: File = new File("."), logger: Logger = new NoopLogger) {
  val git = new Git(cwd, logger)
  val svn: Svn = new Svn(logger)
  def println(s: Any) = {
    logger.log(s.toString)
    Console.println(s)
  }
  def printerr(s: Any) = {
    logger.log(s.toString)
    Console.err.println(s)
  }
}

trait Logger {
  def log(s: String)

  def logReturn[A](s: String): String = logWith(s)(s)

  def logWith[A](s: String)(f: => A): A = {
    log(s)
    f
  }
}

class NoopLogger extends Logger {

  def log(s: String) {}
}

class PrintLogger(val out: PrintWriter) extends Logger {

  def log(s: String) {
    out.println(s)
  }
}

object Version {
  val version = Version.getClass.getPackage.getImplementationVersion
}
