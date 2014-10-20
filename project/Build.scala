import sbt._
import sbt.Keys._

object Build extends sbt.Build {
  lazy val coordinateSettings = Seq(
    organization := "de.choffmeister",
    version := "0.0.1-SNAPSHOT")

  lazy val buildSettings = Seq(
    scalaVersion := "2.11.2",
    crossScalaVersions := Seq("2.10.4", "2.11.2"),
    scalacOptions ++= Seq("-encoding", "utf8"))

  lazy val publishSettings = Seq(
    publishMavenStyle := true,
    publishTo := Some(Resolver.sftp("choffmeister.de repo", "choffmeister.de", "maven2")))

  lazy val commonSettings = Defaults.defaultSettings ++ Scalariform.settings ++ Jacoco.settings ++
    coordinateSettings ++ buildSettings ++ publishSettings

  lazy val common = (project in file("auth-common"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "commons-codec" % "commons-codec" % "1.9",
      "io.spray" %% "spray-json" % "1.2.6",
      "org.specs2" %% "specs2" % "2.4.1" % "test"))
    .settings(name := "auth-common")

  lazy val spray = (project in file("auth-spray"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.2.0",
      "com.typesafe.akka" %% "akka-actor" % "2.3.6",
      "io.spray" %% "spray-routing" % "1.3.1",
      "org.specs2" %% "specs2" % "2.4.1" % "test"))
    .settings(name := "auth-spray")
    .dependsOn(common)

  lazy val root = (project in file("."))
    .settings(commonSettings: _*)
    .settings(publish := {})
    .settings(name := "auth")
    .aggregate(common, spray)
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
