name := "recruitment-sensor-aggregator"

version := "0.1"

scalaVersion := "2.13.3"

val logbackVersion = "1.2.3"
val scalaLoggingVersion = "3.9.2"
val akkaVersion = "2.6.8"
val shapelessVersion = "2.3.3"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0" % "test"
libraryDependencies += "ch.qos.logback" % "logback-classic" % logbackVersion
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.chuusai" %% "shapeless" % shapelessVersion