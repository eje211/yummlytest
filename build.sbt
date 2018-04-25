name := "yummly-test"

version := "1.0"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0"
