name := "memdb"

version := "0.1-SNAPSHOT"

scalaVersion := "2.13.3"
organization := "com.github.alirezameskin"

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
libraryDependencies += "org.typelevel"  %% "cats-effect"  % "2.1.3"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

scalacOptions ++= Seq("-deprecation", "-feature")
scalacOptions += "-Ymacro-annotations"
