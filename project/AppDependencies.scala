import play.core.PlayVersion
import sbt.*

object AppDependencies {

  private val BootstrapPlayVersion = "9.19.0"

  val compile = Seq(
    // This is necessary until the HMRC/Play dependencies bring in the version of Jackson that is not insecure.
    "com.fasterxml.jackson.core"       % "jackson-core"               % "2.20.0",
    "uk.gov.hmrc"                      %% "bootstrap-backend-play-30" % BootstrapPlayVersion,
    "org.typelevel"                    %% "cats-core"                 % "2.13.0",
    "com.beachape"                     %% "enumeratum-play-json"      % "1.9.0"
  )

  val test = Seq(
    "uk.gov.hmrc"                      %% "bootstrap-test-play-30"      % BootstrapPlayVersion            % Test,
    "org.scalatest"                    %% "scalatest"                   % "3.2.19"                         % Test,
    "org.scalamock"                    %% "scalamock"                   % "7.5.0"                         % Test,
    "org.playframework"                %% "play-test"                   % PlayVersion.current             % Test,
    "com.fasterxml.jackson.module"     %% "jackson-module-scala"        % "2.19.0"                        % Test,
    "com.vladsch.flexmark"             %  "flexmark-all"                % "0.64.8"                        % Test,
    "org.scalatestplus.play"           %% "scalatestplus-play"          % "7.0.1"                         % Test,
    "com.networknt"                    %  "json-schema-validator"       % "1.5.7"                        % Test,
    "org.openapi4j"                    %  "openapi-operation-validator" % "1.0.7"                         % Test,
    "org.openapi4j"                    %  "openapi-parser"              % "1.0.7"                         % Test,
    "com.softwaremill.quicklens"       %% "quicklens"                   % "1.9.12"                         % Test
  )
}
