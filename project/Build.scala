import bintray.BintrayKeys._
import sbt._
import sbt.Keys._
import com.typesafe.sbt._
import com.typesafe.sbt.SbtGit.GitKeys._

object Build extends sbt.Build {
  val akkaVersion = "2.4.2"

  lazy val coordinateSettings = Seq(
    organization := "de.choffmeister",
    version in ThisBuild := gitDescribedVersion.value.map(_.drop(1)).get)

  lazy val buildSettings = Seq(
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-encoding", "utf8"))

  lazy val resolverSettings = Seq(
    resolvers ++= Seq(
      "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"))

  lazy val publishSettings = Seq(
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    bintrayReleaseOnPublish in ThisBuild := false
  )

  lazy val commonSettings = Defaults.coreDefaultSettings ++ coordinateSettings ++ buildSettings ++
    resolverSettings ++ publishSettings

  lazy val common = (project in file("auth-common"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "commons-codec" % "commons-codec" % "1.10",
      "io.spray" %% "spray-json" % "1.3.2",
      "org.specs2" %% "specs2-core" % "3.7.1" % "test"))
    .settings(name := "auth-common")

  lazy val akkaHttp = (project in file("auth-akka-http"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.3.0",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaVersion % "test",
      "org.specs2" %% "specs2-core" % "3.7.1" % "test"))
    .settings(name := "auth-akka-http")
    .dependsOn(common)

  lazy val root = (project in file("."))
    .settings(commonSettings: _*)
    .settings(packagedArtifacts := Map.empty)
    .settings(name := "auth")
    .enablePlugins(GitVersioning)
    .aggregate(common, akkaHttp)
}
