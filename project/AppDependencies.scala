import play.core.PlayVersion
import sbt.*

object AppDependencies {

  private val BootstrapPlayVersion = "10.7.0"

  val compile = Seq(
    // This is necessary until the HMRC/Play dependencies bring in the version of Jackson that is not insecure.
    "com.fasterxml.jackson.core" % "jackson-core"              % "2.20.2",
    "uk.gov.hmrc"               %% "bootstrap-backend-play-30" % BootstrapPlayVersion,
    "org.typelevel"             %% "cats-core"                 % "2.13.0",
    "com.beachape"              %% "enumeratum-play-json"      % "1.9.7"
  )

  val test = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30"      % BootstrapPlayVersion % Test,
    "org.scalatest"                %% "scalatest"                   % "3.2.20"             % Test,
    "org.scalamock"                %% "scalamock"                   % "7.5.5"              % Test,
    "org.playframework"            %% "play-test"                   % PlayVersion.current  % Test,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"        % "2.20.2"             % Test,
    "com.vladsch.flexmark"          % "flexmark-all"                % "0.64.8"             % Test,
    "org.scalatestplus.play"       %% "scalatestplus-play"          % "7.0.2"              % Test,
    "com.networknt"                 % "json-schema-validator"       % "1.5.9"              % Test
      // This exclusion is because Play/Pekko depends on an old (Jackson) Scala module, incompatible with jackson-databind version 2.17.1 or later.
      // Without an updated Play/Pekko, some Play tests would throw this error:
      //   Scala module 2.14.3 requires Jackson Databind version >= 2.14.0 and < 2.15.0 - Found jackson-databind version 2.17.1
      // We can exclude the later version because our schema tests don't use many features of Jackson.
      // An earlier version of the library is added by our other dependencies.
      exclude ("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml" /* would be version 2.17.1 or later */ )
      exclude ("com.fasterxml.jackson.core", "jackson-databind" /* would be version 2.17.1 or later */ ),
    "org.openapi4j"                 % "openapi-operation-validator" % "1.0.7"              % Test,
    "org.openapi4j"                 % "openapi-parser"              % "1.0.7"              % Test,
    "com.softwaremill.quicklens"   %% "quicklens"                   % "1.9.12"             % Test
  )
}
