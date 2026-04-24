/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.logging

import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.http.Status
import play.api.test.Helpers.{ await, defaultAwaitTimeout }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.error.{ ConnectorError, TtppEnvelope }
import uk.gov.hmrc.timetopayproxy.models.saonly.common.ProcessingDateTimeInstant
import uk.gov.hmrc.timetopayproxy.models.saonly.common.apistatus.{ ApiName, ApiStatus, ApiStatusCode }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel.{ TtpCancelInformativeError, TtpCancelInternalError }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpfullamend.TtpFullAmendSuccessfulResponse

import java.time.Instant
import scala.concurrent.ExecutionContext

class StatusLoggerSpec extends AnyFreeSpec with MockFactory {
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockLogger: RequestAwareLogger = mock[RequestAwareLogger]

  "StatusLogger" - {
    "logBasedOnStatusCode" - {
      "should return the provided TtppEnvelope" - {
        "when a successful response is provided" - {
          "and not log anything" in {
            val successfulResponse: TtpFullAmendSuccessfulResponse = TtpFullAmendSuccessfulResponse(
              apisCalled = List(
                ApiStatus(
                  name = ApiName("API1"),
                  statusCode = ApiStatusCode(200),
                  processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
                  errorResponse = None
                )
              ),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
            )

            val statusLogger: StatusLogger[TtpFullAmendSuccessfulResponse] =
              StatusLogger(TtppEnvelope(successfulResponse))

            (mockLogger
              .alert(_: PagerAlert, _: String)(_: HeaderCarrier))
              .expects(*, *, *)
              .returning(())
              .never()

            val result = statusLogger.logBasedOnStatusCode(mockLogger)

            await(result.value) shouldBe Right(successfulResponse)
          }
        }

        "when an error response is provided" - {
          "and log when a ConnectorError status is 400" in {
            val connectorError = Left(ConnectorError(Status.BAD_REQUEST, "Test message"))

            def testEither: TtppEnvelope[TtpFullAmendSuccessfulResponse] =
              TtppEnvelope[TtpFullAmendSuccessfulResponse](connectorError)

            val statusLogger: StatusLogger[TtpFullAmendSuccessfulResponse] = StatusLogger(testEither)

            (mockLogger
              .alert(_: PagerAlert, _: String)(_: HeaderCarrier))
              .expects(PagerAlert.ProxyOtherIssueAlert, *, *)
              .returning(())
              .once()

            val result = statusLogger.logBasedOnStatusCode(mockLogger)

            await(result.value) shouldBe connectorError
          }

          "and not log when a ConnectorError status is not 400" in {
            val connectorError = Left(ConnectorError(Status.IM_A_TEAPOT, "Test message"))

            val statusLogger: StatusLogger[TtpFullAmendSuccessfulResponse] =
              StatusLogger(TtppEnvelope[TtpFullAmendSuccessfulResponse](connectorError))

            (mockLogger
              .alert(_: PagerAlert, _: String)(_: HeaderCarrier))
              .expects(PagerAlert.ProxyOtherIssueAlert, *, *)
              .returning(())
              .never()

            val result = statusLogger.logBasedOnStatusCode(mockLogger)

            await(result.value) shouldBe connectorError
          }

          "and not log with a different error" in {
            val nonConnectorError = Left(
              TtpCancelInformativeError(
                apisCalled = Some(
                  List(
                    ApiStatus(
                      ApiName("API1"),
                      ApiStatusCode(200),
                      ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
                      errorResponse = None
                    )
                  )
                ),
                internalErrors = List(TtpCancelInternalError("some error that ttp is responsible for")),
                processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
              )
            )

            val statusLogger: StatusLogger[TtpFullAmendSuccessfulResponse] =
              StatusLogger(TtppEnvelope[TtpFullAmendSuccessfulResponse](nonConnectorError))

            (mockLogger
              .alert(_: PagerAlert, _: String)(_: HeaderCarrier))
              .expects(PagerAlert.ProxyOtherIssueAlert, *, *)
              .returning(())
              .never()

            val result = statusLogger.logBasedOnStatusCode(mockLogger)

            await(result.value) shouldBe nonConnectorError
          }
        }
      }
    }
  }
}
