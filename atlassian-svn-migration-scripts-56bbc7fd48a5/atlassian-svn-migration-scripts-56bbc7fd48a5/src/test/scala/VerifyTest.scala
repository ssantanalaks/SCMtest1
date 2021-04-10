package com.atlassian.svn2git

import org.specs2.{ ScalaCheck, mutable }
import org.scalacheck._
import org.scalacheck.Prop.forAll
import com.atlassian.svn2git.Verify.VersionComparator

class VerifyTest extends mutable.Specification with ScalaCheck {

  def lt(actual: String, required: String) = VersionComparator.lt(actual, required) must beTrue

  "lt simple" >> lt("1.2.3", "1.2.4")
  "lt ascii" >> lt("1.2.5", "1.2.40")
  "lt one major" >> lt("1.2.3", "1.3")
  "lt same major, no minor" >> lt("1.2", "1.2.4")
  "lt major/minor" >> lt("1.1", "1.2.4")
  "lt major" >> lt("1.0", "2.1")

  // Generates two lists of ints, the second always greater than the first
  def versions(max: Int) = for {
    list <- Gen.listOf1(Gen.choose(1, max))
    num <- Gen.choose(0, list.length - 1)
    list2 <- Gen.listOf(Gen.choose(1, max))
    val (h, t) = list.splitAt(num)
    greater <- Gen.choose(t.headOption.getOrElse(0) + 1, max)
  } yield (list, h ++ (greater :: list2))

  def dot(l: List[Int]) = l.mkString(".")

  "lt property check" >> {
    forAll(versions(Integer.MAX_VALUE)) {
      case (a: List[Int], b: List[Int]) => lt(dot(a), dot(b))
    }
  }

  "lt property check small" >> {
    forAll(versions(9)) {
      case (a: List[Int], b: List[Int]) => lt(dot(a), dot(b))
    }
  }
}
