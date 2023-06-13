import Dependencies.Libraries._

Global / onChangedBuildSource := ReloadOnSourceChanges

Test / fork := true
IntegrationTest / fork := true

inThisBuild(
  Seq(
    resolvers ++= Resolver.sonatypeOssRepos("releases"),
    organization := "io.funkode",
    scalaVersion := "3.3.0",
    versionScheme := Some("early-semver"),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0",
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    startYear := Some(2023),
    licenses += ("MIT", new URL("https://opensource.org/licenses/MIT")),
    homepage := Some(url("https://github.com/funkode-io/web3-auth")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/funkode-io/web3-auth"),
        "git@github.com:funkode-io/web3-auth.git"
      )
    ),
    developers := List(
      Developer(
        "carlos-verdes",
        "Carlos Verdes",
        "cverdes@gmail.com",
        url("https://github.com/carlos-verdes")
      )
    )
  )
)
ThisBuild / scalacOptions ++=
  Seq(
    "-deprecation",
    // "-explain",
    "-feature",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    //    "-Yexplicit-nulls", // experimental (I've seen it cause issues with circe)
    "-Ykind-projector",
    //    "-Ysafe-init", // experimental (I've seen it cause issues with circe)
    "-Yretain-trees",
    "-Wunused:all",
    "Wvalue-discard"
  ) ++ Seq("-rewrite", "-indent") ++ Seq("-source", "future-migration")

// assembly
ThisBuild / assemblyMergeStrategy := {
  case PathList("javax", xs @ _*) => MergeStrategy.discard
  case PathList("org", "apache", xs @ _*) => MergeStrategy.first
  case PathList("org", "newsclub", xs @ _*) => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case "reference.conf" => MergeStrategy.concat
  case "unwanted.txt" => MergeStrategy.discard
  case "logback.xml" => MergeStrategy.last
  case x if x.endsWith("module-info.class") => MergeStrategy.concat
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.last
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val testLibs = Seq(zioTest, zioTestSbt).map(_ % "it, test")

lazy val root =
  project
    .in(file("."))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings)
    .settings(headerSettings(Test, IntegrationTest))
    .settings(
      name := "web3-auth",
      libraryDependencies ++= Seq(zioResource, zioSlf4j2Log, jwtZioJson, web3j) ++ testLibs,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
      headerLicense := Some(HeaderLicense.MIT("2023", "Carlos Verdes", HeaderLicenseStyle.SpdxSyntax)),
      coverageExcludedPackages := ".*Main.*",
      assembly / mainClass := Some("io.funkode.web3.auth.Main"),
      assembly / assemblyJarName := "dfolio-auth.jar",
      assembly / assemblyMergeStrategy := (ThisBuild / assemblyMergeStrategy).value
    ).enablePlugins(AutomateHeaderPlugin, JavaAppPackaging)

addCommandAlias("ll", "projects")
addCommandAlias("checkFmtAll", ";scalafmtSbtCheck;scalafmtCheckAll")
addCommandAlias("testAll", ";compile;test;stryker")
addCommandAlias(
  "sanity",
  ";clean;coverage;compile;headerCreate;scalafixAll;scalafmtAll;test;it:test;coverageAggregate;coverageOff"
)
