import play.core.PlayVersion
import sbt.*

object AppDependencies {

  private val BootstrapPlayVersion = "10.8.0"

  val compile = Seq(
    // This is necessary until the HMRC/Play dependencies bring in the version of Jackson that is not insecure.
    "com.fasterxml.jackson.core" % "jackson-core"              % "2.20.2",
    "uk.gov.hmrc"               %% "bootstrap-backend-play-30" % BootstrapPlayVersion,
    "org.typelevel"             %% "cats-core"                 % "2.13.0",
    "com.beachape"              %% "enumeratum-play-json"      % "1.9.8"
  )

  val test = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30"      % BootstrapPlayVersion % Test,
    "org.scalatest"                %% "scalatest"                   % "3.2.20"             % Test,
    "org.scalamock"                %% "scalamock"                   % "7.5.5"              % Test,
    "org.playframework"            %% "play-test"                   % PlayVersion.current  % Test,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"        % "2.20.2"             % Test,
    "com.vladsch.flexmark"          % "flexmark-all"                % "0.64.8"             % Test,
    "org.scalatestplus.play"       %% "scalatestplus-play"          % "7.0.2"              % Test,
    "com.networknt"                 % "json-schema-validator"       % "2.0.4"              % Test,
    "com.softwaremill.quicklens"   %% "quicklens"                   % "1.9.12"             % Test
  )

  val dependencyOverrides = Seq(
    "com.fasterxml.jackson.module"  % "jackson-module-scala_2.13" % "2.15.4",
    "com.fasterxml.jackson.core"    % "jackson-databind"          % "2.15.4",
    "uk.gov.hmrc"                   % "http-verbs-play-30_2.13"   % "15.8.0",
    "org.slf4j"                     % "slf4j-api"                 % "2.0.17",
    "com.google.guava"              % "guava"                     % "32.1.3-jre"
  )
}
