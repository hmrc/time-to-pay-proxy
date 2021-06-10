import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {
  private val allTestEnvs = "test, it"
  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.3.0",
    "uk.gov.hmrc"             %% "auth-client"                % "3.3.0-play-27",
    "org.typelevel"           %% "cats-core"                  % "2.3.0",
    "com.beachape"            %% "enumeratum-play-json"       % "1.6.1"

  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.3.0"             % allTestEnvs,
    "com.github.tomakehurst"  %  "wiremock"                   % "2.27.2"            % allTestEnvs,
    "org.scalatest"           %% "scalatest"                  % "3.2.5"             % allTestEnvs,
    "org.scalamock"           %% "scalamock"                  % "5.1.0"             % allTestEnvs,
    "com.typesafe.play"       %% "play-test"                  % PlayVersion.current % allTestEnvs,
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"            % allTestEnvs,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"             % allTestEnvs
  )
  val overrides: Seq[ModuleID] = {
    val jettyFromWiremockVersion = "9.4.35.v20201120"
    Seq(
      "org.eclipse.jetty"           % "jetty-client"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-continuation" % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-http"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-io"           % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-security"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-server"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlet"      % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlets"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-util"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-webapp"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-xml"          % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-api"      % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-client"   % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-common"   % jettyFromWiremockVersion
    )
  }
}
