package com.atlassian.svn2git

import org.specs2.mutable
import org.apache.commons.io.FileUtils
import java.io.File

class TagsTest extends mutable.Specification {

  "test findOldestAncestor" >> {
    Verify.withTempGitDir {
      dir =>
        val git = new Git(dir)
        FileUtils.write(new File(dir, "test.txt"), "a")
        git("git", "add", "test.txt").!
        git("git", "commit", "-a", "-m", "Initial commit").!
        FileUtils.write(new File(dir, "test.txt"), "b")
        git("git", "add", "test.txt").!
        git("git", "commit", "-m", "First commit").!
        git("git", "commit", "--allow-empty", "-m", "Second commit").!
        git("git", "commit", "--allow-empty", "-m", "Third commit").!
        val x = git.$("git", "rev-parse", "HEAD:")
        Tags.findOldestAncestor(git, x, "HEAD") must equalTo("HEAD^^")
    }
  }

}
