val akkaVersion = "2.4.19"
val akkaHttpVersion = "10.0.8"
val specs2CoreVersion = "3.8.9"

lazy val coordinateSettings = Seq(
  organization := "de.choffmeister",
  version in ThisBuild := com.typesafe.sbt.SbtGit.GitKeys.gitDescribedVersion.value.getOrElse("0.0.0-SNAPSHOT"))

lazy val buildSettings = Seq(
  scalaVersion := "2.12.2",
  crossScalaVersions := Seq("2.12.2", "2.11.11"),
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
    "org.specs2" %% "specs2-core" % specs2CoreVersion % "test"))
  .settings(name := "auth-common")

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(packagedArtifacts := Map.empty)
  .settings(name := "auth")
  .enablePlugins(GitVersioning)
  .aggregate(common)
