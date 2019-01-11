import sbt._
import Keys._

import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import execnpm.ExecNpmPlugin.autoImport._
import execnpm.NpmDeps._


val aceVersion = "1.4.1"
val aceDiffVersion = "2.3.0"
val bootstrapNativeVersion = "2.0.24"
val bootstrapSwitchVersion = "3.3.4"
val bootstrapVersion = "3.3.7"
val bootstrapSliderVersion = "10.4.0"
val d3Version = "4.12.0"
val highlightVersion = "9.12.0"
val querkiVersion = "0.8"
val lunrVersion = "2.1.6"
val rxVersion = "0.4.0"
val scalatagsVersion = "0.6.5"
val scalaJSdomVersion = "0.9.2"
val sortableVersion = "1.7.0"
val sourceCodeVersion = "0.1.2"

scalaVersion in ThisBuild := "2.12.4"

organization in ThisBuild := "fr.iscpif"

name := "scaladget"

publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository in ThisBuild := { _ => false }

licenses in ThisBuild := Seq("Affero GPLv3" -> url("http://www.gnu.org/licenses/"))

homepage in ThisBuild := Some(url("https://github.com/mathieuleclaire/scaladget"))

scmInfo in ThisBuild := Some(ScmInfo(url("https://github.com/mathieuleclaire/scaladget.git"), "scm:git:git@github.com:mathieuleclaire/scaladget.git"))

pomExtra in ThisBuild := {
  <developers>
    <developer>
      <id>mathieu.leclaire</id>
      <name>Mathieu Leclaire</name>
      <url>https://github.com/mathieuleclaire/</url>
    </developer>
  </developers>
}

releasePublishArtifactsAction := PgpKeys.publishSigned.value

releaseVersionBump := sbtrelease.Version.Bump.Minor

releaseTagComment := s"Releasing ${(version in ThisBuild).value}"

releaseCommitMessage := s"Bump version to ${(version in ThisBuild).value}"

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)

//useYarn in ThisBuild := true

lazy val defaultSettings = Seq(
  organization := "fr.iscpif.scaladget"
)

lazy val scalatags = libraryDependencies += "com.lihaoyi" %%% "scalatags" % scalatagsVersion
lazy val scalaJsDom = libraryDependencies += "org.scala-js" %%% "scalajs-dom" % scalaJSdomVersion
lazy val rx = libraryDependencies += "com.lihaoyi" %%% "scalarx" % rxVersion
lazy val querki = libraryDependencies += "org.querki" %%% "querki-jsext" % querkiVersion

lazy val ace = project.in(file("ace")) enablePlugins (ExecNpmPlugin) settings (defaultSettings) settings(
  scalaJsDom,
  querki,
  npmDeps in Compile += Dep("ace-builds", aceVersion, List("ace.js"))
)

lazy val aceDiff = project.in(file("acediff")) enablePlugins (ExecNpmPlugin) dependsOn(ace) settings (defaultSettings) settings(
  scalaJsDom,
  querki,
  npmDeps in Compile += Dep("ace-diff", aceDiffVersion, List("ace-diff.min.js", "ace-diff.min.css"), true)
)

lazy val bootstrapslider = project.in(file("bootstrapslider")) enablePlugins (ExecNpmPlugin) settings (defaultSettings) settings(
  scalaJsDom,
  scalatags,
  querki,
  npmDeps in Compile += Dep("bootstrap-slider", bootstrapSliderVersion, List("bootstrap-slider.min.js", "bootstrap-slider.min.css"))
)

lazy val bootstrapnative = project.in(file("bootstrapnative")) enablePlugins (ExecNpmPlugin) settings (defaultSettings) settings(
  scalaJsDom,
  scalatags,
  querki,
  libraryDependencies += "net.scalapro" %%% "sortable-js-facade" % "0.2.1",
  npmDeps in Compile += Dep("bootstrap.native", bootstrapNativeVersion, List("bootstrap-native.min.js")),
  npmDeps in Compile += Dep("bootstrap-switch", bootstrapSwitchVersion, List("bootstrap-switch.min.css")),
  npmDeps in Compile += Dep("bootstrap", bootstrapVersion, List("bootstrap.min.css")),
  npmDeps in Compile += Dep("sortablejs", sortableVersion, List("Sortable.min.js")),
) dependsOn (tools)

lazy val lunr = project.in(file("lunr")) enablePlugins (ExecNpmPlugin) settings (defaultSettings) settings (
  npmDeps in Compile += Dep("lunr", "2.1.5", List("lunr.js"))
  )

lazy val svg = project.in(file("svg")) enablePlugins (ScalaJSPlugin) settings (defaultSettings) settings(
  scalatags,
  scalaJsDom
) dependsOn (tools)


lazy val tools = project.in(file("tools")) enablePlugins (ScalaJSPlugin) settings (defaultSettings) settings(
  scalatags,
  scalaJsDom,
  rx
)
lazy val runDemo = taskKey[Unit]("runDemo")

lazy val demo = project.in(file("demo")) enablePlugins (ExecNpmPlugin) settings (defaultSettings) settings(
  libraryDependencies += "com.lihaoyi" %%% "scalarx" % rxVersion,
  libraryDependencies += "com.lihaoyi" %%% "sourcecode" % sourceCodeVersion,
  libraryDependencies += "com.github.karasiq" %%% "scalajs-marked" % "1.0.2",
  npmDeps in Compile += Dep("ace-builds",  aceVersion, List("mode-scala.js", "theme-github.js", "ext-language_tools.js"), true),
  runDemo := {

    val demoTarget = target.value
    val demoResource = (resourceDirectory in Compile).value

    IO.copyFile((fullOptJS in Compile).value.data, demoTarget / "js/demo.js")
    IO.copyFile(dependencyFile.value, demoTarget / "js/deps.js")
    IO.copyDirectory(cssFile.value, demoTarget / "css")
    IO.copyDirectory(demoResource, demoTarget)
  }
) dependsOn(bootstrapnative, bootstrapslider, lunr, tools, svg, ace, aceDiff)