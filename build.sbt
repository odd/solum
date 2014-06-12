import AssemblyKeys._

import sbtassembly.Plugin._

import DockerKeys._

import sbtdocker._

name := "solum"

organization := "org.bitbonanza"

version := "0.1"

scalaVersion := "2.11.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

unmanagedResourceDirectories in Compile <++= baseDirectory { base =>
    Seq(base / "src/main/web")
}

resolvers += "Spray Nightlies" at "http://nightlies.spray.io"

resolvers += "Spray" at "http://repo.spray.io"

val AkkaVersion = "2.3.3"

val SprayVersion = "1.3.1-20140423"

libraryDependencies ++= Seq(
    "junit" % "junit" % "4.11" % "test",
    "org.scalatest" %% "scalatest" % "2.2.0-M1" % "test",
    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test",
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    "com.typesafe.akka" %% "akka-persistence-experimental" % AkkaVersion,
    "com.typesafe.akka" %% "akka-remote" % AkkaVersion,
    "com.typesafe.akka" %% "akka-kernel" % AkkaVersion,
    "com.typesafe.akka" %% "akka-camel" % AkkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
    "io.spray" %% "spray-routing" % SprayVersion,
    "io.spray" %% "spray-can" % SprayVersion,
    "io.spray" %% "spray-client" % SprayVersion,
    "io.spray" %% "spray-util" % SprayVersion,
    "io.spray" %% "spray-caching" % SprayVersion,
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.github.mauricio" % "mysql-async_2.10" % "0.2.12"
    //"com.amazonaws" % "aws-java-sdk" % "1.7.9"
)

assemblySettings

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList(ps @ _*) if ps.last == "pom.xml" => MergeStrategy.discard
    case PathList(ps @ _*) if ps.last == "pom.properties" => MergeStrategy.discard
    case x => old(x)
  }
}

dockerSettings

docker <<= docker.dependsOn(assembly)

imageName in docker := {
  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some("v" + version.value))
}

dockerfile in docker := {
  val artifact = (outputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("dockerfile/java")
    maintainer("Odd Möller", "odd.moller@gmail.com")
    add(artifact, artifactTargetPath)
    expose(8080, 3306)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

