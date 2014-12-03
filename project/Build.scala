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
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    pomExtra := mavenInfos)

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

  lazy val mavenInfos = {
    <url>https://github.com/choffmeister/auth-utils</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>http://opensource.org/licenses/MIT</url>
      </license>
    </licenses>
    <scm>
      <url>github.com/choffmeister/auth-utils.git</url>
      <connection>scm:git:github.com/choffmeister/auth-utils.git</connection>
      <developerConnection>scm:git:git@github.com:choffmeister/auth-utils.git</developerConnection>
    </scm>
    <developers>
      <developer>
        <id>choffmeister</id>
        <name>Christian Hoffmeister</name>
        <url>http://choffmeister.de/</url>
      </developer>
    </developers> }
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
