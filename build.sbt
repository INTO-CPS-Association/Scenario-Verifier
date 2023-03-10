name := "scenario_verifier"

scalaVersion := "2.13.8"

// library version
version := "0.2.6"

publishTo := Some("Artifactory Realm" at "https://overture.au.dk/artifactory/into-cps")
credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

// groupId, SCM, license information
organization := "into-cps"
homepage := Some(url("https://github.com/INTO-CPS-Association/Scenario-Verifier"))
scmInfo := Some(ScmInfo(url("https://github.com/INTO-CPS-Association/Scenario-Verifier"), "git@github.com:INTO-CPS-Association/Scenario-Verifier.git"))
developers := List(Developer("SimplisticCode", "SimplisticCode", "sth@ece.au.dk", url("https://github.com/INTO-CPS-Association")))
licenses += ("INTO-CPS-LICENSE", url("https://github.com/INTO-CPS-Association/Scenario-Verifier/blob/master/LICENSE"))
publishMavenStyle := true

// From https://alvinalexander.com/scala/how-use-twirl-templates-standalone-play-framework/
lazy val root = (project in file("."))
  .settings(
    name := name.value,
    version := version.value,
    maintainer := "STH",
    scalaVersion := scalaVersion.value,
    organization := organization.value.toLowerCase(),
    homepage := homepage.value,
    scmInfo := scmInfo.value,
    Compile / scalacOptions += "-Xlint",
    Compile / scalacOptions += "-Xlint:-byname-implicit",
    Compile / scalacOptions += "-deprecation",
    Compile / console / scalacOptions --= Seq("-Ywarn-unused", "-Ywarn-unused-import"),
    Compile / javaOptions += "-Dlog4j.configurationFile=release/log4j2.xml",
    // Set display options for scalatest
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-o")
  ).enablePlugins(SbtTwirl)

val circeVersion = "0.14.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)


libraryDependencies +=
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4"

libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, major)) if major <= 12 =>
      Seq()
    case _ =>
      Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4")
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



// Allow for parsing of fmu instructions
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"

//dependencyOverrides += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.2.0"

// https://mvnrepository.com/artifact/commons-io/commons-io
libraryDependencies += "commons-io" % "commons-io" % "2.8.0"

// https://mvnrepository.com/artifact/org.jcodec/jcodec
libraryDependencies += "org.jcodec" % "jcodec" % "0.2.5"
libraryDependencies += "org.jcodec" % "jcodec-javase" % "0.2.5"

enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)

docker / dockerfile := {
  val appDir: File = stage.value
  val targetDir = "/app"

  new Dockerfile {
    from("adoptopenjdk/openjdk15:jre-15.0.2_7")
    runRaw("mkdir /tools")
    runRaw("apt-get update")
    // Install unzip
    runRaw("apt-get install -y unzip")

    //Copy the application
    copy(appDir, targetDir, chown = "daemon:daemon")

    //Unzip UPPAAL
    runRaw(s"unzip $targetDir/uppaal64-4.1.24.zip -d /tools")
    runRaw(s"rm $targetDir/uppaal64-4.1.24.zip")
    //Rename UPPAAL folder
    runRaw(s"mv /tools/uppaal64-4.1.24 /tools/uppaal")

    runRaw(s"chmod +x /tools/uppaal/bin-Linux/verifyta")
    // Add UPPAAL to path
    env("PATH", "/tools/uppaal/bin-Linux:$PATH")


    //Set the working directory
    workDir(s"$targetDir/bin/")
    // Move example files to the working directory
    runRaw(s"mv $targetDir/examples $targetDir/bin/examples")
    //entryPoint(s"$targetDir/bin/${executableScriptName.value}")
  }
}

docker / imageNames := Seq(
  // Sets the latest tag
  ImageName(s"${organization.value}/${name.value}:latest"),
  // Sets a name with a tag that contains the project version

  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some("v" + version.value)
  )

)


resolvers += "Artifactory" at "https://overture.au.dk/artifactory/into-cps/"