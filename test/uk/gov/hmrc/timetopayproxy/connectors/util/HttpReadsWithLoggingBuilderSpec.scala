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

import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import org.slf4j.{ Logger => Slf4jLogger }
import play.api.Logger
import play.api.libs.json.{ Json, OFormat, OWrites }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, HttpResponse }
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger
import uk.gov.hmrc.timetopayproxy.models.error.ConnectorError
import uk.gov.hmrc.timetopayproxy.models.{ TimeToPayEligibilityError, TimeToPayError, TimeToPayInnerError }

final class HttpReadsWithLoggingBuilderSpec extends AnyFreeSpec with MockFactory {
  "HttpReadsWithLoggingBuilder" - {
    implicit val hc: HeaderCarrier = new HeaderCarrier()

    object TestData {
      def emptyTextBody: String = ""
      def emptyJsonBody: String = "{}"

      object ForApply {
        implicit def writerForTttpInner: OWrites[TimeToPayInnerError] = Json.writes[TimeToPayInnerError]
        implicit def writerForTtpOuter: OWrites[TimeToPayError] = Json.writes[TimeToPayError]
        implicit def writerForTtpeError: OWrites[TimeToPayEligibilityError] = Json.writes[TimeToPayEligibilityError]

        def validTtpErrorBody: String = Json
          .toJson(
            TimeToPayError(
              Seq(
                TimeToPayInnerError(code = "400", reason = "my-reason")
              )
            )
          )
          .toString

        def validTtpEligibilityErrorBody: String = Json
          .toJson(
            TimeToPayEligibilityError(
              code = "400",
              reason = "my-reason"
            )
          )
          .toString

        final case class SuccessWrapper(value: Boolean)
        object SuccessWrapper {
          implicit def format: OFormat[SuccessWrapper] = Json.format[SuccessWrapper]

          def exampleBody: String = Json.toJson(SuccessWrapper(true)).toString
        }
      }

      object TestDataForCombinations {

        val successStatus236: Int = 236
        val successStatus419: Int = 419
        val errorStatus109: Int = 109
        val errorStatus211Transf: Int = 211

        val expectedStatuses: Seq[Int] =
          Seq(successStatus236, successStatus419, errorStatus109, errorStatus211Transf)
        val unexpectedStatuses2xx: Seq[Int] = (200 to 299) diff expectedStatuses
        val unexpectedStatusesNon2xx: Seq[Int] = (100 to 199) ++ (300 to 599) diff expectedStatuses

        sealed trait SuccessWrapper

        final case class SuccessWrapper236(for236: Boolean) extends SuccessWrapper
        object SuccessWrapper236 {
          def exampleValue: SuccessWrapper236 = SuccessWrapper236(true)
          def exampleBody: String = Json.toJson(exampleValue).toString

          implicit def format: OFormat[SuccessWrapper236] = Json.format[SuccessWrapper236]
        }

        final case class SuccessWrapper419(for419: Boolean) extends SuccessWrapper
        object SuccessWrapper419 {
          def exampleValue: SuccessWrapper419 = SuccessWrapper419(true)
          def exampleBody: String = Json.toJson(exampleValue).toString

          implicit def format: OFormat[SuccessWrapper419] = Json.format[SuccessWrapper419]
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

        final case class ErrorFor211Transformed(for211: String)

        // AnyRef is a common supertype for ConnectorError and our errors.
        def makeHttpReadsBuilder(
          obj: HttpReadsWithLoggingBuilder[AnyRef, SuccessWrapper]
        ): HttpReadsWithLoggingBuilder[AnyRef, SuccessWrapper] =
          obj
            .orSuccess[SuccessWrapper236](incomingStatus = successStatus236)
            .orSuccess[SuccessWrapper419](incomingStatus = successStatus419)
            .orError[ErrorFor109](incomingStatus = errorStatus109)
            .orErrorTransformed[ErrorFor211ToTransform](
              errorStatus211Transf,
              _.myTransform
            )
      }
    }

    sealed trait TestFixture[Err >: ConnectorError, Result] {
      private val underlyingLogger: Slf4jLogger = mock[Slf4jLogger]

      protected val logger: RequestAwareLogger = new RequestAwareLogger(new Logger(underlyingLogger))

      var savedErrorLogs: List[String] = Nil
      def expectOneErrorLogAndStoreInVar(): Unit = {
        (() => underlyingLogger.isErrorEnabled).expects().once().returning(true)
        (underlyingLogger.error(_: String)).expects(*).once().onCall { (msg: String) =>
          savedErrorLogs = savedErrorLogs :+ msg
        }
      }

      val obj: HttpReadsWithLoggingBuilder[Err, Result] = HttpReadsWithLoggingBuilder[Err, Result]
    }

    ".apply then .httpReads" - {
      "when given a normal logger and header carrier" - {
        "when reading a 200 OK" - {
          "with an empty body" in new TestFixture[ConnectorError, Nothing] {
            val httpReads: HttpReads[Either[ConnectorError, Nothing]] = obj.httpReads(logger)(hc)
            val response: HttpResponse = HttpResponse(status = 200, body = TestData.emptyTextBody, headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[ConnectorError, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              ConnectorError(
                statusCode = 200,
                message = """Upstream response status is unexpected."""
              )
            )

            savedErrorLogs shouldBe List(
              """Upstream response status is unexpected.
                |Response status being returned: 200
                |Request made: MYMETHOD some/url
                |Response status received: 200
                |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
            )
          }

          "with an empty JSON body" in new TestFixture[ConnectorError, Nothing] {
            val httpReads: HttpReads[Either[ConnectorError, Nothing]] = obj.httpReads(logger)(hc)
            val response: HttpResponse = HttpResponse(status = 200, body = TestData.emptyJsonBody, headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[ConnectorError, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              ConnectorError(
                statusCode = 200,
                message = """Upstream response status is unexpected."""
              )
            )

            savedErrorLogs shouldBe List(
              """Upstream response status is unexpected.
                |Response status being returned: 200
                |Request made: MYMETHOD some/url
                |Response status received: 200
                |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
            )
          }

          "with a properly formatted TTP error JSON body" in new TestFixture[ConnectorError, Nothing] {
            val httpReads: HttpReads[Either[ConnectorError, Nothing]] = obj.httpReads(logger)(hc)
            val response: HttpResponse =
              HttpResponse(status = 200, body = TestData.ForApply.validTtpErrorBody, headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[ConnectorError, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              ConnectorError(
                statusCode = 200,
                message = """Upstream response status is unexpected."""
              )
            )

            savedErrorLogs shouldBe List(
              """Upstream response status is unexpected.
                |Response status being returned: 200
                |Request made: MYMETHOD some/url
                |Response status received: 200
                |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
            )
          }

          "with a properly formatted TTP-E error JSON body" in new TestFixture[ConnectorError, Nothing] {
            val httpReads: HttpReads[Either[ConnectorError, Nothing]] = obj.httpReads(logger)(hc)
            val response: HttpResponse =
              HttpResponse(status = 200, body = TestData.ForApply.validTtpEligibilityErrorBody, headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[ConnectorError, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              ConnectorError(
                statusCode = 200,
                message = """Upstream response status is unexpected."""
              )
            )

            savedErrorLogs shouldBe List(
              """Upstream response status is unexpected.
                |Response status being returned: 200
                |Request made: MYMETHOD some/url
                |Response status received: 200
                |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
            )
          }
        }

        "when reading a 400 BAD REQUEST" - {
          "with an empty body" in new TestFixture[ConnectorError, Nothing] {
            val httpReads: HttpReads[Either[ConnectorError, Nothing]] = obj.httpReads(logger)(hc)
            val response: HttpResponse = HttpResponse(status = 400, body = TestData.emptyTextBody, headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[ConnectorError, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              ConnectorError(
                statusCode = 400,
                message = """Upstream response status is unexpected."""
              )
            )

            savedErrorLogs shouldBe List(
              """Upstream response status is unexpected.
                |Response status being returned: 400
                |Request made: MYMETHOD some/url
                |Response status received: 400
                |Incoming HTTP response body: """.stripMargin
            )
          }

          "with an empty JSON body" in new TestFixture[ConnectorError, Nothing] {
            val httpReads: HttpReads[Either[ConnectorError, Nothing]] = obj.httpReads(logger)(hc)
            val response: HttpResponse = HttpResponse(status = 400, body = TestData.emptyJsonBody, headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[ConnectorError, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              ConnectorError(
                statusCode = 400,
                message = """Upstream response status is unexpected."""
              )
            )

            savedErrorLogs shouldBe List(
              """Upstream response status is unexpected.
                |Response status being returned: 400
                |Request made: MYMETHOD some/url
                |Response status received: 400
                |Incoming HTTP response body: {}""".stripMargin
            )
          }

          "with a properly formatted TTP error JSON body" in new TestFixture[ConnectorError, Nothing] {
            val httpReads: HttpReads[Either[ConnectorError, Nothing]] = obj.httpReads(logger)(hc)
            val response: HttpResponse =
              HttpResponse(status = 400, body = TestData.ForApply.validTtpErrorBody, headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[ConnectorError, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              ConnectorError(
                statusCode = 400,
                message = """Upstream response status is unexpected."""
              )
            )

            savedErrorLogs shouldBe List(
              """Upstream response status is unexpected.
                |Response status being returned: 400
                |Request made: MYMETHOD some/url
                |Response status received: 400
                |Incoming HTTP response body: {"failures":[{"code":"400","reason":"my-reason"}]}""".stripMargin
            )
          }

          "with a properly formatted TTP-E error JSON body" in new TestFixture[ConnectorError, Nothing] {
            val httpReads: HttpReads[Either[ConnectorError, Nothing]] = obj.httpReads(logger)(hc)
            val response: HttpResponse =
              HttpResponse(status = 400, body = TestData.ForApply.validTtpEligibilityErrorBody, headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[ConnectorError, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              ConnectorError(
                statusCode = 400,
                message = """Upstream response status is unexpected."""
              )
            )

            savedErrorLogs shouldBe List(
              """Upstream response status is unexpected.
                |Response status being returned: 400
                |Request made: MYMETHOD some/url
                |Response status received: 400
                |Incoming HTTP response body: {"code":"400","reason":"my-reason"}""".stripMargin
            )
          }
        }

        "when reading a 299 status with an empty JSON body, still doesn't log the body" in
          new TestFixture[ConnectorError, Nothing] {
            val httpReads: HttpReads[Either[ConnectorError, Nothing]] = obj.httpReads(logger)(hc)
            val response: HttpResponse =
              HttpResponse(status = 299, body = TestData.emptyJsonBody, headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[ConnectorError, Nothing] = httpReads.read("MYMETHOD", "some/url", response)
            result shouldBe Left(
              ConnectorError(
                statusCode = 299,
                message = """Upstream response status is unexpected."""
              )
            )

            savedErrorLogs shouldBe List(
              """Upstream response status is unexpected.
                |Response status being returned: 299
                |Request made: MYMETHOD some/url
                |Response status received: 299
                |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
            )
          }
      }
    }

    ".orSuccess + .orSuccess + .orError + .orErrorTransformed + .httpReads" - {
      // We're running the statuses in combinations because this would be many thousands more unit tests otherwise.
      // We'll still go with atypical values, to prove that no special handling for 200 and 201 exists.

      "(check the test data)" in {
        import TestData.TestDataForCombinations._

        unexpectedStatuses2xx should have size 98
        unexpectedStatusesNon2xx should have size 398
      }

      "when given the successful statuses" - {
        import TestData.TestDataForCombinations.{ SuccessWrapper, SuccessWrapper236, SuccessWrapper419, makeHttpReadsBuilder, successStatus236, successStatus419 }

        "and a valid body" - {
          for {
            (successfulStatus, exampleValue, exampleBody) <-
              List(
                (successStatus236, SuccessWrapper236.exampleValue, SuccessWrapper236.exampleBody),
                (successStatus419, SuccessWrapper419.exampleValue, SuccessWrapper419.exampleBody)
              )
          }
            s"<$successfulStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse = HttpResponse(status = successfulStatus, body = exampleBody, headers = Map())

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Right(exampleValue)
              savedErrorLogs shouldBe Nil
            }
        }

        "and an empty JSON body" - {
          s"<$successStatus236>" in new TestFixture[AnyRef, SuccessWrapper] {
            val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)

            val response: HttpResponse = HttpResponse(status = successStatus236, body = """{}""", headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              ConnectorError(
                statusCode = 503,
                message = """JSON structure is not valid in successful upstream response."""
              )
            )

            savedErrorLogs shouldBe List(
              s"""JSON structure is not valid in successful upstream response.
                 |Response status being returned: 503
                 |Request made: MYMETHOD some/url
                 |Response status received: ${successStatus236: Int}
                 |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
            )
          }

          s"<$successStatus419>" in new TestFixture[AnyRef, SuccessWrapper] {
            val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)

            val response: HttpResponse = HttpResponse(status = successStatus419, body = """{}""", headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              ConnectorError(
                statusCode = 503,
                message = """JSON structure is not valid in successful upstream response."""
              )
            )

            savedErrorLogs shouldBe List(
              s"""JSON structure is not valid in successful upstream response.
                 |Response status being returned: 503
                 |Request made: MYMETHOD some/url
                 |Response status received: ${successStatus419: Int}
                 |Incoming HTTP response body: {}""".stripMargin
            )
          }
        }

        "and an unparsable text body" - {
          s"<$successStatus236>" in new TestFixture[AnyRef, SuccessWrapper] {
            val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)

            val response: HttpResponse = HttpResponse(status = successStatus236, body = """TEXT""", headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              ConnectorError(
                statusCode = 503,
                message = """Successful upstream response body is not JSON."""
              )
            )

            savedErrorLogs shouldBe List(
              s"""Successful upstream response body is not JSON.
                 |Response status being returned: 503
                 |Request made: MYMETHOD some/url
                 |Response status received: ${successStatus236: Int}
                 |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
            )

            savedErrorLogs.toString should not include "TEXT"
          }

          s"<$successStatus419>" in new TestFixture[AnyRef, SuccessWrapper] {
            val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)

            val response: HttpResponse = HttpResponse(status = successStatus419, body = """TEXT""", headers = Map())

            expectOneErrorLogAndStoreInVar()

            val result: Either[AnyRef, SuccessWrapper] =
              httpReads.read("MYMETHOD", "some/url", response)

            result shouldBe Left(
              ConnectorError(
                statusCode = 503,
                message = """Successful upstream response body is not JSON."""
              )
            )

            savedErrorLogs shouldBe List(
              s"""Successful upstream response body is not JSON.
                 |Response status being returned: 503
                 |Request made: MYMETHOD some/url
                 |Response status received: ${successStatus419: Int}
                 |Incoming HTTP response body: TEXT""".stripMargin
            )
          }
        }
      }

      "when given the error non-transformed status" - {
        import TestData.TestDataForCombinations.{ ErrorFor109, SuccessWrapper, errorStatus109, makeHttpReadsBuilder }

        "and a valid body" in new TestFixture[AnyRef, SuccessWrapper] {
          def exampleValue: ErrorFor109 = ErrorFor109.exampleValue
          def exampleBody: String = ErrorFor109.exampleBody

          val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)

          val response: HttpResponse = HttpResponse(status = errorStatus109, body = exampleBody, headers = Map())

          val result: Either[AnyRef, SuccessWrapper] =
            httpReads.read("MYMETHOD", "some/url", response)

          result shouldBe Left(exampleValue)
          savedErrorLogs shouldBe Nil
        }

        "and an empty JSON body" in new TestFixture[AnyRef, SuccessWrapper] {
          val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)

          val response: HttpResponse = HttpResponse(status = errorStatus109, body = """{}""", headers = Map())

          expectOneErrorLogAndStoreInVar()

          val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

          result shouldBe Left(
            ConnectorError(
              statusCode = 503,
              message = """JSON structure is not valid in error upstream response."""
            )
          )

          savedErrorLogs shouldBe List(
            s"""JSON structure is not valid in error upstream response.
               |Response status being returned: 503
               |Request made: MYMETHOD some/url
               |Response status received: ${errorStatus109: Int}
               |Incoming HTTP response body: {}""".stripMargin
          )
        }

        "and an unparsable text body" in new TestFixture[AnyRef, SuccessWrapper] {
          val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)

          val response: HttpResponse = HttpResponse(status = errorStatus109, body = """TEXT""", headers = Map())

          expectOneErrorLogAndStoreInVar()

          val result: Either[AnyRef, SuccessWrapper] =
            httpReads.read("MYMETHOD", "some/url", response)

          result shouldBe Left(
            ConnectorError(
              statusCode = 503,
              message = """Error upstream response body is not JSON."""
            )
          )

          savedErrorLogs shouldBe List(
            s"""Error upstream response body is not JSON.
               |Response status being returned: 503
               |Request made: MYMETHOD some/url
               |Response status received: ${errorStatus109: Int}
               |Incoming HTTP response body: TEXT""".stripMargin
          )
        }
      }

      "when given the error transformed status" - {
        import TestData.TestDataForCombinations.{ ErrorFor211ToTransform, SuccessWrapper, errorStatus211Transf, makeHttpReadsBuilder }

        "and a valid body" in new TestFixture[AnyRef, SuccessWrapper] {
          val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)

          val response: HttpResponse =
            HttpResponse(status = errorStatus211Transf, body = ErrorFor211ToTransform.exampleBody, headers = Map())

          val result: Either[AnyRef, SuccessWrapper] =
            httpReads.read("MYMETHOD", "some/url", response)

          result shouldBe Left(ErrorFor211ToTransform.exampleValue.myTransform)
          savedErrorLogs shouldBe Nil
        }

        "and an empty JSON body" in new TestFixture[AnyRef, SuccessWrapper] {
          val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)

          val response: HttpResponse =
            HttpResponse(status = errorStatus211Transf, body = """{}""", headers = Map())

          expectOneErrorLogAndStoreInVar()

          val result: Either[AnyRef, SuccessWrapper] = httpReads.read("MYMETHOD", "some/url", response)

          result shouldBe Left(
            ConnectorError(
              statusCode = 503,
              message = """JSON structure is not valid in error upstream response."""
            )
          )

          savedErrorLogs shouldBe List(
            s"""JSON structure is not valid in error upstream response.
               |Response status being returned: 503
               |Request made: MYMETHOD some/url
               |Response status received: ${errorStatus211Transf: Int}
               |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
          )
        }

        "and an unparsable text body" in new TestFixture[AnyRef, SuccessWrapper] {
          val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)

          val response: HttpResponse =
            HttpResponse(status = errorStatus211Transf, body = """TEXT""", headers = Map())

          expectOneErrorLogAndStoreInVar()

          val result: Either[AnyRef, SuccessWrapper] =
            httpReads.read("MYMETHOD", "some/url", response)

          result shouldBe Left(
            ConnectorError(
              statusCode = 503,
              message = """Error upstream response body is not JSON."""
            )
          )

          savedErrorLogs shouldBe List(
            s"""Error upstream response body is not JSON.
               |Response status being returned: 503
               |Request made: MYMETHOD some/url
               |Response status received: ${errorStatus211Transf: Int}
               |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
          )
        }
      }

      "when given an unexpected 2xx status, will not log or return the body" - {
        import TestData.TestDataForCombinations.{ SuccessWrapper, makeHttpReadsBuilder, unexpectedStatuses2xx }

        "when given the first successful JSON body" - {
          import TestData.TestDataForCombinations.SuccessWrapper236

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(status = unexpected2xxStatus, body = SuccessWrapper236.exampleBody, headers = Map())

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpected2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpected2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpected2xxStatus: Int}
                   |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
              )

              savedErrorLogs.toString should not include SuccessWrapper236.exampleBody // We don't forward invalid success bodies.
            }
        }

        "when given the second successful JSON body" - {
          import TestData.TestDataForCombinations.SuccessWrapper419

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(status = unexpected2xxStatus, body = SuccessWrapper419.exampleBody, headers = Map())

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpected2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpected2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpected2xxStatus: Int}
                   |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
              )

              savedErrorLogs.toString should not include SuccessWrapper419.exampleBody // We don't forward invalid success bodies.
            }
        }

        "when given the first error JSON body" - {
          import TestData.TestDataForCombinations.ErrorFor109

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(status = unexpected2xxStatus, body = ErrorFor109.exampleBody, headers = Map())

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpected2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpected2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpected2xxStatus: Int}
                   |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
              )

              savedErrorLogs.toString should not include ErrorFor109.exampleBody // We don't forward invalid success bodies.
            }
        }

        "when given the second error JSON body" - {
          import TestData.TestDataForCombinations.ErrorFor211ToTransform

          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(status = unexpected2xxStatus, body = ErrorFor211ToTransform.exampleBody, headers = Map())

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpected2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpected2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpected2xxStatus: Int}
                   |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
              )

              savedErrorLogs.toString should not include ErrorFor211ToTransform.exampleBody // We don't forward invalid success bodies.
            }
        }

        "when given an empty JSON body" - {
          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(status = unexpected2xxStatus, body = """{}""", headers = Map())

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpected2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpected2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpected2xxStatus: Int}
                   |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
              )

              savedErrorLogs.toString should not include "{}" // We don't forward invalid success bodies.
            }
        }

        "when given an unparsable text body" - {
          for (unexpected2xxStatus <- unexpectedStatuses2xx)
            s"<$unexpected2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(status = unexpected2xxStatus, body = """SOMETEXT""", headers = Map())

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpected2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpected2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpected2xxStatus: Int}
                   |Incoming HTTP response body not logged for successful (2xx) statuses.""".stripMargin
              )

              savedErrorLogs.toString should not include "SOMETEXT" // We don't forward invalid success bodies.
            }
        }

      }

      "when given an unexpected non-2xx status, WILL LOG the body, but not return it" - {
        import TestData.TestDataForCombinations.{ SuccessWrapper, makeHttpReadsBuilder, unexpectedStatusesNon2xx }

        "when given the first successful JSON body" - {
          import TestData.TestDataForCombinations.SuccessWrapper236

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(status = unexpectedNon2xxStatus, body = SuccessWrapper236.exampleBody, headers = Map())

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpectedNon2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpectedNon2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpectedNon2xxStatus: Int}
                   |Incoming HTTP response body: ${SuccessWrapper236.exampleBody: String}""".stripMargin
              )
            }
        }

        "when given the second successful JSON body" - {
          import TestData.TestDataForCombinations.SuccessWrapper419

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(status = unexpectedNon2xxStatus, body = SuccessWrapper419.exampleBody, headers = Map())

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpectedNon2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpectedNon2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpectedNon2xxStatus: Int}
                   |Incoming HTTP response body: ${SuccessWrapper419.exampleBody: String}""".stripMargin
              )
            }
        }

        "when given the first error JSON body" - {
          import TestData.TestDataForCombinations.ErrorFor109

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(status = unexpectedNon2xxStatus, body = ErrorFor109.exampleBody, headers = Map())

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpectedNon2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpectedNon2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpectedNon2xxStatus: Int}
                   |Incoming HTTP response body: ${ErrorFor109.exampleBody: String}""".stripMargin
              )
            }
        }

        "when given the second error JSON body" - {
          import TestData.TestDataForCombinations.ErrorFor211ToTransform

          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(
                  status = unexpectedNon2xxStatus,
                  body = ErrorFor211ToTransform.exampleBody,
                  headers = Map()
                )

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpectedNon2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpectedNon2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpectedNon2xxStatus: Int}
                   |Incoming HTTP response body: ${ErrorFor211ToTransform.exampleBody: String}""".stripMargin
              )
            }
        }

        "when given an empty JSON body" - {
          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(status = unexpectedNon2xxStatus, body = """{}""", headers = Map())

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpectedNon2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpectedNon2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpectedNon2xxStatus: Int}
                   |Incoming HTTP response body: {}""".stripMargin
              )
            }
        }

        "when given an unparsable text body" - {
          for (unexpectedNon2xxStatus <- unexpectedStatusesNon2xx)
            s"<$unexpectedNon2xxStatus>" in new TestFixture[AnyRef, SuccessWrapper] {
              val httpReads: HttpReads[Either[AnyRef, SuccessWrapper]] = makeHttpReadsBuilder(obj).httpReads(logger)(hc)

              val response: HttpResponse =
                HttpResponse(status = unexpectedNon2xxStatus, body = """SOMETEXT""", headers = Map())

              expectOneErrorLogAndStoreInVar()

              val result: Either[AnyRef, SuccessWrapper] =
                httpReads.read("MYMETHOD", "some/url", response)

              result shouldBe Left(
                ConnectorError(
                  statusCode = unexpectedNon2xxStatus,
                  message = """Upstream response status is unexpected."""
                )
              )

              savedErrorLogs shouldBe List(
                s"""Upstream response status is unexpected.
                   |Response status being returned: ${unexpectedNon2xxStatus: Int}
                   |Request made: MYMETHOD some/url
                   |Response status received: ${unexpectedNon2xxStatus: Int}
                   |Incoming HTTP response body: SOMETEXT""".stripMargin
              )
            }
        }

      }

    }
  }
}
