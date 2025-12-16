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

package uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder

import org.apache.pekko.http.scaladsl.model.{ StatusCode, StatusCodes }
import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import org.slf4j.{ Logger => Slf4jLogger }
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{ Json, OFormat }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, HttpResponse }
import uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder.implcommontoallrepos.{ HttpReadsBuilderError, HttpReadsBuilderErrorConverter, ResponseContext }
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
      def toConnectorError[ServiceError >: String](builderError: HttpReadsBuilderError[ServiceError]): ServiceError =
        stringRepr(builderError)

      def stringRepr[ServiceError >: String](builderError: Any): ServiceError =
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
              """UnexpectedStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 200 OK
                |#
                |#  <empty body>
                |#  ===
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                """HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                  |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 200 OK
                  |#
                  |#  <empty body>
                  |#  ===
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
              """UnexpectedStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 200 OK
                |#
                |#  {}
                |#  ===
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                """HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                  |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 200 OK
                  |#
                  |#  {}
                  |#  ===
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
              """UnexpectedStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 400 Bad Request
                |#
                |#  <empty body>
                |#  ===
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                """HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                  |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 400 Bad Request
                  |#
                  |#  <empty body>
                  |#  ===
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
              """UnexpectedStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 400 Bad Request
                |#
                |#  {}
                |#  ===
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                """HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                  |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 400 Bad Request
                  |#
                  |#  {}
                  |#  ===
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
            """UnexpectedStatusCode(
              |#  sourceClass = class java.lang.Thread,
              |#  responseContext = RESPONSE TO: MYMETHOD some/url
              |#  ===
              |#  HTTP 299 Custom Status
              |#
              |#  {}
              |#  ===
              |)""".stripMargin
          )

          allCapturedLogs shouldBe List(
            "ERROR" ->
              """HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 299 Custom Status
                |#
                |#  {}
                |#  ===
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

        /** Makes each case class toString easier to distinguish by including the field names and surrounding classes. */
        sealed trait WithClassChainProductToString { this: Product =>
          override final def toString: String = {
            val elements: String =
              this.productElementNames
                .zip(this.productIterator)
                .map { case (name, value) => s"$name=$value" }
                .mkString(", ")

            val classChainString: String =
              Iterator
                // Start with this class, then its parent, then its parent's parent, forever. Lazily computed so it won't throw.
                .iterate[Class[_]](this.getClass)(_.getDeclaringClass)
                .takeWhile(_ != null)
                // Written as a takeWhile instead of dropWhile so we won't empty it if this trait is extended elsewhere by mistake.
                .takeWhile(_ !== classOf[TestDataForCombinations.type])
                .toList
                .reverse
                .map(_.getSimpleName)
                .mkString(".")

            s"""$classChainString($elements)(toString from WithClassChainProductToString)"""
          }
        }

        /** Every value in the `Right` of the return type should extend this.
          * The errors will be converted, so they don't need a supertype.
          */
        sealed trait SuccessWrapper

        /** Used to check the handleSuccess method for a 2xx. (Basic expected scenario) */
        object Succ236Json {
          val status: Int = 236

          final case class Wrapper(for236: Boolean) extends WithClassChainProductToString with SuccessWrapper
          object Wrapper {
            def exampleValue: Wrapper = Wrapper(true)
            def exampleBody: String = Json.toJson(exampleValue).toString

            implicit def format: OFormat[Wrapper] = Json.format[Wrapper]
          }
        }

        /** Used to check the handleSuccessNoEntity method for a 2xx. (Basic expected scenario) */
        object Succ265NoBody {
          val status: Int = 265

          case object Singleton extends WithClassChainProductToString with SuccessWrapper {
            def exampleBody: String = ""
          }
        }

        /** Used to check the handleSuccess method for a non-2xx. (Unusual, but it must work.) */
        object Succ419Json {
          val status: Int = 419

          final case class Wrapper(for419: Boolean) extends WithClassChainProductToString with SuccessWrapper
          object Wrapper {
            def exampleValue: Wrapper = Wrapper(true)
            def exampleBody: String = Json.toJson(exampleValue).toString

            implicit def format: OFormat[Wrapper] = Json.format[Wrapper]
          }
        }

        /** Used to check the handleSuccessNoEntity method for a non-2xx. (Unusual, but it must work.) */
        object Succ445NoBody {
          val status: Int = 445

          case object Singleton extends WithClassChainProductToString with SuccessWrapper {
            def exampleBody: String = ""
          }
        }

        /** Used to check the handleError method. (Body MAY be logged in production.) */
        object Err109Json {
          val status: Int = 109

          final case class Wrapper(for109: String) extends WithClassChainProductToString
          object Wrapper {
            def exampleValue: Wrapper = Wrapper("example")
            def exampleBody: String = Json.toJson(exampleValue).toString

            implicit def format: OFormat[Wrapper] = Json.format[Wrapper]
          }

        }

        /** Used to check the handleErrorTransformed method. (Body MUST NOT be logged in production.) */
        object Err211JsonTransf {
          val status: Int = 211

          final case class ToTransform(for211: String) extends WithClassChainProductToString {
            def myTransform: Transformed = Transformed(for211 = this.for211)
          }
          object ToTransform {
            def exampleValue: ToTransform = ToTransform("example")
            def exampleBody: String = Json.toJson(exampleValue).toString

            implicit def format: OFormat[ToTransform] = Json.format[ToTransform]
          }

          final case class Transformed(for211: String) extends WithClassChainProductToString
        }

        /** Used to check the handleErrorNoEntity method. (Body MUST NOT be logged in production.) */
        object Err277NoBody {
          val status: Int = 277

          case object Singleton extends WithClassChainProductToString {
            def exampleBody: String = ""
          }
        }

        /** Used to check the handleErrorNoEntity method. (Body MAY be logged in production.) */
        object Err477NoBody {
          val status: Int = 477

          case object Singleton extends WithClassChainProductToString {
            def exampleBody: String = ""
          }
        }

        // Successes first, then errors. For each, use the status code order.
        val expectedStatuses: Seq[Int] = Seq(
          Succ236Json.status,
          Succ265NoBody.status,
          Succ419Json.status,
          Succ445NoBody.status,
          Err109Json.status,
          Err211JsonTransf.status,
          Err277NoBody.status,
          Err477NoBody.status
        )
        val unexpectedStatuses2xx: Seq[Int] = (100 to 599).filter(Status.isSuccessful) diff expectedStatuses
        val unexpectedStatusesNon2xx: Seq[Int] = (100 to 599).filterNot(Status.isSuccessful) diff expectedStatuses

        // AnyRef is a common supertype for the centrally implemented connector errors and the errors declared above.
        def makeHttpReadsBuilder(
          obj: HttpReadsBuilder[AnyRef, SuccessWrapper]
        ): HttpReadsBuilder[AnyRef, SuccessWrapper] =
          obj
            // Successes first, then errors. For each, use the status code order.
            .handleSuccess[Succ236Json.Wrapper](incomingStatus = Succ236Json.status)
            .handleSuccessNoEntity(incomingStatus = Succ265NoBody.status, value = Succ265NoBody.Singleton)
            .handleSuccess[Succ419Json.Wrapper](incomingStatus = Succ419Json.status)
            .handleSuccessNoEntity(incomingStatus = Succ445NoBody.status, value = Succ445NoBody.Singleton)
            .handleError[Err109Json.Wrapper](incomingStatus = Err109Json.status)
            .handleErrorTransformed[Err211JsonTransf.ToTransform](Err211JsonTransf.status, _.myTransform)
            .handleErrorNoEntity(incomingStatus = Err277NoBody.status, value = Err277NoBody.Singleton)
            .handleErrorNoEntity(incomingStatus = Err477NoBody.status, value = Err477NoBody.Singleton)
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
            HttpResponse(status = Succ236Json.status, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-236-not-json",
            HttpResponse(status = Succ236Json.status, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-265-wrong-structure",
            HttpResponse(status = Succ265NoBody.status, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-265-not-json",
            HttpResponse(status = Succ265NoBody.status, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-419-wrong-structure",
            HttpResponse(status = Succ419Json.status, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-419-not-json",
            HttpResponse(status = Succ419Json.status, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-445-wrong-structure",
            HttpResponse(status = Succ445NoBody.status, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-success-445-not-json",
            HttpResponse(status = Succ445NoBody.status, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-109-correct-structure-for-warning-log",
            HttpResponse(status = Err109Json.status, body = Err109Json.Wrapper.exampleBody, headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-109-wrong-structure",
            HttpResponse(status = Err109Json.status, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-109-not-json",
            HttpResponse(status = Err109Json.status, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-211-correct-structure-for-warning-log",
            HttpResponse(
              status = Err211JsonTransf.status,
              body = Err211JsonTransf.ToTransform.exampleBody,
              headers = Map()
            )
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-211-wrong-structure",
            HttpResponse(status = Err211JsonTransf.status, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-211-not-json",
            HttpResponse(status = Err211JsonTransf.status, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-277-correct-structure-for-warning-log",
            HttpResponse(status = Err277NoBody.status, body = Err277NoBody.Singleton.exampleBody, headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-277-wrong-structure",
            HttpResponse(status = Err277NoBody.status, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-277-not-json",
            HttpResponse(status = Err277NoBody.status, body = """some body that's not JSON""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-477-correct-structure-for-warning-log",
            HttpResponse(status = Err477NoBody.status, body = Err477NoBody.Singleton.exampleBody, headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-477-wrong-structure",
            HttpResponse(status = Err477NoBody.status, body = """{}""", headers = Map())
          )

          httpReads.read(
            "POST",
            "https://some.domain/for-failure-477-not-json",
            HttpResponse(status = Err477NoBody.status, body = """some body that's not JSON""", headers = Map())
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
            """ERROR | JSON structure is not valid in received HTTP response. Originally expected to turn response into a Right. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyInvalidJsonStructure( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-236-wrong-structure #  === #  HTTP 236 Custom Status # #  {} #  ===, #  whichEitherWasExpected = OriginallyMeantToBeRight, #  errs = List((/for236,List(JsonValidationError(List(error.path.missing),List())))) )> . Request made for received HTTP response: POST https://some.domain/for-success-236-wrong-structure . Received HTTP response status: 236. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | HTTP body is not JSON in received HTTP response. Originally expected to turn response into a Right. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotJson( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-236-not-json #  === #  HTTP 236 Custom Status # #  some body that's not JSON #  ===, #  whichEitherWasExpected = OriginallyMeantToBeRight, #  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'some': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') #   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5] )> . Request made for received HTTP response: POST https://some.domain/for-success-236-not-json . Received HTTP response status: 236. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | Body of received HTTP response is not empty. Originally expected to turn response into a Right. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-265-wrong-structure #  === #  HTTP 265 Custom Status # #  {} #  ===, #  whichEitherWasExpected = OriginallyMeantToBeRight )> . Request made for received HTTP response: POST https://some.domain/for-success-265-wrong-structure . Received HTTP response status: 265. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | Body of received HTTP response is not empty. Originally expected to turn response into a Right. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-265-not-json #  === #  HTTP 265 Custom Status # #  some body that's not JSON #  ===, #  whichEitherWasExpected = OriginallyMeantToBeRight )> . Request made for received HTTP response: POST https://some.domain/for-success-265-not-json . Received HTTP response status: 265. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | JSON structure is not valid in received HTTP response. Originally expected to turn response into a Right. Detail: Validation errors:     - For path /for419 , errors: [error.path.missing]. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyInvalidJsonStructure( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-419-wrong-structure #  === #  HTTP 419 Custom Status # #  {} #  ===, #  whichEitherWasExpected = OriginallyMeantToBeRight, #  errs = List((/for419,List(JsonValidationError(List(error.path.missing),List())))) )> . Request made for received HTTP response: POST https://some.domain/for-success-419-wrong-structure . Received HTTP response status: 419. Received HTTP response body: {}""",
            """ERROR | HTTP body is not JSON in received HTTP response. Originally expected to turn response into a Right. Detail: com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'some': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')  at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5] Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotJson( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-419-not-json #  === #  HTTP 419 Custom Status # #  some body that's not JSON #  ===, #  whichEitherWasExpected = OriginallyMeantToBeRight, #  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'some': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') #   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5] )> . Request made for received HTTP response: POST https://some.domain/for-success-419-not-json . Received HTTP response status: 419. Received HTTP response body: some body that's not JSON""",
            """ERROR | Body of received HTTP response is not empty. Originally expected to turn response into a Right. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-445-wrong-structure #  === #  HTTP 445 Custom Status # #  {} #  ===, #  whichEitherWasExpected = OriginallyMeantToBeRight )> . Request made for received HTTP response: POST https://some.domain/for-success-445-wrong-structure . Received HTTP response status: 445. Received HTTP response body: {}""",
            """ERROR | Body of received HTTP response is not empty. Originally expected to turn response into a Right. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-success-445-not-json #  === #  HTTP 445 Custom Status # #  some body that's not JSON #  ===, #  whichEitherWasExpected = OriginallyMeantToBeRight )> . Request made for received HTTP response: POST https://some.domain/for-success-445-not-json . Received HTTP response status: 445. Received HTTP response body: some body that's not JSON""",
            """WARN | Valid and expected error response was received from HTTP call. Request made for received HTTP response: POST https://some.domain/for-failure-109-correct-structure-for-warning-log . Received HTTP response status: 109. Received HTTP response body: {"for109":"example"}""",
            """ERROR | JSON structure is not valid in received HTTP response. Originally expected to turn response into a Left. Detail: Validation errors:     - For path /for109 , errors: [error.path.missing]. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyInvalidJsonStructure( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-109-wrong-structure #  === #  HTTP 109 Custom Status # #  {} #  ===, #  whichEitherWasExpected = OriginallyMeantToBeLeft, #  errs = List((/for109,List(JsonValidationError(List(error.path.missing),List())))) )> . Request made for received HTTP response: POST https://some.domain/for-failure-109-wrong-structure . Received HTTP response status: 109. Received HTTP response body: {}""",
            """ERROR | HTTP body is not JSON in received HTTP response. Originally expected to turn response into a Left. Detail: com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'some': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')  at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5] Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotJson( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-109-not-json #  === #  HTTP 109 Custom Status # #  some body that's not JSON #  ===, #  whichEitherWasExpected = OriginallyMeantToBeLeft, #  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'some': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') #   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5] )> . Request made for received HTTP response: POST https://some.domain/for-failure-109-not-json . Received HTTP response status: 109. Received HTTP response body: some body that's not JSON""",
            """WARN | Valid and expected error response was received from HTTP call. Request made for received HTTP response: POST https://some.domain/for-failure-211-correct-structure-for-warning-log . Received HTTP response status: 211. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | JSON structure is not valid in received HTTP response. Originally expected to turn response into a Left. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyInvalidJsonStructure( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-211-wrong-structure #  === #  HTTP 211 Custom Status # #  {} #  ===, #  whichEitherWasExpected = OriginallyMeantToBeLeft, #  errs = List((/for211,List(JsonValidationError(List(error.path.missing),List())))) )> . Request made for received HTTP response: POST https://some.domain/for-failure-211-wrong-structure . Received HTTP response status: 211. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | HTTP body is not JSON in received HTTP response. Originally expected to turn response into a Left. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotJson( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-211-not-json #  === #  HTTP 211 Custom Status # #  some body that's not JSON #  ===, #  whichEitherWasExpected = OriginallyMeantToBeLeft, #  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'some': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') #   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5] )> . Request made for received HTTP response: POST https://some.domain/for-failure-211-not-json . Received HTTP response status: 211. Received HTTP response body not logged for 2xx statuses.""",
            """WARN | Valid and expected error response was received from HTTP call. Request made for received HTTP response: POST https://some.domain/for-failure-277-correct-structure-for-warning-log . Received HTTP response status: 277. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | Body of received HTTP response is not empty. Originally expected to turn response into a Left. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-277-wrong-structure #  === #  HTTP 277 Custom Status # #  {} #  ===, #  whichEitherWasExpected = OriginallyMeantToBeLeft )> . Request made for received HTTP response: POST https://some.domain/for-failure-277-wrong-structure . Received HTTP response status: 277. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | Body of received HTTP response is not empty. Originally expected to turn response into a Left. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-277-not-json #  === #  HTTP 277 Custom Status # #  some body that's not JSON #  ===, #  whichEitherWasExpected = OriginallyMeantToBeLeft )> . Request made for received HTTP response: POST https://some.domain/for-failure-277-not-json . Received HTTP response status: 277. Received HTTP response body not logged for 2xx statuses.""",
            """WARN | Valid and expected error response was received from HTTP call. Request made for received HTTP response: POST https://some.domain/for-failure-477-correct-structure-for-warning-log . Received HTTP response status: 477. Received HTTP response body: """,
            """ERROR | Body of received HTTP response is not empty. Originally expected to turn response into a Left. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-477-wrong-structure #  === #  HTTP 477 Custom Status # #  {} #  ===, #  whichEitherWasExpected = OriginallyMeantToBeLeft )> . Request made for received HTTP response: POST https://some.domain/for-failure-477-wrong-structure . Received HTTP response status: 477. Received HTTP response body: {}""",
            """ERROR | Body of received HTTP response is not empty. Originally expected to turn response into a Left. Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-failure-477-not-json #  === #  HTTP 477 Custom Status # #  some body that's not JSON #  ===, #  whichEitherWasExpected = OriginallyMeantToBeLeft )> . Request made for received HTTP response: POST https://some.domain/for-failure-477-not-json . Received HTTP response status: 477. Received HTTP response body: some body that's not JSON""",
            """ERROR | HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left. Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-299-unexpected-success #  === #  HTTP 299 Custom Status # #  some irrelevant body because status is unexpected #  === )> . Request made for received HTTP response: POST https://some.domain/for-299-unexpected-success . Received HTTP response status: 299. Received HTTP response body not logged for 2xx statuses.""",
            """ERROR | HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left. Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode( #  sourceClass = class java.lang.Thread, #  responseContext = RESPONSE TO: POST https://some.domain/for-404-unexpected-failure #  === #  HTTP 404 Not Found # #  some irrelevant body because status is unexpected #  === )> . Request made for received HTTP response: POST https://some.domain/for-404-unexpected-failure . Received HTTP response status: 404. Received HTTP response body: some irrelevant body because status is unexpected"""
          )
        }
      }

      "when given the successful statuses" - {
        import TestDataForCombinations.{ Succ236Json, Succ265NoBody, Succ419Json, Succ445NoBody }

        "and a valid body" - {
          for {
            (successfulStatus, exampleValue, exampleBody) <- List(
                                                               (
                                                                 Succ236Json.status,
                                                                 Succ236Json.Wrapper.exampleValue,
                                                                 Succ236Json.Wrapper.exampleBody
                                                               ),
                                                               (
                                                                 Succ265NoBody.status,
                                                                 Succ265NoBody.Singleton,
                                                                 Succ265NoBody.Singleton.exampleBody
                                                               ),
                                                               (
                                                                 Succ419Json.status,
                                                                 Succ419Json.Wrapper.exampleValue,
                                                                 Succ419Json.Wrapper.exampleBody
                                                               ),
                                                               (
                                                                 Succ445NoBody.status,
                                                                 Succ445NoBody.Singleton,
                                                                 Succ445NoBody.Singleton.exampleBody
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
          s"<${Succ236Json.status}>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse = HttpResponse(status = Succ236Json.status, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyInvalidJsonStructure(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 236 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                  |#  errs = List((/for236,List(JsonValidationError(List(error.path.missing),List()))))
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""JSON structure is not valid in received HTTP response. Originally expected to turn response into a Right.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyInvalidJsonStructure(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 236 Custom Status
                     |#
                     |#  {}
                     |#  ===,
                     |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                     |#  errs = List((/for236,List(JsonValidationError(List(error.path.missing),List()))))
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 236.
                     |Received HTTP response body not logged for 2xx statuses.""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse = HttpResponse(status = Succ236Json.status, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyInvalidJsonStructure(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 236 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                  |#  errs = List((/for236,List(JsonValidationError(List(error.path.missing),List()))))
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<${Succ265NoBody.status}>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse =
                HttpResponse(status = Succ265NoBody.status, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyNotEmpty(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 265 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""Body of received HTTP response is not empty. Originally expected to turn response into a Right.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 265 Custom Status
                     |#
                     |#  {}
                     |#  ===,
                     |#  whichEitherWasExpected = OriginallyMeantToBeRight
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 265.
                     |Received HTTP response body not logged for 2xx statuses.""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse =
                HttpResponse(status = Succ265NoBody.status, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyNotEmpty(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 265 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<${Succ419Json.status}>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse = HttpResponse(status = Succ419Json.status, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyInvalidJsonStructure(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 419 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                  |#  errs = List((/for419,List(JsonValidationError(List(error.path.missing),List()))))
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""JSON structure is not valid in received HTTP response. Originally expected to turn response into a Right.
                     |Detail: Validation errors:
                     |    - For path /for419 , errors: [error.path.missing].
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyInvalidJsonStructure(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 419 Custom Status
                     |#
                     |#  {}
                     |#  ===,
                     |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                     |#  errs = List((/for419,List(JsonValidationError(List(error.path.missing),List()))))
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 419.
                     |Received HTTP response body: {}""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse = HttpResponse(status = Succ419Json.status, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyInvalidJsonStructure(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 419 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                  |#  errs = List((/for419,List(JsonValidationError(List(error.path.missing),List()))))
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<${Succ445NoBody.status}>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse =
                HttpResponse(status = Succ445NoBody.status, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyNotEmpty(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 445 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""Body of received HTTP response is not empty. Originally expected to turn response into a Right.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 445 Custom Status
                     |#
                     |#  {}
                     |#  ===,
                     |#  whichEitherWasExpected = OriginallyMeantToBeRight
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 445.
                     |Received HTTP response body: {}""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse =
                HttpResponse(status = Succ445NoBody.status, body = """{}""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyNotEmpty(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 445 Custom Status
                  |#
                  |#  {}
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }
        }

        "and an unparsable text body" - {
          s"<${Succ236Json.status}>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse = HttpResponse(status = Succ236Json.status, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyNotJson(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 236 Custom Status
                  |#
                  |#  TEXT
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                  |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                  |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""HTTP body is not JSON in received HTTP response. Originally expected to turn response into a Right.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotJson(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 236 Custom Status
                     |#
                     |#  TEXT
                     |#  ===,
                     |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                     |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                     |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 236.
                     |Received HTTP response body not logged for 2xx statuses.""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse = HttpResponse(status = Succ236Json.status, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyNotJson(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 236 Custom Status
                  |#
                  |#  TEXT
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                  |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                  |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<${Succ265NoBody.status}>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse =
                HttpResponse(status = Succ265NoBody.status, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyNotEmpty(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 265 Custom Status
                  |#
                  |#  TEXT
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""Body of received HTTP response is not empty. Originally expected to turn response into a Right.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 265 Custom Status
                     |#
                     |#  TEXT
                     |#  ===,
                     |#  whichEitherWasExpected = OriginallyMeantToBeRight
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 265.
                     |Received HTTP response body not logged for 2xx statuses.""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse =
                HttpResponse(status = Succ265NoBody.status, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                s"""ResponseBodyNotEmpty(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 265 Custom Status
                   |#
                   |#  TEXT
                   |#  ===,
                   |#  whichEitherWasExpected = OriginallyMeantToBeRight
                   |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<${Succ419Json.status}>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse = HttpResponse(status = Succ419Json.status, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyNotJson(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 419 Custom Status
                  |#
                  |#  TEXT
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                  |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                  |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""HTTP body is not JSON in received HTTP response. Originally expected to turn response into a Right.
                     |Detail: com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                     | at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotJson(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 419 Custom Status
                     |#
                     |#  TEXT
                     |#  ===,
                     |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                     |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                     |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 419.
                     |Received HTTP response body: TEXT""".stripMargin
              )
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse = HttpResponse(status = Succ419Json.status, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyNotJson(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 419 Custom Status
                  |#
                  |#  TEXT
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight,
                  |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                  |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }

          s"<${Succ445NoBody.status}>" - {
            "then .httpReads" in new TestFixtureForHttpReads {
              val response: HttpResponse =
                HttpResponse(status = Succ445NoBody.status, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyNotEmpty(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 445 Custom Status
                  |#
                  |#  TEXT
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe List(
                "ERROR" ->
                  s"""Body of received HTTP response is not empty. Originally expected to turn response into a Right.
                     |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP 445 Custom Status
                     |#
                     |#  TEXT
                     |#  ===,
                     |#  whichEitherWasExpected = OriginallyMeantToBeRight
                     |)> .
                     |Request made for received HTTP response: MYMETHOD some/url .
                     |Received HTTP response status: 445.
                     |Received HTTP response body: TEXT""".stripMargin
              )

              allCapturedLogs.toString should include("TEXT") // This success is not a 2xx, so it's safe to log.
            }

            "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
              val response: HttpResponse =
                HttpResponse(status = Succ445NoBody.status, body = """TEXT""", headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                """ResponseBodyNotEmpty(
                  |#  sourceClass = class java.lang.Thread,
                  |#  responseContext = RESPONSE TO: MYMETHOD some/url
                  |#  ===
                  |#  HTTP 445 Custom Status
                  |#
                  |#  TEXT
                  |#  ===,
                  |#  whichEitherWasExpected = OriginallyMeantToBeRight
                  |)""".stripMargin
              )

              allCapturedLogs shouldBe Nil
            }
          }
        }
      }

      "when given the error non-transformed status (which is not 2xx)" - {
        import TestDataForCombinations.{ Err109Json, SuccessWrapper }

        "and a valid body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse =
              HttpResponse(status = Err109Json.status, body = Err109Json.Wrapper.exampleBody, headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """PassthroughServiceError(
                |#  error = Err109Json$.Wrapper(for109=example)(toString from WithClassChainProductToString)
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "WARN" ->
                s"""Valid and expected error response was received from HTTP call.
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 109.
                   |Received HTTP response body: ${Err109Json.Wrapper.exampleBody: String}""".stripMargin
            )
          }
        }

        "and an empty JSON body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse = HttpResponse(status = Err109Json.status, body = """{}""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """ResponseBodyInvalidJsonStructure(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 109 Custom Status
                |#
                |#  {}
                |#  ===,
                |#  whichEitherWasExpected = OriginallyMeantToBeLeft,
                |#  errs = List((/for109,List(JsonValidationError(List(error.path.missing),List()))))
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""JSON structure is not valid in received HTTP response. Originally expected to turn response into a Left.
                   |Detail: Validation errors:
                   |    - For path /for109 , errors: [error.path.missing].
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyInvalidJsonStructure(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 109 Custom Status
                   |#
                   |#  {}
                   |#  ===,
                   |#  whichEitherWasExpected = OriginallyMeantToBeLeft,
                   |#  errs = List((/for109,List(JsonValidationError(List(error.path.missing),List()))))
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 109.
                   |Received HTTP response body: {}""".stripMargin
            )
          }
        }

        "and an unparsable text body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse = HttpResponse(status = Err109Json.status, body = """TEXT""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """ResponseBodyNotJson(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 109 Custom Status
                |#
                |#  TEXT
                |#  ===,
                |#  whichEitherWasExpected = OriginallyMeantToBeLeft,
                |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""HTTP body is not JSON in received HTTP response. Originally expected to turn response into a Left.
                   |Detail: com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                   | at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotJson(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 109 Custom Status
                   |#
                   |#  TEXT
                   |#  ===,
                   |#  whichEitherWasExpected = OriginallyMeantToBeLeft,
                   |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                   |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 109.
                   |Received HTTP response body: TEXT""".stripMargin
            )
          }

        }
      }

      "when given the error transformed status (which is 2xx)" - {
        import TestDataForCombinations.{ Err211JsonTransf, SuccessWrapper }

        "and a valid body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse =
              HttpResponse(
                status = Err211JsonTransf.status,
                body = Err211JsonTransf.ToTransform.exampleBody,
                headers = Map()
              )

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """PassthroughServiceError(
                |#  error = Err211JsonTransf$.Transformed(for211=example)(toString from WithClassChainProductToString)
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
              HttpResponse(status = Err211JsonTransf.status, body = """{}""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """ResponseBodyInvalidJsonStructure(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 211 Custom Status
                |#
                |#  {}
                |#  ===,
                |#  whichEitherWasExpected = OriginallyMeantToBeLeft,
                |#  errs = List((/for211,List(JsonValidationError(List(error.path.missing),List()))))
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""JSON structure is not valid in received HTTP response. Originally expected to turn response into a Left.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyInvalidJsonStructure(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 211 Custom Status
                   |#
                   |#  {}
                   |#  ===,
                   |#  whichEitherWasExpected = OriginallyMeantToBeLeft,
                   |#  errs = List((/for211,List(JsonValidationError(List(error.path.missing),List()))))
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
              HttpResponse(status = Err211JsonTransf.status, body = """TEXT""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              s"""ResponseBodyNotJson(
                 |#  sourceClass = class java.lang.Thread,
                 |#  responseContext = RESPONSE TO: MYMETHOD some/url
                 |#  ===
                 |#  HTTP 211 Custom Status
                 |#
                 |#  TEXT
                 |#  ===,
                 |#  whichEitherWasExpected = OriginallyMeantToBeLeft,
                 |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                 |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                 |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""HTTP body is not JSON in received HTTP response. Originally expected to turn response into a Left.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotJson(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 211 Custom Status
                   |#
                   |#  TEXT
                   |#  ===,
                   |#  whichEitherWasExpected = OriginallyMeantToBeLeft,
                   |#  sensitiveException = com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'TEXT': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
                   |#   at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 211.
                   |Received HTTP response body not logged for 2xx statuses.""".stripMargin
            )
          }
        }
      }

      "when given the no-body error 2xx status" - {
        import TestDataForCombinations.{ Err277NoBody, SuccessWrapper }

        "and a valid body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse =
              HttpResponse(status = Err277NoBody.status, body = Err277NoBody.Singleton.exampleBody, headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """PassthroughServiceError(
                |#  error = Err277NoBody$.Singleton$()(toString from WithClassChainProductToString)
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
              HttpResponse(status = Err277NoBody.status, body = """{}""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """ResponseBodyNotEmpty(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 277 Custom Status
                |#
                |#  {}
                |#  ===,
                |#  whichEitherWasExpected = OriginallyMeantToBeLeft
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""Body of received HTTP response is not empty. Originally expected to turn response into a Left.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 277 Custom Status
                   |#
                   |#  {}
                   |#  ===,
                   |#  whichEitherWasExpected = OriginallyMeantToBeLeft
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
              HttpResponse(status = Err277NoBody.status, body = """TEXT""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """ResponseBodyNotEmpty(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 277 Custom Status
                |#
                |#  TEXT
                |#  ===,
                |#  whichEitherWasExpected = OriginallyMeantToBeLeft
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""Body of received HTTP response is not empty. Originally expected to turn response into a Left.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 277 Custom Status
                   |#
                   |#  TEXT
                   |#  ===,
                   |#  whichEitherWasExpected = OriginallyMeantToBeLeft
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 277.
                   |Received HTTP response body not logged for 2xx statuses.""".stripMargin
            )
          }
        }
      }

      "when given the no-body error non-2xx status" - {
        import TestDataForCombinations.{ Err477NoBody, SuccessWrapper }

        "and a valid body" - {
          "then .httpReads" in new TestFixtureForHttpReads {

            val response: HttpResponse =
              HttpResponse(status = Err477NoBody.status, body = Err477NoBody.Singleton.exampleBody, headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """PassthroughServiceError(
                |#  error = Err477NoBody$.Singleton$()(toString from WithClassChainProductToString)
                |)""".stripMargin
            )
            allCapturedLogs shouldBe List(
              "WARN" ->
                s"""Valid and expected error response was received from HTTP call.
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 477.
                   |Received HTTP response body: ${Err477NoBody.Singleton.exampleBody: String}""".stripMargin
            )
          }
        }

        "and an empty JSON body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse = HttpResponse(status = Err477NoBody.status, body = """{}""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """ResponseBodyNotEmpty(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 477 Custom Status
                |#
                |#  {}
                |#  ===,
                |#  whichEitherWasExpected = OriginallyMeantToBeLeft
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""Body of received HTTP response is not empty. Originally expected to turn response into a Left.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 477 Custom Status
                   |#
                   |#  {}
                   |#  ===,
                   |#  whichEitherWasExpected = OriginallyMeantToBeLeft
                   |)> .
                   |Request made for received HTTP response: MYMETHOD some/url .
                   |Received HTTP response status: 477.
                   |Received HTTP response body: {}""".stripMargin
            )
          }
        }

        "and an unparsable text body" - {
          "then .httpReads" in new TestFixtureForHttpReads {
            val response: HttpResponse = HttpResponse(status = Err477NoBody.status, body = """TEXT""", headers = Map())

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              """ResponseBodyNotEmpty(
                |#  sourceClass = class java.lang.Thread,
                |#  responseContext = RESPONSE TO: MYMETHOD some/url
                |#  ===
                |#  HTTP 477 Custom Status
                |#
                |#  TEXT
                |#  ===,
                |#  whichEitherWasExpected = OriginallyMeantToBeLeft
                |)""".stripMargin
            )

            allCapturedLogs shouldBe List(
              "ERROR" ->
                s"""Body of received HTTP response is not empty. Originally expected to turn response into a Left.
                   |Returning: [DEEMED SAFE BY TEST LOGIC] <ResponseBodyNotEmpty(
                   |#  sourceClass = class java.lang.Thread,
                   |#  responseContext = RESPONSE TO: MYMETHOD some/url
                   |#  ===
                   |#  HTTP 477 Custom Status
                   |#
                   |#  TEXT
                   |#  ===,
                   |#  whichEitherWasExpected = OriginallyMeantToBeLeft
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
          import TestDataForCombinations.Succ236Json

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = Succ236Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for236":true}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  {"for236":true}
                       |#  ===
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpected2xxStatus: Int}.
                       |Received HTTP response body not logged for 2xx statuses.""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = Succ236Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for236":true}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the second successful JSON body" - {
          import TestDataForCombinations.Succ419Json

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = Succ419Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for419":true}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  {"for419":true}
                       |#  ===
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpected2xxStatus: Int}.
                       |Received HTTP response body not logged for 2xx statuses.""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = Succ419Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for419":true}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the first error JSON body" - {
          import TestDataForCombinations.Err109Json

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = Err109Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for109":"example"}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  {"for109":"example"}
                       |#  ===
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpected2xxStatus: Int}.
                       |Received HTTP response body not logged for 2xx statuses.""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpected2xxStatus, body = Err109Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for109":"example"}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the second error JSON body" - {
          import TestDataForCombinations.Err211JsonTransf

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(
                    status = unexpected2xxStatus,
                    body = Err211JsonTransf.ToTransform.exampleBody,
                    headers = Map()
                  )

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for211":"example"}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  {"for211":"example"}
                       |#  ===
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpected2xxStatus: Int}.
                       |Received HTTP response body not logged for 2xx statuses.""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(
                    status = unexpected2xxStatus,
                    body = Err211JsonTransf.ToTransform.exampleBody,
                    headers = Map()
                  )

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {"for211":"example"}
                     |#  ===
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
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  {}
                       |#  ===
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
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  {}
                     |#  ===
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
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  SOMETEXT
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpected2xxStatus): String}
                       |#
                       |#  SOMETEXT
                       |#  ===
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
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpected2xxStatus): String}
                     |#
                     |#  SOMETEXT
                     |#  ===
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
          import TestDataForCombinations.Succ236Json

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = Succ236Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for236":true}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  {"for236":true}
                       |#  ===
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpectedNon2xxStatus: Int}.
                       |Received HTTP response body: ${Succ236Json.Wrapper.exampleBody: String}""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = Succ236Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for236":true}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the second successful JSON body" - {
          import TestDataForCombinations.Succ419Json

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = Succ419Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for419":true}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  {"for419":true}
                       |#  ===
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpectedNon2xxStatus: Int}.
                       |Received HTTP response body: ${Succ419Json.Wrapper.exampleBody: String}""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = Succ419Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for419":true}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the first error JSON body" - {
          import TestDataForCombinations.Err109Json

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = Err109Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for109":"example"}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  {"for109":"example"}
                       |#  ===
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpectedNon2xxStatus: Int}.
                       |Received HTTP response body: ${Err109Json.Wrapper.exampleBody: String}""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(status = unexpectedNon2xxStatus, body = Err109Json.Wrapper.exampleBody, headers = Map())

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for109":"example"}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe Nil
              }
            }
        }

        "when given the second error JSON body" - {
          import TestDataForCombinations.Err211JsonTransf

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" - {
              "then .httpReads" in new TestFixtureForHttpReads {
                val response: HttpResponse =
                  HttpResponse(
                    status = unexpectedNon2xxStatus,
                    body = Err211JsonTransf.ToTransform.exampleBody,
                    headers = Map()
                  )

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for211":"example"}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  {"for211":"example"}
                       |#  ===
                       |)> .
                       |Request made for received HTTP response: MYMETHOD some/url .
                       |Received HTTP response status: ${unexpectedNon2xxStatus: Int}.
                       |Received HTTP response body: ${Err211JsonTransf.ToTransform.exampleBody: String}""".stripMargin
                )
              }

              "then .httpReadsNoLogging" in new TestFixtureForHttpReadsNoLogging {
                val response: HttpResponse =
                  HttpResponse(
                    status = unexpectedNon2xxStatus,
                    body = Err211JsonTransf.ToTransform.exampleBody,
                    headers = Map()
                  )

                val result: Either[AnyRef, SuccessWrapper] =
                  httpReads.read("MYMETHOD", "some/url", response)

                result shouldBe Left(
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {"for211":"example"}
                     |#  ===
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
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {}
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  {}
                       |#  ===
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
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  {}
                     |#  ===
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
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  SOMETEXT
                     |#  ===
                     |)""".stripMargin
                )

                allCapturedLogs shouldBe List(
                  "ERROR" ->
                    s"""HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left.
                       |Returning: [DEEMED SAFE BY TEST LOGIC] <UnexpectedStatusCode(
                       |#  sourceClass = class java.lang.Thread,
                       |#  responseContext = RESPONSE TO: MYMETHOD some/url
                       |#  ===
                       |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                       |#
                       |#  SOMETEXT
                       |#  ===
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
                  s"""UnexpectedStatusCode(
                     |#  sourceClass = class java.lang.Thread,
                     |#  responseContext = RESPONSE TO: MYMETHOD some/url
                     |#  ===
                     |#  HTTP ${statusString(unexpectedNon2xxStatus): String}
                     |#
                     |#  SOMETEXT
                     |#  ===
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
