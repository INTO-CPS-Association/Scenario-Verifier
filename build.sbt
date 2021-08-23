name := "scenario_verifier"

scalaVersion := "2.13.3"

// library version
version := "0.1.1"

publishTo := Some("Artifactory Realm" at "https://overture.au.dk/artifactory/into-cps")
credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

// groupId, SCM, license information
organization := "INTO-CPS-Association"
homepage := Some(url("https://github.com/INTO-CPS-Association/Scenario-Verifier"))
scmInfo := Some(ScmInfo(url("https://github.com/INTO-CPS-Association/Scenario-Verifier"), "git@github.com:INTO-CPS-Association/Scenario-Verifier.git"))
developers := List(Developer("SimplisticCode", "SimplisticCode", "sth@ece.au.dk", url("https://github.com/INTO-CPS-Association")))
licenses += ("INTO-CPS-LICENSE", url("https://github.com/INTO-CPS-Association/Scenario-Verifier/blob/master/LICENSE"))
publishMavenStyle := true


val circeVersion = "0.14.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)



libraryDependencies +=
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.3"

libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, major)) if major <= 12 =>
      Seq()
    case _ =>
      Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.3")
  }
}


// disable publish with scala version, otherwise artifact name will include scala version
//crossPaths := false

libraryDependencies += "org.scalatest" %% "scalatest" % "3.3.0-SNAP2" % Test

// https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.13.2"

// https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-api-scala
libraryDependencies += "org.apache.logging.log4j" %% "log4j-api-scala" % "12.0"

// https://mvnrepository.com/artifact/com.github.scopt/scopt
libraryDependencies += "com.github.scopt" %% "scopt" % "4.0.0-RC2"

libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.14.0"

// From https://alvinalexander.com/scala/how-use-twirl-templates-standalone-play-framework/
lazy val root = (project in file(".")).enablePlugins(SbtTwirl)

// Allow for parsing of fmu instructions
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"

// https://mvnrepository.com/artifact/commons-io/commons-io
libraryDependencies += "commons-io" % "commons-io" % "2.8.0"

// https://mvnrepository.com/artifact/org.jcodec/jcodec
libraryDependencies += "org.jcodec" % "jcodec" % "0.2.5"
libraryDependencies += "org.jcodec" % "jcodec-javase" % "0.2.5"

// Set display options for scalatest
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-o")

resolvers +=
  "Artifactory" at "https://overture.au.dk/artifactory/into-cps/"