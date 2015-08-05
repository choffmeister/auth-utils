import sbt._
import sbt.Keys._
import com.typesafe.sbt._
import com.typesafe.sbt.SbtGit.GitKeys._

object Build extends sbt.Build {
  lazy val coordinateSettings = Seq(
    organization := "de.choffmeister",
    version in ThisBuild := gitDescribedVersion.value.map(_.drop(1)).get)

  lazy val buildSettings = Seq(
    scalaVersion := "2.11.5",
    scalacOptions ++= Seq("-encoding", "utf8"))

  lazy val resolverSettings = Seq(
    resolvers ++= Seq(
      "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"))

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

  lazy val commonSettings = Defaults.coreDefaultSettings ++ coordinateSettings ++ buildSettings ++
    resolverSettings ++ publishSettings

  lazy val common = (project in file("auth-common"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "commons-codec" % "commons-codec" % "1.9",
      "io.spray" %% "spray-json" % "1.3.1",
      "org.specs2" %% "specs2-core" % "3.3.1" % "test"))
    .settings(name := "auth-common")

lazy val akkaHttp = (project in file("auth-akka-http"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.2.0",
    "com.typesafe.akka" %% "akka-actor" % "2.3.12",
    "com.typesafe.akka" %% "akka-http-experimental" % "1.0",
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % "1.0" % "test",
    "org.specs2" %% "specs2-core" % "3.3.1" % "test"))
  .settings(name := "auth-akka-http")
  .dependsOn(common)

  lazy val spray = (project in file("auth-spray"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.2.0",
      "com.typesafe.akka" %% "akka-actor" % "2.3.12",
      "io.spray" %% "spray-routing" % "1.3.1",
      "org.specs2" %% "specs2-core" % "3.3.1" % "test"))
    .settings(name := "auth-spray")
    .dependsOn(common)

  lazy val root = (project in file("."))
    .settings(commonSettings: _*)
    .settings(publish := {})
    .settings(name := "auth")
    .enablePlugins(GitVersioning)
    .aggregate(common, akkaHttp, spray)

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
