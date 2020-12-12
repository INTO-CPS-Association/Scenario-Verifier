name := "scenario_verifier"

version := "0.1"

scalaVersion := "2.13.3"

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

// Set display options for scalatest
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-o")
