/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.timetopayproxy.connectors.util

import org.apache.pekko.http.scaladsl.model.{ StatusCode, StatusCodes }
import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import org.slf4j.{ Logger => Slf4jLogger }
import play.api.Logger
import play.api.libs.json.{ Json, OFormat }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, HttpResponse }
import uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilderimpl.commontoallrepos.{ HttpReadsBuilderError, HttpReadsBuilderErrorConverter, ResponseContext }
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger

import scala.util.Try

/* ℹ️ Note about updating this file: ℹ️
 * This file must be kept consistent with every copy in the other debt-transformation repos.
 * The most complex features are required by time-to-pay, so that is the best place to test any refactoring.
 */
final class HttpReadsBuilderSpec extends AnyFreeSpec with MockFactory {
  "HttpReadsBuilder" - {
    implicit val hc: HeaderCarrier = new HeaderCarrier()

    sealed trait TestFixtureWithoutApply {
      private val underlyingLogger: Slf4jLogger = mock[Slf4jLogger]

      protected val logger: RequestAwareLogger = new RequestAwareLogger(new Logger(underlyingLogger))

      /** List of `(LEVEL, MESSAGE)` tuples to assert on at the end of each test.
        * e.g. {{{"ERROR" -> "My error log line"}}}, {{{"WARN" -> "My warning line"}}}
        */
      var allCapturedLogs: List[(String, String)] = Nil
      locally {
        // Make the mock automatically allow (and capture) all WARN and ERROR logs.
        // As long as each test asserts on `allCapturedLogs`, there's no reason to be upfront about which logs are allowed.

        (() => underlyingLogger.isErrorEnabled).expects().anyNumberOfTimes().returning(true)
        (underlyingLogger.error(_: String)).expects(*).anyNumberOfTimes().onCall { (msg: String) =>
          allCapturedLogs = allCapturedLogs :+ ("ERROR", msg)
        }
        (() => underlyingLogger.isWarnEnabled).expects().anyNumberOfTimes().returning(true)
        (underlyingLogger.warn(_: String)).expects(*).anyNumberOfTimes().onCall { (msg: String) =>
          allCapturedLogs = allCapturedLogs :+ ("WARN", msg)
        }
      }
    }

    def statusString(statusCode: Int): String =
      Try(StatusCode.int2StatusCode(statusCode))
        .getOrElse(StatusCodes.custom(statusCode, reason = "Custom Status"))
        .value

    object ErrorConverterToTupleString extends HttpReadsBuilderErrorConverter[String] {
      def toConnectorError[ServError >: String](builderError: HttpReadsBuilderError[ServError]): ServError =
        stringRepr(builderError)

      def stringRepr[ServError >: String](builderError: Any): ServError =
        builderError match {
          case ResponseContext(method, url, response) =>
            val headerLines = response.headers.flatMap(h => h._2.map((h._1, _))).map(h => s"${h._1}: ${h._2}").toList
            val bodyLines = List(if (response.body.nonEmpty) response.body else "<empty body>")

            val lines = List("===") ++
              List(s"HTTP ${statusString(response.status)}") ++
              headerLines ++
              List("") ++
              bodyLines ++
              List("===")

            s"""RESPONSE TO: $method $url
               |${lines.mkString("\n")}""".stripMargin
          case product: Product with HttpReadsBuilderError[_] =>
            val productPairs = product.productElementNames
              .zip(product.productIterator)
              .map(kv => s"${this.stringRepr(kv._1)} = ${this.stringRepr(kv._2)}")
              .map(_.replaceAll(raw"(?:^|\n)(?= *\S)", "$0  ")) // Indent non-empty lines. (?=...) is a lookahead.
              // Prepend a '#' to each line to make the Kibana test pass visually. (Where Kibana strips newlines)
              // This is just to make the tests more visually distinctive and has no bearing on production.
              .map(_.replaceAll("^|\n", "$0#"))
              .mkString("\n", ",\n", "\n")

            s"""${product.productPrefix}($productPairs)"""
          case nonProduct => nonProduct.toString
        }
    }

    ".empty then .httpReads" - {
      // Here we're testing the fallback behaviour because nothing was configured.
      // Could add cases for known incoming errors.

      "when given a normal logger and header carrier" - {
        "when reading a 200 OK" - {
          "with an empty body" in new TestFixtureWithoutApply {
            val httpReads: HttpReads[Either[String, Nothing]] =
              HttpReadsBuilder
                .empty[String, Nothing](
                  sourceClass = classOf[java.lang.Thread],
                  ErrorConverterToTupleString
                )
                .httpReads(logger, e => s"[DEEMED SAFE BY TEST LOGIC] <${e.toString}>")(hc)

            val response: HttpResponse = HttpResponse(status = 200, body = "", headers = Map())

            val result: Either[String, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              """GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 200 OK
                |#
                |#  <empty body>
                |#  ===,
                |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                """HTTP status is unexpected in received HTTP response.
                  |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 200 OK
                  |#
                  |#  <empty body>
                  |#  ===,
                  |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                  |)> .
                  |Request made for received HTTP response: MYMETHOD some/url .
                  |Received HTTP response status: 200.
                  |Received HTTP response body not logged for 2xx statuses.""".stripMargin
            )
          }

          "with an empty JSON body" in new TestFixtureWithoutApply {
            val httpReads: HttpReads[Either[String, Nothing]] =
              HttpReadsBuilder
                .empty[String, Nothing](
                  sourceClass = classOf[java.lang.Thread],
                  ErrorConverterToTupleString
                )
                .httpReads(logger, e => s"[DEEMED SAFE BY TEST LOGIC] <${e.toString}>")(hc)

            val response: HttpResponse = HttpResponse(status = 200, body = "{}", headers = Map())

            val result: Either[String, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              """GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 200 OK
                |#
                |#  {}
                |#  ===,
                |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                """HTTP status is unexpected in received HTTP response.
                  |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 200 OK
                  |#
                  |#  {}
                  |#  ===,
                  |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                  |)> .
                  |Request made for received HTTP response: MYMETHOD some/url .
                  |Received HTTP response status: 200.
                  |Received HTTP response body not logged for 2xx statuses.""".stripMargin
            )
          }
        }

        "when reading a 400 BAD REQUEST" - {
          "with an empty body" in new TestFixtureWithoutApply {
            val httpReads: HttpReads[Either[String, Nothing]] =
              HttpReadsBuilder
                .empty[String, Nothing](
                  sourceClass = classOf[java.lang.Thread],
                  ErrorConverterToTupleString
                )
                .httpReads(logger, e => s"[DEEMED SAFE BY TEST LOGIC] <${e.toString}>")(hc)

            val response: HttpResponse = HttpResponse(status = 400, body = "", headers = Map())

            val result: Either[String, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              """GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 400 Bad Request
                |#
                |#  <empty body>
                |#  ===,
                |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                """HTTP status is unexpected in received HTTP response.
                  |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 400 Bad Request
                  |#
                  |#  <empty body>
                  |#  ===,
                  |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                  |)> .
                  |Request made for received HTTP response: MYMETHOD some/url .
                  |Received HTTP response status: 400.
                  |Received HTTP response body: """.stripMargin
            )
          }

          "with an empty JSON body" in new TestFixtureWithoutApply {
            val httpReads: HttpReads[Either[String, Nothing]] =
              HttpReadsBuilder
                .empty[String, Nothing](
                  sourceClass = classOf[java.lang.Thread],
                  ErrorConverterToTupleString
                )
                .httpReads(logger, makeErrorSafeToLogInProd = e => s"[DEEMED SAFE BY TEST LOGIC] <${e.toString}>")(hc)

            val response: HttpResponse = HttpResponse(status = 400, body = "{}", headers = Map())

            val result: Either[String, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              """GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 400 Bad Request
                |#
                |#  {}
                |#  ===,
                |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                """HTTP status is unexpected in received HTTP response.
                  |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 400 Bad Request
                  |#
                  |#  {}
                  |#  ===,
                  |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                  |)> .
                  |Request made for received HTTP response: MYMETHOD some/url .
                  |Received HTTP response status: 400.
                  |Received HTTP response body: {}""".stripMargin
            )
          }
        }

        "when reading a 299 status with an empty JSON body, still doesn't log the body" in new TestFixtureWithoutApply {

          val httpReads: HttpReads[Either[String, Nothing]] =
            HttpReadsBuilder
              .empty[String, Nothing](
                sourceClass = classOf[java.lang.Thread],
                ErrorConverterToTupleString
              )
              .httpReads(logger, makeErrorSafeToLogInProd = e => s"[DEEMED SAFE BY TEST LOGIC] <${e.toString}>")(hc)

          val response: HttpResponse =
            HttpResponse(status = 299, body = "{}", headers = Map())

          val result: Either[String, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
          result shouldBe Left(
            """GeneralErrorForUnsuccessfulStatusCode(
              |#  sourceClass = class java.lang.Thread,
              |#  responseContext = RESPONSE TO: MYMETHOD some/url
              |#  ===
              |#  HTTP 299 Custom Status
              |#
              |#  {}
              |#  ===,
              |#  simpleMessage = HTTP status is unexpected in received HTTP response.
              |)""".stripMargin
          )

          allCapturedLogs shouldBe List(
            "ERROR" ->
              """HTTP status is unexpected in received HTTP response.
                |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 299 Custom Status
                |#
                |#  {}
                |#  ===,
                |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                |)> .
                |Request made for received HTTP response: MYMETHOD some/url .
                |Received HTTP response status: 299.
                |Received HTTP response body not logged for 2xx statuses.""".stripMargin
          )
        }
      }
    }

    ".empty + (all handlers)" - {
      // We're running the statuses in one large combination because this would be many thousands more unit tests otherwise.
      // We'll still test atypical values explicitly, to prove that no special handling for 200 and 201 exists.
      // The point of this test group isn't to test construction, but how the different handlers interact.

      import TestDataForCombinations.SuccessWrapper

      sealed trait TestFixtureWithConstructor[Result] extends TestFixtureWithoutApply {
        protected def createTestedBuilder: HttpReadsBuilder[AnyRef, Result] =
          HttpReadsBuilder.empty[AnyRef, Result](
            sourceClass = classOf[java.lang.Thread],
            ErrorConverterToTupleString
          )
      }

      sealed trait TestFixtureForHttpReadsNoLogging extends TestFixtureWithConstructor[SuccessWrapper] {

        lazy val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] =
          TestDataForCombinations.makeHttpReadsBuilder(createTestedBuilder).httpReadsNoLogging
      }

      sealed trait TestFixtureForHttpReads extends TestFixtureWithConstructor[SuccessWrapper] {

        lazy val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] =
          TestDataForCombinations
            .makeHttpReadsBuilder(createTestedBuilder)
            .httpReads(logger, makeErrorSafeToLogInProd = safeErrorString)(hc)

        /** Since our errors may or may not have safe representations, this will be our way of pretending in the logs. */
        private def safeErrorString(e: AnyRef): String = s"[DEEMED SAFE BY TEST LOGIC] <$e>"
      }

      object TestDataForCombinations {

        // Used to check the handleSuccess method for a 2xx. (Basic expected scenario)
        val successStatus236: Int = 236
        // Used to check the handleSuccessNoEntity method for a 2xx. (Basic expected scenario)
        val successStatus265NoBody: Int = 265
        // Used to check the handleSuccess method for a non-2xx. (Unusual, but it must work.)
        val successStatus419: Int = 419
        // Used to check the handleSuccessNoEntity method for a non-2xx. (Unusual, but it must work.)
        val successStatus445NoBody: Int = 445
        // Used to check the handleError method. (Body MAY be logged in production.)
        val errorStatus109: Int = 109
        // Used to check the handleErrorTransformed method. (Body MUST NOT be logged in production.)
        val errorStatus211Transf: Int = 211
        // Used to check the handleErrorNoEntity method. (Body MUST NOT be logged in production.)
        val errorStatus277NoBody: Int = 277
        // Used to check the handleErrorNoEntity method. (Body MAY be logged in production.)
        val errorStatus477NoBody: Int = 477

        val expectedStatuses: Seq[Int] = Seq(
          successStatus236,
          successStatus265NoBody,
          successStatus419,
          successStatus445NoBody,
          errorStatus109,
          errorStatus211Transf,
          errorStatus277NoBody,
          errorStatus477NoBody
        )
        val unexpectedStatuses2xx: Seq[Int] = (200 to 299) diff expectedStatuses
        val unexpectedStatusesNon2xx: Seq[Int] = (100 to 199) ++ (300 to 599) diff expectedStatuses

        sealed trait SuccessWrapper

        final case class SuccessWrapper236(for236: Boolean) extends SuccessWrapper
        object SuccessWrapper236 {
          def exampleValue: SuccessWrapper236 = SuccessWrapper236(true)
          def exampleBody: String = Json.toJson(exampleValue).toString

          implicit def format: OFormat[SuccessWrapper236] = Json.format[SuccessWrapper236]
        }

        case object SuccessWrapper265NoBody extends SuccessWrapper {
          def exampleBody: String = ""
        }

        final case class SuccessWrapper419(for419: Boolean) extends SuccessWrapper
        object SuccessWrapper419 {
          def exampleValue: SuccessWrapper419 = SuccessWrapper419(true)
          def exampleBody: String = Json.toJson(exampleValue).toString

          implicit def format: OFormat[SuccessWrapper419] = Json.format[SuccessWrapper419]
        }

        case object SuccessWrapper445NoBody extends SuccessWrapper {
          def exampleBody: String = ""
        }

        final case class ErrorFor109(for109: String)
        object ErrorFor109 {
          def exampleValue: ErrorFor109 = ErrorFor109("example")
          def exampleBody: String = Json.toJson(exampleValue).toString

          implicit def format: OFormat[ErrorFor109] = Json.format[ErrorFor109]
        }

        final case class ErrorFor211ToTransform(for211: String) {
          def myTransform: ErrorFor211Transformed = ErrorFor211Transformed(for211 = this.for211)
        }
        object ErrorFor211ToTransform {
          def exampleValue: ErrorFor211ToTransform = ErrorFor211ToTransform("example")
          def exampleBody: String = Json.toJson(exampleValue).toString

          implicit def format: OFormat[ErrorFor211ToTransform] = Json.format[ErrorFor211ToTransform]
        }

        case object ErrorFor277NoBody {
          def exampleBody: String = ""
        }

        final case class ErrorFor211Transformed(for211: String)

        case object ErrorFor477NoBody {
          def exampleBody: String = ""
        }

        // AnyRef is a common supertype for CatchAllConnectorError and our errors.
        def makeHttpReadsBuilder(
          obj: HttpReadsBuilder[AnyRef, SuccessWrapper]
        ): HttpReadsBuilder[AnyRef, SuccessWrapper] =
          obj
            .handleSuccess[SuccessWrapper236](incomingStatus = successStatus236)
            .handleSuccessNoEntity(incomingStatus = 445, value = SuccessWrapper445NoBody)
            .handleSuccess[SuccessWrapper419](incomingStatus = successStatus419)
            .handleSuccessNoEntity(incomingStatus = successStatus265NoBody, value = SuccessWrapper265NoBody)
            .handleError[ErrorFor109](incomingStatus = errorStatus109)
            .handleErrorTransformed[ErrorFor211ToTransform](errorStatus211Transf, _.myTransform)
            .handleErrorNoEntity(incomingStatus = errorStatus277NoBody, value = ErrorFor277NoBody)
            .handleErrorNoEntity(incomingStatus = errorStatus477NoBody, value = ErrorFor477NoBody)
      }

      "(check the test data)" in {
        import TestDataForCombinations._

        unexpectedStatuses2xx should have size 96
        unexpectedStatusesNon2xx should have size 396
      }

      "when given many requests and checking the logs with stripped line breaks" - {
        // We won't test .httpReadsNoLogging here because how Kibana handles line breaks would be irrelevant for it.

        "then .httpReads" in new TestFixtureForHttpReads {
          import TestDataForCombinations._

          httpReads.read(
            "POST",
            "https://some.domain/for-success-236-wrong-structure",
            HttpResponse(status = successStatus236, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-236-not-json",
            HttpResponse(status = successStatus236, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-265-wrong-structure",
            HttpResponse(status = successStatus265NoBody, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-265-not-json",
            HttpResponse(status = successStatus265NoBody, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-419-wrong-structure",
            HttpResponse(status = successStatus419, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-419-not-json",
            HttpResponse(status = successStatus419, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-445-wrong-structure",
            HttpResponse(status = successStatus445NoBody, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-445-not-json",
            HttpResponse(status = successStatus445NoBody, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-109-correct-structure-for-warning-log",
            HttpResponse(status = errorStatus109, body = ErrorFor109.exampleBody, headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-109-wrong-structure",
            HttpResponse(status = errorStatus109, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-109-not-json",
            HttpResponse(status = errorStatus109, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-211-correct-structure-for-warning-log",
            HttpResponse(status = errorStatus211Transf, body = ErrorFor211ToTransform.exampleBody, headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-211-wrong-structure",
            HttpResponse(status = errorStatus211Transf, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-211-not-json",
            HttpResponse(status = errorStatus211Transf, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-277-correct-structure-for-warning-log",
            HttpResponse(status = errorStatus277NoBody, body = ErrorFor277NoBody.exampleBody, headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-277-wrong-structure",
            HttpResponse(status = errorStatus277NoBody, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-277-not-json",
            HttpResponse(status = errorStatus277NoBody, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-477-correct-structure-for-warning-log",
            HttpResponse(status = errorStatus477NoBody, body = ErrorFor477NoBody.exampleBody, headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-477-wrong-structure",
            HttpResponse(status = errorStatus477NoBody, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-477-not-json",
            HttpResponse(status = errorStatus477NoBody, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-299-unexpected-success",
            HttpResponse(status = 299, body = """some irrelevant body because status is unexpected""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-404-unexpected-failure",
            HttpResponse(status = 404, body = """some irrelevant body because status is unexpected""", headers = Map())
          )

          val stripped: List[String] =
            allCapturedLogs.map { case (level, msg) => s"$level | ${msg.replaceAll("\n", " ")}" }

          // The plain log line must make sense and be readable when Kibana strips the line breaks.
          // The URLs should not touch any punctuation.
          stripped shouldBe List(
            """ERROR | JSON structure is not valid in received successful HTTP response. Returning: [DEEMED SAFE BY TEST LOGIC] <JsonNotValidErrorForSuccess( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-236-wrong-structure #  === #  HTTP 236 Custom Status # #  {} #  ===, #  errs = List((/for236,List(JsonValidationError(List(error.path.missing),List())))) )> . Request made for received HTTP response: POST https://some.domain/for-success-236-wrong-structure . Received HTTP response status: 236. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | HTTP body is not JSON in received successful HTTP response. Returning: [DEEMED SAFE BY TEST LOGIC] <NotJsonErrorForSuccess( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-236-not-json #  === #  HTTP 236 Custom Status # #  some body that's not JSON #  ===, #  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'some': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') #   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5] )> . Request made for received HTTP response: POST https://some.domain/for-success-236-not-json . Received HTTP response status: 236. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | Body of successful upstream HTTP response is not empty. Trimmed body length: 2. Returning: [DEEMED SAFE BY TEST LOGIC] <BodyNotEmptyErrorForSuccess( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-265-wrong-structure #  === #  HTTP 265 Custom Status # #  {} #  === )> . Request made for received HTTP response: POST https://some.domain/for-success-265-wrong-structure . Received HTTP response status: 265. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | Body of successful upstream HTTP response is not empty. Trimmed body length: 25. Returning: [DEEMED SAFE BY TEST LOGIC] <BodyNotEmptyErrorForSuccess( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-265-not-json #  === #  HTTP 265 Custom Status # #  some body that's not JSON #  === )> . Request made for received HTTP response: POST https://some.domain/for-success-265-not-json . Received HTTP response status: 265. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | JSON structure is not valid in received successful HTTP response. Returning: [DEEMED SAFE BY TEST LOGIC] <JsonNotValidErrorForSuccess( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-419-wrong-structure #  === #  HTTP 419 Custom Status # #  {} #  ===, #  errs = List((/for419,List(JsonValidationError(List(error.path.missing),List())))) )> . Request made for received HTTP response: POST https://some.domain/for-success-419-wrong-structure . Received HTTP response status: 419. Received HTTP response body: {}""",
            """ERROR | HTTP body is not JSON in received successful HTTP response. Returning: [DEEMED SAFE BY TEST LOGIC] <NotJsonErrorForSuccess( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-419-not-json #  === #  HTTP 419 Custom Status # #  some body that's not JSON #  ===, #  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'some': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') #   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5] )> . Request made for received HTTP response: POST https://some.domain/for-success-419-not-json . Received HTTP response status: 419. Received HTTP response body: some body that's not JSON""",
            """ERROR | Body of successful upstream HTTP response is not empty. Trimmed body length: 2. Returning: [DEEMED SAFE BY TEST LOGIC] <BodyNotEmptyErrorForSuccess( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-445-wrong-structure #  === #  HTTP 445 Custom Status # #  {} #  === )> . Request made for received HTTP response: POST https://some.domain/for-success-445-wrong-structure . Received HTTP response status: 445. Received HTTP response body: {}""",
            """ERROR | Body of successful upstream HTTP response is not empty. Trimmed body length: 25. Returning: [DEEMED SAFE BY TEST LOGIC] <BodyNotEmptyErrorForSuccess( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-445-not-json #  === #  HTTP 445 Custom Status # #  some body that's not JSON #  === )> . Request made for received HTTP response: POST https://some.domain/for-success-445-not-json . Received HTTP response status: 445. Received HTTP response body: some body that's not JSON""",
            """WARN | Valid and expected error response was received from HTTP call. Request made for received HTTP response: POST https://some.domain/for-failure-109-correct-structure-for-warning-log . Received HTTP response status: 109. Received HTTP response body: {"for109":"example"}""",
            """ERROR | JSON structure is not valid in received error HTTP response.   Validation errors:     - For path /for109 , errors: [error.path.missing]. Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-109-wrong-structure #  === #  HTTP 109 Custom Status # #  {} #  ===, #  simpleMessage = JSON structure is not valid in received error HTTP response. #    Validation errors: #      - For path /for109 , errors: [error.path.missing]. )> . Request made for received HTTP response: POST https://some.domain/for-failure-109-wrong-structure . Received HTTP response status: 109. Received HTTP response body: {}""",
            """ERROR | HTTP body is not JSON in received error HTTP response. Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-109-not-json #  === #  HTTP 109 Custom Status # #  some body that's not JSON #  ===, #  simpleMessage = HTTP body is not JSON in received error HTTP response. )> . Request made for received HTTP response: POST https://some.domain/for-failure-109-not-json . Received HTTP response status: 109. Received HTTP response body: some body that's not JSON""",
            """WARN | Valid and expected error response was received from HTTP call. Request made for received HTTP response: POST https://some.domain/for-failure-211-correct-structure-for-warning-log . Received HTTP response status: 211. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | JSON structure is not valid in received error HTTP response. Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-211-wrong-structure #  === #  HTTP 211 Custom Status # #  {} #  ===, #  simpleMessage = JSON structure is not valid in received error HTTP response. )> . Request made for received HTTP response: POST https://some.domain/for-failure-211-wrong-structure . Received HTTP response status: 211. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | HTTP body is not JSON in received error HTTP response. Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-211-not-json #  === #  HTTP 211 Custom Status # #  some body that's not JSON #  ===, #  simpleMessage = HTTP body is not JSON in received error HTTP response. )> . Request made for received HTTP response: POST https://some.domain/for-failure-211-not-json . Received HTTP response status: 211. Received HTTP response body not logged for 2xx statuses.""",
            """WARN | Valid and expected error response was received from HTTP call. Request made for received HTTP response: POST https://some.domain/for-failure-277-correct-structure-for-warning-log . Received HTTP response status: 277. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | Body of error upstream HTTP response is not empty. Trimmed body length: 2. Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-277-wrong-structure #  === #  HTTP 277 Custom Status # #  {} #  ===, #  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 2. )> . Request made for received HTTP response: POST https://some.domain/for-failure-277-wrong-structure . Received HTTP response status: 277. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | Body of error upstream HTTP response is not empty. Trimmed body length: 25. Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-277-not-json #  === #  HTTP 277 Custom Status # #  some body that's not JSON #  ===, #  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 25. )> . Request made for received HTTP response: POST https://some.domain/for-failure-277-not-json . Received HTTP response status: 277. Received HTTP response body not logged for 2xx statuses.""",
            """WARN | Valid and expected error response was received from HTTP call. Request made for received HTTP response: POST https://some.domain/for-failure-477-correct-structure-for-warning-log . Received HTTP response status: 477. Received HTTP response body: """,
            """ERROR | Body of error upstream HTTP response is not empty. Trimmed body length: 2. Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-477-wrong-structure #  === #  HTTP 477 Custom Status # #  {} #  ===, #  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 2. )> . Request made for received HTTP response: POST https://some.domain/for-failure-477-wrong-structure . Received HTTP response status: 477. Received HTTP response body: {}""",
            """ERROR | Body of error upstream HTTP response is not empty. Trimmed body length: 25. Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-477-not-json #  === #  HTTP 477 Custom Status # #  some body that's not JSON #  ===, #  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 25. )> . Request made for received HTTP response: POST https://some.domain/for-failure-477-not-json . Received HTTP response status: 477. Received HTTP response body: some body that's not JSON""",
            """ERROR | HTTP status is unexpected in received HTTP response. Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-299-unexpected-success #  === #  HTTP 299 Custom Status # #  some irrelevant body because status is unexpected #  ===, #  simpleMessage = HTTP status is unexpected in received HTTP response. )> . Request made for received HTTP response: POST https://some.domain/for-299-unexpected-success . Received HTTP response status: 299. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | HTTP status is unexpected in received HTTP response. Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-404-unexpected-failure #  === #  HTTP 404 Not Found # #  some irrelevant body because status is unexpected #  ===, #  simpleMessage = HTTP status is unexpected in received HTTP response. )> . Request made for received HTTP response: POST https://some.domain/for-404-unexpected-failure . Received HTTP response status: 404. Received HTTP response body: some irrelevant body because status is unexpected"""
          )
        }
      }

      "when given the successful statuses" - {
        import TestDataForCombinations.{ SuccessWrapper236, SuccessWrapper265NoBody, SuccessWrapper419, SuccessWrapper445NoBody, successStatus236, successStatus265NoBody, successStatus419, successStatus445NoBody }

        "and a valid body" - {
          for {
            (successfulStatus, exampleValue, exampleBody) <-
              List(
                (
                  successStatus236,
                  SuccessWrapper236.exampleValue,
                  SuccessWrapper236.exampleBody
                ),
                (
                  successStatus265NoBody,
                  SuccessWrapper265NoBody,
                  SuccessWrapper265NoBody.exampleBody
                ),
                (
                  successStatus419,
                  SuccessWrapper419.exampleValue,
                  SuccessWrapper419.exampleBody
                ),
                (
                  successStatus445NoBody,
                  SuccessWrapper445NoBody,
                  SuccessWrapper445NoBody.exampleBody
                )
              )
          } s"<$successfulStatus>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse = HttpResponse(status = successfulStatus, body = exampleBody, headers = Map())

              val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Right(exampleValue)
              allCapturedLogs shouldBe Nil
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse = HttpResponse(status = successfulStatus, body = exampleBody, headers = Map())

              val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Right(exampleValue)
              allCapturedLogs shouldBe Nil
            }
          }
        }

        "and an empty JSON body" - {
          s"<$successStatus236>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse = HttpResponse(status = successStatus236, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """JsonNotValidErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 236 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  errs = List((/for236,List(JsonValidationError(List(error.path.missing),List()))))
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""JSON structure is not valid in received successful HTTP response.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <JsonNotValidErrorForSuccess(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 236 Custom Status
                     |#
                     |#  {}
                     |#  ===,
                     |#  errs = List((/for236,List(JsonValidationError(List(error.path.missing),List()))))
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 236.
                     |Received HTTP response body not logged for 2xx statuses.""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse = HttpResponse(status = successStatus236, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """JsonNotValidErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 236 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  errs = List((/for236,List(JsonValidationError(List(error.path.missing),List()))))
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<$successStatus265NoBody>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse =
                HttpResponse(status = successStatus265NoBody, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """BodyNotEmptyErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 265 Custom Status
                  |#
                  |#  {}
                  |#  ===
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""Body of successful upstream HTTP response is not empty. Trimmed body length: 2.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <BodyNotEmptyErrorForSuccess(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 265 Custom Status
                     |#
                     |#  {}
                     |#  ===
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 265.
                     |Received HTTP response body not logged for 2xx statuses.""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse =
                HttpResponse(status = successStatus265NoBody, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """BodyNotEmptyErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 265 Custom Status
                  |#
                  |#  {}
                  |#  ===
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<$successStatus419>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse = HttpResponse(status = successStatus419, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """JsonNotValidErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 419 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  errs = List((/for419,List(JsonValidationError(List(error.path.missing),List()))))
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""JSON structure is not valid in received successful HTTP response.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <JsonNotValidErrorForSuccess(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 419 Custom Status
                     |#
                     |#  {}
                     |#  ===,
                     |#  errs = List((/for419,List(JsonValidationError(List(error.path.missing),List()))))
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 419.
                     |Received HTTP response body: {}""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse = HttpResponse(status = successStatus419, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """JsonNotValidErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 419 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  errs = List((/for419,List(JsonValidationError(List(error.path.missing),List()))))
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<$successStatus445NoBody>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse =
                HttpResponse(status = successStatus445NoBody, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """BodyNotEmptyErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 445 Custom Status
                  |#
                  |#  {}
                  |#  ===
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""Body of successful upstream HTTP response is not empty. Trimmed body length: 2.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <BodyNotEmptyErrorForSuccess(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 445 Custom Status
                     |#
                     |#  {}
                     |#  ===
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 445.
                     |Received HTTP response body: {}""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse =
                HttpResponse(status = successStatus445NoBody, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """BodyNotEmptyErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 445 Custom Status
                  |#
                  |#  {}
                  |#  ===
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }
        }

        "and an unparsable text body" - {
          s"<$successStatus236>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse = HttpResponse(status = successStatus236, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """NotJsonErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 236 Custom Status
                  |#
                  |#  TEXT
                  |#  ===,
                  |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                  |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""HTTP body is not JSON in received successful HTTP response.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <NotJsonErrorForSuccess(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 236 Custom Status
                     |#
                     |#  TEXT
                     |#  ===,
                     |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                     |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 236.
                     |Received HTTP response body not logged for 2xx statuses.""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse = HttpResponse(status = successStatus236, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """NotJsonErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 236 Custom Status
                  |#
                  |#  TEXT
                  |#  ===,
                  |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                  |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<$successStatus265NoBody>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse =
                HttpResponse(status = successStatus265NoBody, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """BodyNotEmptyErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 265 Custom Status
                  |#
                  |#  TEXT
                  |#  ===
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""Body of successful upstream HTTP response is not empty. Trimmed body length: 4.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <BodyNotEmptyErrorForSuccess(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 265 Custom Status
                     |#
                     |#  TEXT
                     |#  ===
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 265.
                     |Received HTTP response body not logged for 2xx statuses.""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse =
                HttpResponse(status = successStatus265NoBody, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                s"""BodyNotEmptyErrorForSuccess(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 265 Custom Status
                   |#
                   |#  TEXT
                   |#  ===
                   |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<$successStatus419>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse = HttpResponse(status = successStatus419, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """NotJsonErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 419 Custom Status
                  |#
                  |#  TEXT
                  |#  ===,
                  |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                  |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""HTTP body is not JSON in received successful HTTP response.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <NotJsonErrorForSuccess(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 419 Custom Status
                     |#
                     |#  TEXT
                     |#  ===,
                     |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                     |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 419.
                     |Received HTTP response body: TEXT""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse = HttpResponse(status = successStatus419, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """NotJsonErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 419 Custom Status
                  |#
                  |#  TEXT
                  |#  ===,
                  |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                  |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<$successStatus445NoBody>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse =
                HttpResponse(status = successStatus445NoBody, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """BodyNotEmptyErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 445 Custom Status
                  |#
                  |#  TEXT
                  |#  ===
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""Body of successful upstream HTTP response is not empty. Trimmed body length: 4.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <BodyNotEmptyErrorForSuccess(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 445 Custom Status
                     |#
                     |#  TEXT
                     |#  ===
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 445.
                     |Received HTTP response body: TEXT""".stripMargin
              )

              allCapturedLogs.toString should include("TEXT") // This success is not a 2xx, so it's safe to log.
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse =
                HttpResponse(status = successStatus445NoBody, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """BodyNotEmptyErrorForSuccess(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 445 Custom Status
                  |#
                  |#  TEXT
                  |#  ===
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }
        }
      }

      "when given the error non-transformed status (which is not 2xx)" - {
        import TestDataForCombinations.{ ErrorFor109, SuccessWrapper, errorStatus109 }

        "and a valid body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse =
              HttpResponse(status = errorStatus109, body = ErrorFor109.exampleBody, headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """PassthroughServiceError(
                |#  error = ErrorFor109(example)
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "WARN" ->
                s"""Valid and expected error response was received from HTTP call.
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 109.
                   |Received HTTP response body: ${ErrorFor109.exampleBody: String}""".stripMargin
            )
          }
        }

        "and an empty JSON body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse = HttpResponse(status = errorStatus109, body = """{}""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 109 Custom Status
                |#
                |#  {}
                |#  ===,
                |#  simpleMessage = JSON structure is not valid in received error HTTP response.
                |#    Validation errors:
                |#      - For path /for109 , errors: [error.path.missing].
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""JSON structure is not valid in received error HTTP response.
                   |  Validation errors:
                   |    - For path /for109 , errors: [error.path.missing].
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 109 Custom Status
                   |#
                   |#  {}
                   |#  ===,
                   |#  simpleMessage = JSON structure is not valid in received error HTTP response.
                   |#    Validation errors:
                   |#      - For path /for109 , errors: [error.path.missing].
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 109.
                   |Received HTTP response body: {}""".stripMargin
            )
          }
        }

        "and an unparsable text body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse = HttpResponse(status = errorStatus109, body = """TEXT""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 109 Custom Status
                |#
                |#  TEXT
                |#  ===,
                |#  simpleMessage = HTTP body is not JSON in received error HTTP response.
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""HTTP body is not JSON in received error HTTP response.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 109 Custom Status
                   |#
                   |#  TEXT
                   |#  ===,
                   |#  simpleMessage = HTTP body is not JSON in received error HTTP response.
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 109.
                   |Received HTTP response body: TEXT""".stripMargin
            )
          }

        }
      }

      "when given the error transformed status (which is 2xx)" - {
        import TestDataForCombinations.{ ErrorFor211ToTransform, SuccessWrapper, errorStatus211Transf }

        "and a valid body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse =
              HttpResponse(status = errorStatus211Transf, body = ErrorFor211ToTransform.exampleBody, headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """PassthroughServiceError(
                |#  error = ErrorFor211Transformed(example)
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "WARN" ->
                s"""Valid and expected error response was received from HTTP call.
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 211.
                   |Received HTTP response body not logged for 2xx statuses.""".stripMargin
            )
          }
        }

        "and an empty JSON body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse =
              HttpResponse(status = errorStatus211Transf, body = """{}""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 211 Custom Status
                |#
                |#  {}
                |#  ===,
                |#  simpleMessage = JSON structure is not valid in received error HTTP response.
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""JSON structure is not valid in received error HTTP response.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 211 Custom Status
                   |#
                   |#  {}
                   |#  ===,
                   |#  simpleMessage = JSON structure is not valid in received error HTTP response.
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 211.
                   |Received HTTP response body not logged for 2xx statuses.""".stripMargin
            )
          }
        }

        "and an unparsable text body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse =
              HttpResponse(status = errorStatus211Transf, body = """TEXT""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              s"""GeneralErrorForUnsuccessfulStatusCode(
                 |#  sourceClass = class java.lang.Thread,
                 |#  responseContext = RESPONSE TO: MYMETHOD some/url
                 |#  ===
                 |#  HTTP 211 Custom Status
                 |#
                 |#  TEXT
                 |#  ===,
                 |#  simpleMessage = HTTP body is not JSON in received error HTTP response.
                 |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""HTTP body is not JSON in received error HTTP response.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 211 Custom Status
                   |#
                   |#  TEXT
                   |#  ===,
                   |#  simpleMessage = HTTP body is not JSON in received error HTTP response.
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 211.
                   |Received HTTP response body not logged for 2xx statuses.""".stripMargin
            )
          }
        }
      }

      "when given the no-body error 2xx status" - {
        import TestDataForCombinations.{ ErrorFor277NoBody, SuccessWrapper, errorStatus277NoBody }

        "and a valid body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse =
              HttpResponse(status = errorStatus277NoBody, body = ErrorFor277NoBody.exampleBody, headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """PassthroughServiceError(
                |#  error = ErrorFor277NoBody
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "WARN" ->
                s"""Valid and expected error response was received from HTTP call.
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 277.
                   |Received HTTP response body not logged for 2xx statuses.""".stripMargin
            )
          }
        }

        "and an empty JSON body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse =
              HttpResponse(status = errorStatus277NoBody, body = """{}""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 277 Custom Status
                |#
                |#  {}
                |#  ===,
                |#  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 2.
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""Body of error upstream HTTP response is not empty. Trimmed body length: 2.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 277 Custom Status
                   |#
                   |#  {}
                   |#  ===,
                   |#  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 2.
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 277.
                   |Received HTTP response body not logged for 2xx statuses.""".stripMargin
            )
          }
        }

        "and an unparsable text body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse =
              HttpResponse(status = errorStatus277NoBody, body = """TEXT""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 277 Custom Status
                |#
                |#  TEXT
                |#  ===,
                |#  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 4.
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""Body of error upstream HTTP response is not empty. Trimmed body length: 4.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 277 Custom Status
                   |#
                   |#  TEXT
                   |#  ===,
                   |#  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 4.
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 277.
                   |Received HTTP response body not logged for 2xx statuses.""".stripMargin
            )
          }
        }
      }

      "when given the no-body error non-2xx status" - {
        import TestDataForCombinations.{ ErrorFor477NoBody, SuccessWrapper, errorStatus477NoBody }

        "and a valid body" - {
          "then .httpReads" in new TestFixtureForHttpReads {

            val response: HttpResponse =
              HttpResponse(status = errorStatus477NoBody, body = ErrorFor477NoBody.exampleBody, headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """PassthroughServiceError(
                |#  error = ErrorFor477NoBody
                |)""".stripMargin
            )
            allCapturedLogs shouldBe List(
              "WARN" ->
                s"""Valid and expected error response was received from HTTP call.
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 477.
                   |Received HTTP response body: ${ErrorFor477NoBody.exampleBody: String}""".stripMargin
            )
          }
        }

        "and an empty JSON body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse = HttpResponse(status = errorStatus477NoBody, body = """{}""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 477 Custom Status
                |#
                |#  {}
                |#  ===,
                |#  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 2.
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""Body of error upstream HTTP response is not empty. Trimmed body length: 2.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 477 Custom Status
                   |#
                   |#  {}
                   |#  ===,
                   |#  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 2.
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 477.
                   |Received HTTP response body: {}""".stripMargin
            )
          }
        }

        "and an unparsable text body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse = HttpResponse(status = errorStatus477NoBody, body = """TEXT""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """GeneralErrorForUnsuccessfulStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 477 Custom Status
                |#
                |#  TEXT
                |#  ===,
                |#  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 4.
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""Body of error upstream HTTP response is not empty. Trimmed body length: 4.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 477 Custom Status
                   |#
                   |#  TEXT
                   |#  ===,
                   |#  simpleMessage = Body of error upstream HTTP response is not empty. Trimmed body length: 4.
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 477.
                   |Received HTTP response body: TEXT""".stripMargin
            )
          }
        }
      }

      "when given an unexpected 2xx status, WILL NOT LOG or return the body" - {
        import TestDataForCombinations.unexpectedStatuses2xx

        "when given the first successful JSON body" - {
          import TestDataForCombinations.SuccessWrapper236

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = SuccessWrapper236.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for236":true}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  {"for236":true}
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpected2xxStatus: Int}.
                       |Received HTTP response body not logged for 2xx statuses.""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = SuccessWrapper236.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for236":true}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the second successful JSON body" - {
          import TestDataForCombinations.SuccessWrapper419

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = SuccessWrapper419.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for419":true}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  {"for419":true}
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpected2xxStatus: Int}.
                       |Received HTTP response body not logged for 2xx statuses.""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = SuccessWrapper419.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for419":true}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the first error JSON body" - {
          import TestDataForCombinations.ErrorFor109

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = ErrorFor109.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for109":"example"}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  {"for109":"example"}
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpected2xxStatus: Int}.
                       |Received HTTP response body not logged for 2xx statuses.""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = ErrorFor109.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for109":"example"}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the second error JSON body" - {
          import TestDataForCombinations.ErrorFor211ToTransform

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = ErrorFor211ToTransform.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for211":"example"}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  {"for211":"example"}
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpected2xxStatus: Int}.
                       |Received HTTP response body not logged for 2xx statuses.""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = ErrorFor211ToTransform.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for211":"example"}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given an empty JSON body" - {
          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = """{}""", headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  {}
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpected2xxStatus: Int}.
                       |Received HTTP response body not logged for 2xx statuses.""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = """{}""", headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given an unparsable text body" - {
          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = """SOMETEXT""", headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  SOMETEXT
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  SOMETEXT
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpected2xxStatus: Int}.
                       |Received HTTP response body not logged for 2xx statuses.""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = """SOMETEXT""", headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  SOMETEXT
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

      }

      "when given an unexpected non-2xx status, WILL LOG the body, but not return it" - {
        import TestDataForCombinations.unexpectedStatusesNon2xx

        "when given the first successful JSON body" - {
          import TestDataForCombinations.SuccessWrapper236

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = SuccessWrapper236.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for236":true}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  {"for236":true}
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpectedNon2xxStatus: Int}.
                       |Received HTTP response body: ${SuccessWrapper236.exampleBody: String}""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = SuccessWrapper236.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for236":true}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the second successful JSON body" - {
          import TestDataForCombinations.SuccessWrapper419

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = SuccessWrapper419.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for419":true}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  {"for419":true}
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpectedNon2xxStatus: Int}.
                       |Received HTTP response body: ${SuccessWrapper419.exampleBody: String}""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = SuccessWrapper419.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for419":true}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the first error JSON body" - {
          import TestDataForCombinations.ErrorFor109

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = ErrorFor109.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for109":"example"}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  {"for109":"example"}
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpectedNon2xxStatus: Int}.
                       |Received HTTP response body: ${ErrorFor109.exampleBody: String}""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = ErrorFor109.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for109":"example"}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the second error JSON body" - {
          import TestDataForCombinations.ErrorFor211ToTransform

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(
                    status = unexpectedNon2xxStatus,
                    body = ErrorFor211ToTransform.exampleBody,
                    headers = Map()
                  )

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for211":"example"}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  {"for211":"example"}
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpectedNon2xxStatus: Int}.
                       |Received HTTP response body: ${ErrorFor211ToTransform.exampleBody: String}""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(
                    status = unexpectedNon2xxStatus,
                    body = ErrorFor211ToTransform.exampleBody,
                    headers = Map()
                  )

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for211":"example"}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given an empty JSON body" - {
          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = """{}""", headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  {}
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpectedNon2xxStatus: Int}.
                       |Received HTTP response body: {}""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = """{}""", headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {}
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given an unparsable text body" - {
          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = """SOMETEXT""", headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  SOMETEXT
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <GeneralErrorForUnsuccessfulStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  SOMETEXT
                       |#  ===,
                       |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpectedNon2xxStatus: Int}.
                       |Received HTTP response body: SOMETEXT""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = """SOMETEXT""", headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""GeneralErrorForUnsuccessfulStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  SOMETEXT
                     |#  ===,
                     |#  simpleMessage = HTTP status is unexpected in received HTTP response.
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

      }

    }
  }
}
