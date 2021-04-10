package com.atlassian.svn2git

import java.io.ByteArrayInputStream
import org.specs2.mutable

class AuthorsTest extends mutable.Specification {

  def parseUserXml(xml: String, expected: String*) = {
    val authors = Authors.parseUserXml(new ByteArrayInputStream(xml.getBytes))
    authors.toSet must equalTo(expected.toSet)
  }

  "parseUserXmlSingle" >> {
    parseUserXml("""<log><logentry><author>a</author></logentry></log>""", "a")
  }

  "parseUserXmlMulti" >> {
    parseUserXml("""<log><logentry><author>a</author><author>b</author></logentry></log>""", "a", "b")
  }

  "parseUserXmlMultiDupes" >> {
    parseUserXml("""<log><logentry><author>a</author><author>b</author><author>a</author></logentry></log>""", "a", "b")
  }

  "onDemandBaseUrl" >> {
    Authors.onDemandBaseUrl("https://chocs.jira-dev.com/svn/CMN/foo") must equalTo(Some("chocs.jira-dev.com"))
  }

  "not onDemandBaseUrl" >> {
    Authors.onDemandBaseUrl("https://chocs.abc.com/svn/CMN/foo") must equalTo(None)
  }

  "test mapUserDetails" >> {
    Authors.mapUserDetails(List("b", "c", "a")) {
      user => if (user != "c") Some(user.toUpperCase, "%s@%s" format (user, "example.com")) else None
    } must equalTo(List("c", "a = A <a@example.com>", "b = B <b@example.com>"))
  }

  "test username" >> {
    Authors.processUsername("zaphod") must equalTo(Some("zaphod", "zaphod@mycompany.com"))
  }

  "test email username" >> {
    Authors.processUsername("arthur.dent@example.com") must equalTo(Some("Arthur Dent", "arthur.dent@example.com"))
  }

  "test empty username" >> {
    Authors.processUsername("") must beNone
  }

  "test svn command line for http:// schema" >> {
    val url = "http://pug.jira.com/svn"
    Authors.svnCommandLineOptions(url, None) must be equalTo List("svn", "log", "--trust-server-cert", "--no-auth-cache", "--xml", "--non-interactive", "-q", url)
  }

  "test svn command line for https:// schema" >> {
    val url = "https://pug.jira.com/svn"
    Authors.svnCommandLineOptions(url, None) must be equalTo List("svn", "log", "--trust-server-cert", "--no-auth-cache", "--xml", "--non-interactive", "-q", url)
  }

  "test svn command line for file:// schema" >> {
    val url = "file:///Users/stefan/dev/svn2git/repos/a"
    Authors.svnCommandLineOptions(url, None) must be equalTo List("svn", "log", "--xml", "--non-interactive", "-q", url)
  }

  "test svn command line for svn:// schema" >> {
    val url = "svn://localhost"
    Authors.svnCommandLineOptions(url, None) must be equalTo List("svn", "log", "--xml", "--non-interactive", "-q", url)
  }

  "test svn command line for a local directory" >> {
    val url = "."
    new java.io.File(url).isDirectory must beTrue
    Authors.svnCommandLineOptions(url, None) must be equalTo List("svn", "log", "--xml", "--non-interactive", "-q", "file://" + new java.io.File(url).getCanonicalPath)
  }

  import net.liftweb.json

  "test valid parseOnDemandJson" >> {
    Authors.parseOnDemandJson(json.parse(
      """{"displayName": "a", "emailAddress": "b"}"""
    )) must equalTo(Some("a", "b"))
  }

  "test invalid parseOnDemandJson" >> {
    Authors.parseOnDemandJson(json.parse("{}")) must equalTo(None)
  }
}
