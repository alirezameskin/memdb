name := "memdb"

version := "0.1"

scalaVersion := "2.13.3"
libraryDependencies ++= Seq("com.chuusai" %% "shapeless" % "2.3.3")
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
libraryDependencies += "org.typelevel" %% "cats-effect" % "2.1.3"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full
)

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "8.0.15",
  "io.getquill" %% "quill-jdbc" % "3.4.2-SNAPSHOT"
)
