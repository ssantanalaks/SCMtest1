package com.atlassian.svn2git

import java.io.File

class MockCmd(dir: File, override val svn: Svn = new MockSvn) extends Cmd(dir) {

  val list = collection.mutable.ListBuffer[String]()

  override def println(a: Any) = list += a.toString

  override def printerr(a: Any) = list += a.toString

}

class MockSvn(fi: Array[String] => Array[String] = identity) extends Svn {

  override def findItems(svnUrls: Array[String]) = fi(svnUrls)

}
