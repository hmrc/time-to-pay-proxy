import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "time-to-pay-proxy"

val silencerVersion = "1.7.3"
lazy val ItTest = config("it") extend Test
coverageExcludedPackages := "<empty>;Reverse.*;.*Module.*;.*AuthService.*;models\\.data\\..*;uk.gov.hmrc.BuildInfo;app.*;nr.*;res.*;prod.*;.*RuleAST.*;config.*;testOnlyDoNotUseInAppConf.*;definition.*"
lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(
    majorVersion                     := 0,
    scalaVersion                     := "2.12.13",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    dependencyOverrides              ++= AppDependencies.overrides,
    scalacOptions ++= Seq(
      "-P:silencer:pathFilters=routes",
      "-Ypartial-unification",
      "-Ywarn-dead-code",
      "-Xfatal-warnings",
      "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals", // Warn if a local definition is unused.
      "-Ywarn-unused:params", // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates" // Warn if a private member is unused.
    ),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
    // ***************
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .configs(ItTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources"
  )