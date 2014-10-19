import sbt._
import sbt.Keys._

object Build extends sbt.Build {
  lazy val dependencies = Seq(
    "commons-codec" % "commons-codec" % "1.9",
    "io.spray" %% "spray-json" % "1.2.6")

  lazy val testDependencies = Seq(
    "org.specs2" %% "specs2" % "2.4.1")

  lazy val buildSettings = Seq(
    scalaVersion := "2.11.2",
    crossScalaVersions := Seq("2.11.2"),
    scalacOptions ++= Seq("-encoding", "utf8"))

  lazy val publishSettings = Seq(
    publishMavenStyle := true,
    publishTo := Some(Resolver.sftp("choffmeister.de repo", "choffmeister.de", "maven2")))

  lazy val root = (project in file("."))
    .settings(Defaults.defaultSettings: _*)
    .settings(Scalariform.settings: _*)
    .settings(Jacoco.settings: _*)
    .settings(buildSettings: _*)
    .settings(publishSettings: _*)
    .settings(libraryDependencies ++= dependencies)
    .settings(libraryDependencies ++= testDependencies.map(_ % "test"))
    .settings(
      name := "auth-utils",
      organization := "de.choffmeister",
      version := "0.0.1-SNAPSHOT")
}

object Jacoco {
  import de.johoop.jacoco4sbt._
  import JacocoPlugin._

  lazy val settings = jacoco.settings ++ reports

  lazy val reports = Seq(
    jacoco.reportFormats in jacoco.Config := Seq(
      XMLReport(encoding = "utf-8"),
      ScalaHTMLReport(withBranchCoverage = true)))
}

object Scalariform {
  import com.typesafe.sbt._
  import com.typesafe.sbt.SbtScalariform._
  import scalariform.formatter.preferences._

  lazy val settings = SbtScalariform.scalariformSettings ++ preferences

  lazy val preferences = Seq(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(SpacesWithinPatternBinders, true)
      .setPreference(CompactControlReadability, false))
}
