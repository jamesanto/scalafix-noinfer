lazy val scalafixVersion = "0.9.15.2-SNAPSHOT"
// Use a scala version supported by scalafix.
ThisBuild / scalaVersion := "2.13.2"

ThisBuild / organization := "com.eed3si9n.fix"
ThisBuild / organizationName := "eed3si9n"
ThisBuild / organizationHomepage := Some(url("http://eed3si9n.com/"))
ThisBuild / homepage := Some(
  url("https://github.com/eed3si9n/scalafix-noinfer")
)
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/eed3si9n/scalafix-noinfer"),
    "git@github.com:eed3si9n/scalafix-noinfer.git"
  )
)
ThisBuild / developers := List(
  Developer(
    "eed3si9n",
    "Eugene Yokota",
    "@eed3si9n",
    url("https://github.com/eed3si9n")
  )
)
ThisBuild / version := "0.1.1-SNAPSHOT"
ThisBuild / description := "A Scalafix rule to suppress specific type inference."
ThisBuild / licenses := Seq(
  "Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")
)

lazy val root = (project in file("."))
  .aggregate(rules, tests)
  .settings(name := "scalafix-noinfer-root", publish / skip := true)

lazy val rules = project
  .settings(
    name := "scalafix-noinfer",
    version := "0.1.1.3-SNAPSHOT",
    libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % scalafixVersion,
    credentials += Credentials("Sonatype Nexus Repository Manager", "127.0.0.1", "admin", "admin"),
    publishTo := Some("Sonatype Nexus Repository Manager" at "http://127.0.0.1:8081/repository/maven-snapshots")
  )

lazy val input = project
  .settings(
    addCompilerPlugin(scalafixSemanticdb("4.3.10")),
    scalacOptions ++= List(
      "-Xlint",
      "-Yrangepos",
      "-Wunused",
      "-P:semanticdb:synthetics:on",
      s"-P:semanticdb:sourceroot:${baseDirectory.value}"
    ),
    publish / skip := true
  )

lazy val output = project
  .settings(publish / skip := true)

lazy val tests = project
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(input, rules)
  .settings(
    publish / skip := true,
    libraryDependencies += "ch.epfl.scala" % "scalafix-testkit" % scalafixVersion % Test cross CrossVersion.full,
    buildInfoPackage := "fix",
    buildInfoKeys := Seq[BuildInfoKey](
      "inputBaseDirectory" ->
        (input / baseDirectory).value,
      "inputSourceroot" ->
        (input / Compile / sourceDirectory).value,
      "outputSourceroot" ->
        (output / Compile / sourceDirectory).value,
      "inputClassdirectory" ->
        (input / Compile / classDirectory).value
    )
  )
