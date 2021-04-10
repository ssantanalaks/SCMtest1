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

import java.io.{ File, IOException }
import sys.process._
import org.apache.commons.io.FileUtils

object Verify extends Command {
  val name = "verify"
  val help = "Verifies that dependencies are present and the environment is suitable."

  object VersionComparator extends Ordering[String] {
    def atoi(s: String) = {
      var i = 0
      while (i < s.length && Character.isDigit(s(i))) i = i + 1
      try {
        s.substring(0, i).toInt
      } catch {
        case e: NumberFormatException => 0
      }
    }

    override def compare(l: String, r: String): Int = {
      val lefts = l.split("[-._]")
      val rights = r.split("[-._]")
      var i = 0
      var v = 0
      while (i < math.min(lefts.length, rights.length) && v == 0) {
        v = atoi(lefts(i)).compareTo(atoi(rights(i)))
        i = i + 1
      }
      if (v != 0) v else lefts.length.compareTo(rights.length)
    }
  }

  def findVersion(cwd: File, s: String*): Either[String, String] =
    (try {
      Process(s :+ "--version", cwd).lines.headOption
    } catch { case e: Exception => None })
      .flatMap("(?<=version )[^ ]+".r findFirstIn _).toRight("Unable to determine version.")

  def requireVersion(actual: String, required: String): Either[String, String] =
    if (VersionComparator.lt(actual, required)) Left("Version %s required (found %s).".format(required, actual)) else Right(actual)

  case class Dependency(name: String, required: String, invocation: String*)

  def parse(arguments: Array[String]) = Right(Array(), Array())

  def checkCaseSensitivity: Boolean =
    try {
      val cwd = new File(".")
      val tempFile = File.createTempFile("svn-migration-scripts", ".tmp", cwd)
      tempFile.deleteOnExit()
      if (new File(cwd, tempFile.getName.toUpperCase).exists)
        println("You appear to be running on a case-insensitive file-system. This is unsupported, and can result in data loss.")
      false
    } catch {
      case e: IOException =>
        println("Unable to determine whether the file-system is case-insensitive. Case-insensitive file-systems are unsupported, and can result in data loss.")
        true
    }

  def checkHttpConnectivity: Boolean = {
    import java.net.{ InetSocketAddress, Proxy, Socket }
    import java.util.concurrent.TimeUnit._

    val socket = new Socket(Proxy.NO_PROXY)
    try {
      socket.connect(new InetSocketAddress("atlassian.com", 80), MILLISECONDS.convert(30, SECONDS).asInstanceOf[Int])
      socket.close()
      false
    } catch {
      case ex: Exception =>
        println("Cannot connect directly to internet. This may interfere with your ability to clone Subversion repositories and push Git repositories.")
        true
    }
  }

  def withTempGitDir[T](callback: File => T) = {
    val dir = new File(System.getProperty("java.io.tmpdir"), java.util.UUID.randomUUID().toString)
    Process("git init -q " + dir.getCanonicalPath).!
    try {
      callback(dir)
    } finally {
      if (!FileUtils.deleteQuietly(dir)) {
        FileUtils.forceDeleteOnExit(dir)
      }
    }
  }

  def apply(cmd: Cmd, options: Array[String], arguments: Array[String]) = {
    val versionString = "%s: using version %s"
    cmd.println(versionString format ("svn-migration-scripts", Version.version))
    def verify(dir: File, deps: Dependency*) = {
      deps.map(command => findVersion(dir, command.invocation: _*).right.flatMap(requireVersion(_, command.required)).fold(
        error => {
          cmd.println("%s: ERROR: %s".format(command.name, error))
          true
        },
        version => {
          cmd.println(versionString.format(command.name, version))
          false
        }
      )).contains(true)
    }

    val gitErrors = verify(new File("."), Dependency("Git", "1.7.7.5", "git"))
    val svnErrors = verify(new File("."), Dependency("Subversion", "1.6.17", "svn"))
    val gitSvnErrors = !gitErrors &&
      withTempGitDir {
        dir =>
          verify(dir, Dependency("git-svn", "1.7.7.5", "git", "svn"))
      }

    gitErrors || svnErrors || gitSvnErrors || checkCaseSensitivity || checkHttpConnectivity
  }
}
