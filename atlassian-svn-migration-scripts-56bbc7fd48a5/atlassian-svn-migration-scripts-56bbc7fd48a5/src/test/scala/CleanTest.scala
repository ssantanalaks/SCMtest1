package com.atlassian.svn2git

import org.specs2.mutable
import org.apache.commons.io.FileUtils
import java.io.File

class CleanTest extends mutable.Specification {

  def writeSVNRemoteConfig(dir: File, extra: String = "") = {
    FileUtils.writeStringToFile(new File(dir, ".git/config"),
      """
        |[svn-remote "svn"]
        |	url = https://a/b
        |	fetch = trunk:refs/remotes/trunk
        |	branches = branches/*:refs/remotes/*
        |	tags = tags/*:refs/remotes/tags/*
      """.stripMargin + "\n" + extra)
  }

  "test getSVNRoot" >> {
    Verify.withTempGitDir {
      dir =>
        writeSVNRemoteConfig(dir, "branches = branches2/*/3:refs/remotes/*")
        val (branches, tags) = Clean.getSVNRoots(new Cmd(dir))
        branches must equalTo(Array("https://a/b/branches/", "https://a/b/branches2/*/3"))
        tags must equalTo(Array("https://a/b/tags/"))
    }
  }

  "Clean Integration test empty" >> {
    Verify.withTempGitDir {
      dir =>
        writeSVNRemoteConfig(dir)
        val cmd = new MockCmd(dir, new MockSvn(urls => Array("trunk", "newbranch")))
        cmd.git("git", "commit", "--allow-empty", "-m", "Initial commit").!
        Clean(cmd, Array("--force"), Array())
        cmd.list.mkString("\n") must equalTo(
          """# Creating annotated tags...
            |# Creating local branches...
            |# Checking for obsolete tags...
            |No obsolete tags to remove.
            |# Checking for obsolete branches...
            |No obsolete branches to remove.
            |# Cleaning tag names
            |# Cleaning branch names""".stripMargin)
    }
  }

  "Clean Integration test everything" >> {
    Verify.withTempGitDir {
      dir =>
        writeSVNRemoteConfig(dir)
        val cmd = new MockCmd(dir, new MockSvn(urls =>
          if (urls(0).contains("branches")) Array("trunk", "newbranch", "space branch") else Array("newtag", "space tag")
        ))
        cmd.git("git", "commit", "--allow-empty", "-m", "Initial commit").!
        cmd.git("git", "commit", "--allow-empty", "-m", "First commit").!
        cmd.git("git", "update-ref", "refs/remotes/trunk", "master^").!
        cmd.git("git", "update-ref", "refs/remotes/tags/newtag", "master").!
        cmd.git("git", "update-ref", "refs/remotes/newbranch", "master").!
        cmd.git("git", "update-ref", "refs/remotes/newbranch2", "master").!
        cmd.git("git", "branch", "oldbranch").!
        cmd.git("git", "branch", "space%20branch").!
        cmd.git("git", "tag", "oldtag").!
        cmd.git("git", "tag", "space%20tag").!
        Clean(cmd, Array("--force"), Array())
        cmd.list.mkString("\n") must equalTo(
          """# Creating annotated tags...
            |tag has diverged: newtag
            |Creating annotated tag 'newtag' at refs/remotes/tags/newtag.
            |# Creating local branches...
            |Creating the local branch 'newbranch' for Subversion branch 'refs/remotes/newbranch'.
            |Creating the local branch 'newbranch2' for Subversion branch 'refs/remotes/newbranch2'.
            |# Checking for obsolete tags...
            |Deleting Git tag 'oldtag' not in Subversion.
            |# Checking for obsolete branches...
            |Deleting Git branch 'newbranch2' not in Subversion.
            |Deleting Git branch 'oldbranch' not in Subversion.
            |# Cleaning tag names
            |Replacing tag 'space%20tag' with 'space-tag' at space%20tag^{commit}.
            |# Cleaning branch names
            |Replacing branch 'space%20branch' with 'space-branch'.""".stripMargin)
        cmd.git.$("git", "tag") must equalTo("newtag\nspace-tag")
        cmd.git.$("git", "branch") must equalTo("* master\n  newbranch\n  space-branch")
    }
  }
}
