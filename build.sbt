name := "concurrent-batch-framework"

version := "1.0-SNAPSHOT"

organization := "be.objectify"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
    "com.typesafe.akka" % "akka-actor_2.10" % "2.3.1",
    "com.typesafe.akka" % "akka-testkit_2.10" % "2.3.1" % "test",
    "org.specs2" % "specs2_2.10" % "2.3.10" % "test",
    "org.scalatest" % "scalatest_2.10" % "2.1.2" % "test"
)
