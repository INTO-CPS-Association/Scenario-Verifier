# Scenario-Verifier

This repository contains the verifier used in the paper: "Verification of Co-Simulation Algorithms". The Verifier lets the user specify a scenario in an easy and readable way. The tool can parse the scenario and algorithm and runs in against the Uppaal model from the previously mentioned paper.

## Examples

See script `release.ps1` where examples are deployed and run.

## Setup Development Environment

This is a simple [sbt](https://www.scala-sbt.org/) project.

See  [build.properties](project\build.properties)  and  [build.sbt](build.sbt)  for sbt and scala versions.

Sbt requires Java version 11 or greater.

In order to successfully run the tests the `verifyTA` executable of UPPAAL (we have tested using v. 4.1.24) should be added in the system environment.

The following will produce the jar of the app.

```bash
sbt assembly
```

The script `release.ps1` shows the steps to produce a release version.

## Running tests

All tests are run when the `sbt assembly` is invoked.
To run a specific test, use the following on sbt interpreter:

```bash
testOnly PositiveTests -- -z "work for simple_master"
```

where `PositiveTests` denotes the suite, and `work for simple_master` is the text identifying the specific test function to call.

## Running the app with logging

App uses log4j2 for logging.
To run it, set the location of the xml configuration of the logging:

```bash
java -D"log4j.configurationFile=.\log4j2.xml" -jar .\scenario_verifier-assembly-0.1.jar
```

We have an example of `log4j2.xml` in the source.