import Dependencies.Libraries._

Global / onChangedBuildSource := ReloadOnSourceChanges

Test / fork := true
IntegrationTest / fork := true

inThisBuild(
  Seq(
    resolvers ++= Resolver.sonatypeOssRepos("releases"),
    organization := "io.funkode",
    scalaVersion := "3.2.2",
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
    "-Yretain-trees"
  ) ++ Seq("-rewrite", "-indent") ++ Seq("-source", "future-migration")

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
      coverageExcludedPackages := ".*Main.*"
    ).enablePlugins(AutomateHeaderPlugin)

addCommandAlias("ll", "projects")
addCommandAlias("checkFmtAll", ";scalafmtSbtCheck;scalafmtCheckAll")
addCommandAlias("testAll", ";compile;test;stryker")
addCommandAlias(
  "sanity",
  ";clean;coverage;compile;headerCreate;scalafixAll;scalafmtAll;test;it:test;coverageAggregate;coverageOff"
)
