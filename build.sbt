
name := "thesis-adaptive-traffic-light-management-system"

version := "0.1.0-SNAPSHOT"

resolvers += Resolver.mavenLocal

scalaVersion := "2.12.8"

val akkaVersion = "2.5.22"
val akkaHttpVersion = "10.1.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.102",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
).map(_.withSources())

Revolver.settings

mainClass in (Compile, run) := Some("trafficlightscontrol.system.Application")

lazy val root = (project in file("."))

fork := true

connectInput in run := true

outputStrategy := Some(StdoutOutput)

scalafmtOnCompile in Compile := true
scalafmtOnCompile in Test := true
