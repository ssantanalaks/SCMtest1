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

object Clean extends Command {
  case class Options(shouldCreate: Boolean, shouldDelete: Boolean, stripMetadata: Boolean)

  val name = "clean-git"
  override val usage = Some("[--force] [--no-delete] [--strip-metadata] ...")
  val help = "Cleans conversion artefacts from a converted Subversion repository."

  def parse(arguments: Array[String]) = {
    Right(arguments.partition(_ startsWith "-"))
  }

  def apply(cmd: Cmd, options: Array[String], arguments: Array[String]): Boolean = {
    import cmd._
    git.ensureRootGitDirExists()

    // get the list of branches and tags in the Subversion side
    val (branches, tags) = getSVNRoots(cmd)

    // read options
    val force = options.contains("--force")
    implicit val opts = Options(force, force && !options.contains("--no-delete"), options.contains("--strip-metadata"))
    def printDryRunWarning() {
      if (!force) {
        println(
          """###########################################################
            |#         This is a dry run, add --force to commit        #
            |#        No changes will be made to your repository       #
            |###########################################################""".stripMargin)
      }
    }

    printDryRunWarning()

    // create annotated tags for Subversion remote tags
    Tags.annotate(cmd)

    // create local branches for Subversion remote branches
    Branches.createLocal(cmd)

    // delete tags and branches removed in Subversion
    def checkObsolete(a: Array[String], f: Array[String] => Unit) = {
      if (a.find(_.contains("*")).isEmpty) {
        f(a)
      } else {
        println("WARNING: Non-standard SVN branch/tag configuration, could not clean.")
      }
    }
    checkObsolete(tags, Tags.checkObsolete(cmd))
    checkObsolete(branches, Branches.checkObsolete(cmd))

    // fix branches and tags' names to respect Git constraints
    Tags.fixNames(cmd)
    Branches.fixNames(cmd)

    // strip the git-svn metadata at the end of every commit (if --strip-metadata)
    stripMetadata(cmd)

    printDryRunWarning()

    // gc the repository and verify its size (if it's too big, warn the user)
    git.gc()
    git.warnIfLargeRepository()
  }

  def getSVNRoots(cmd: Cmd) = {
    def getConfig(s: String) = {
      val key = "svn-remote.%s.%s".format("svn", s)
      try {
        cmd.git.lines("git", "config", "--get-all", key)
      } catch {
        case ex: Exception =>
          println("Could not retrieve the config for the key: " + key)
          sys.exit(1)
      }
    }
    val url = getConfig("url").headOption.getOrElse("")
    def splitRefSpec(s: Stream[String]) = s.map(_.split("\\:")(0).replaceAll("\\*$", "")).toArray
    def getRefSpec(s: String) = splitRefSpec(getConfig(s)).map(safeURLAppend(url, _))
    (getRefSpec("branches"), getRefSpec("tags"))
  }

  def stripMetadata(cmd: Cmd)(implicit options: Clean.Options) = {
    import java.io.File
    import cmd._

    if (options.stripMetadata) {
      if ((git.dir /: Seq("info", "grafts"))(new File(_, _)).exists) {
        println("ERROR: Metadata not stripped: grafts exist.")
        true
      } else if (Option((git.dir /: Seq("refs", "replace"))(new File(_, _)).listFiles).getOrElse(Array()).nonEmpty) {
        println("ERROR: Metadata not stripped: replacement refs exist.")
        true
      } else {
        println("# removing Subversion metadata from Git commit messages")
        if (options.shouldDelete) {
          git("git", "filter-branch", "--msg-filter", "sed -e '/^git-svn-id:/d'", "--tag-name-filter", "cat", "--", "--all").!
        }
        false
      }
    } else false
  }

}
