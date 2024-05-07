import play.core.PlayVersion
import sbt.*

object AppDependencies {

  private val BootstrapPlayVersion = "8.5.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % BootstrapPlayVersion,
    "org.typelevel" %% "cats-core"                 % "2.10.0",
    "com.beachape"  %% "enumeratum-play-json"      % "1.8.0"
  )

  val test = Seq(
    "uk.gov.hmrc"                      %% "bootstrap-test-play-30"      % BootstrapPlayVersion            % Test,
    "org.scalatest"                    %% "scalatest"                   % "3.2.18"                         % Test,
    "org.scalamock"                    %% "scalamock"                   % "5.1.0"                         % Test,
    "org.playframework"                %% "play-test"                   % PlayVersion.current             % Test,
    "com.fasterxml.jackson.module"     %% "jackson-module-scala"        % "2.17.0"                        % Test,
    "com.vladsch.flexmark"             %  "flexmark-all"                % "0.64.8"                        % Test,
    "org.scalatestplus.play"           %% "scalatestplus-play"          % "7.0.1"                         % Test,
    "com.networknt"                    %  "json-schema-validator"       % "1.4.0"                        % Test,
    "org.openapi4j"                    %  "openapi-operation-validator" % "1.0.7"                         % Test,
    "org.openapi4j"                    %  "openapi-parser"              % "1.0.7"                         % Test,
    "com.softwaremill.quicklens"       %% "quicklens"                   % "1.9.7"                         % Test
  )
}
