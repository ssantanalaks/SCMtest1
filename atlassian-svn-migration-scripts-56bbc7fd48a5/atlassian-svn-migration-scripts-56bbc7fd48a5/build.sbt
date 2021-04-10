name := "svn-migration-scripts"

version := "0.1"

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.7" % "test",
  "org.specs2" %% "specs2" % "1.9" % "test",
  "org.scalacheck" %% "scalacheck" % "1.10.0" % "test",
  "net.databinder" %% "dispatch-http" % "0.8.8",
  "commons-io" % "commons-io" % "2.3",
  "org.scalesxml" %% "scales-xml" % "0.3.1"
)

fork in run := true // We use sys.exit

mainClass in (Compile, run) := Some("com.atlassian.svn2git.Main")

mainClass in (Compile, packageBin) <<= mainClass in (Compile, run)

testOptions += Tests.Argument(TestFrameworks.Specs2, "console", "junitxml")

makeInJarFilter <<= (makeInJarFilter) {
  (makeInJarFilter) => {
    (file) => file match {
      case "httpcore-4.1.4.jar" => makeInJarFilter(file) + ",!META-INF/NOTICE*,!META-INF/LICENSE*"
      case "httpclient-4.1.3.jar" => makeInJarFilter(file) + ",!META-INF/NOTICE*,!META-INF/LICENSE*"
      case "commons-io-2.3.jar" => makeInJarFilter(file) + ",!META-INF/NOTICE*,!META-INF/LICENSE*"
      case _ => makeInJarFilter(file)
    }
  }
}

minJarPath <<= artifact { a => file("target") / (a.name + "." + a.extension) asFile }

packageOptions in (Compile, packageBin) <+= version.map { v =>  Package.ManifestAttributes( "Implementation-Version" -> (v + "." + ("git describe --always --tags"!!).trim )) }

net.virtualvoid.sbt.graph.Plugin.graphSettings

seq(ScctPlugin.instrumentSettings : _*)

scalariformSettings
