package com.atlassian.svn2git

import org.specs2._
import java.io.File

class UtilsTest extends mutable.Specification {

  val dir = new File(".").getCanonicalFile

  "getRootGitDir" >> {

    "same directory" >> {
      new Git(dir).getRootGitDir() must equalTo(Some(dir))
    }
    "up one directory" >> {
      new Git(new File(dir, "src")).getRootGitDir() must equalTo(Some(dir))
    }
    "not found" >> {
      new Git(new File("/tmp")).getRootGitDir() must equalTo(None)
    }
  }

}
