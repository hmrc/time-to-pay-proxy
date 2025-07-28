import uk.gov.hmrc.DefaultBuildSettings
import scoverage.ScoverageKeys

val appName = "time-to-pay-proxy"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.16"

val silencerVersion = "1.7.3"
lazy val ItTest = config("it") extend Test
lazy val coverageSettings: Seq[Setting[_]] = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*Module.*;.*AuthService.*;models\\.data\\..*;uk.gov.hmrc.BuildInfo;app.*;nr.*;res.*;prod.*;.*RuleAST.*;config.*;testOnlyDoNotUseInAppConf.*;definition.*;.*FeatureSwitch.*",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions ++= Seq(
      "-Wconf:src=routes/.*:s",
      "-Ywarn-dead-code",
      "-Xfatal-warnings",
      "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals", // Warn if a local definition is unused.
      "-Ywarn-unused:params", // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates", // Warn if a private member is unused.
      "-language:higherKinds",
      "-Wconf:src=routes/.*:s",
      "-Wconf:cat=unused-imports&src=html/.*:s"
    ),
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.19.2"
  )
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(coverageSettings: _*)
  .disablePlugins(JUnitXmlReportPlugin)

lazy val it = project
    .enablePlugins(PlayScala)
    .dependsOn(microservice % "test->test")
    .settings(DefaultBuildSettings.itSettings())