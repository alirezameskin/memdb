import sbt.Keys.{libraryDependencies, scalacOptions}

lazy val scala213          = "2.13.3"
lazy val catsEffectVersion = "2.1.3"
lazy val scalaTestVersion  = "3.1.1"

Global / scalaVersion := scala213
Global / organization := "com.github.alirezameskin"

lazy val core = project
  .in(file("modules/core"))
  .settings(
    name := "memdb",
    description := "In-memory database",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scala213,
      "org.typelevel"  %% "cats-effect"  % catsEffectVersion,
      "org.scalatest"  %% "scalatest"    % scalaTestVersion % Test
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-Ymacro-annotations"
    ),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
    )
  )

lazy val examples = project
  .in(file("modules/examples"))
  .settings(
    name := "memdb-examples",
    libraryDependencies ++= Seq(
      "org.typelevel"            %% "cats-effect" % catsEffectVersion,
      "com.github.alirezameskin" %% "memdb"       % version.value
    ),
    scalacOptions += "-Ymacro-annotations",
    skip in publish := true
  )
  .dependsOn(core)

lazy val root = project
  .in(file("."))
  .settings(
    skip in publish := true
  )
  .aggregate(core, examples)
