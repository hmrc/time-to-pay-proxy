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

package uk.gov.hmrc.timetopayproxy.services

import cats.data.NonEmptyList
import cats.syntax.either._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpFeedbackLoopConnector
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.error.{ ConnectorError, TtppEnvelope }
import uk.gov.hmrc.timetopayproxy.models.saonly.common.apistatus.{ ApiName, ApiStatus, ApiStatusCode }
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel.{ CancellationDate, TtpCancelPaymentPlan, TtpCancelRequest, TtpCancelSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform.{ DdiReference, TtpInformPaymentPlan, TtpInformRequest, TtpInformSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models._

import java.time.{ Instant, LocalDate }
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class TtpFeedbackLoopServiceSpec extends AnyFreeSpec with MockFactory with ScalaFutures {

  private val mockConnector = mock[TtpFeedbackLoopConnector]
  private val service = new TtpFeedbackLoopService(mockConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "TtpFeedbackLoopService" - {
    "cancelTtp" - {

      val ttpCancelRequest = TtpCancelRequest(
        identifications = NonEmptyList.of(
          Identification(idType = IdType("NINO"), idValue = IdValue("AB123456C"))
        ),
        paymentPlan = TtpCancelPaymentPlan(
          arrangementAgreedDate = ArrangementAgreedDate(LocalDate.of(2025, 1, 1)),
          ttpEndDate = TtpEndDate(LocalDate.of(2025, 2, 1)),
          frequency = FrequencyLowercase.Monthly,
          cancellationDate = CancellationDate(LocalDate.of(2025, 1, 15)),
          initialPaymentDate = Some(InitialPaymentDate(LocalDate.of(2025, 1, 5))),
          initialPaymentAmount = Some(GbpPounds.createOrThrow(100.00))
        ),
        instalments = NonEmptyList.of(
          SaOnlyInstalment(
            InstalmentDueDate(LocalDate.of(2024, 2, 1)),
            GbpPounds.createOrThrow(500.25)
          )
        ),
        channelIdentifier = ChannelIdentifier.Advisor,
        transitioned = Some(TransitionedIndicator(true))
      )

      val ttpCancelResponse = TtpCancelSuccessfulResponse(
        apisCalled = List(
          ApiStatus(
            name = ApiName("TTP_PROXY"),
            statusCode = ApiStatusCode(200),
            processingDateTime = ProcessingDateTimeInstant(Instant.parse("2024-01-15T10:30:00Z")),
            errorResponse = None
          )
        ),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2024-01-15T10:30:00Z"))
      )

      "return success response when connector returns success" in {
        (mockConnector
          .cancelTtp(_: TtpCancelRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(ttpCancelRequest, *, *)
          .returning(TtppEnvelope(ttpCancelResponse))

        val result = service.cancelTtp(ttpCancelRequest)

        result.value.futureValue shouldBe ttpCancelResponse.asRight
      }

      "return error when connector returns error" in {
        val error = ConnectorError(500, "Internal Server Error")

        (mockConnector
          .cancelTtp(_: TtpCancelRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(ttpCancelRequest, *, *)
          .returning(TtppEnvelope(error.asLeft[TtpCancelSuccessfulResponse]))

        val result = service.cancelTtp(ttpCancelRequest)

        result.value.futureValue shouldBe error.asLeft
      }
    }

    "informTtp" - {

      val ttpInformRequest = TtpInformRequest(
        identifications = NonEmptyList.of(
          Identification(idType = IdType("NINO"), idValue = IdValue("AB123456C"))
        ),
        paymentPlan = TtpInformPaymentPlan(
          arrangementAgreedDate = ArrangementAgreedDate(LocalDate.of(2025, 1, 1)),
          ttpEndDate = TtpEndDate(LocalDate.of(2025, 2, 1)),
          frequency = FrequencyLowercase.Monthly,
          initialPaymentDate = Some(InitialPaymentDate(LocalDate.of(2025, 1, 5))),
          initialPaymentAmount = Some(GbpPounds.createOrThrow(100.00)),
          ddiReference = Some(DdiReference("Test DDI Reference"))
        ),
        instalments = NonEmptyList.of(
          SaOnlyInstalment(
            InstalmentDueDate(LocalDate.of(2024, 2, 1)),
            GbpPounds.createOrThrow(500.25)
          )
        ),
        channelIdentifier = ChannelIdentifier.Advisor,
        transitioned = Some(TransitionedIndicator(true))
      )

      val ttpInformResponse = TtpInformSuccessfulResponse(
        apisCalled = List(
          ApiStatus(
            name = ApiName("TTP_PROXY"),
            statusCode = ApiStatusCode(200),
            processingDateTime = ProcessingDateTimeInstant(Instant.parse("2024-01-15T10:30:00Z")),
            errorResponse = None
          )
        ),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2024-01-15T10:30:00Z"))
      )

      "return success response when connector returns success" in {
        (mockConnector
          .informTtp(_: TtpInformRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(ttpInformRequest, *, *)
          .returning(TtppEnvelope(ttpInformResponse))

        val result = service.informTtp(ttpInformRequest)

        result.value.futureValue shouldBe ttpInformResponse.asRight
      }

      "return error when connector returns error" in {
        val error = ConnectorError(500, "Internal Server Error")

        (mockConnector
          .informTtp(_: TtpInformRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(ttpInformRequest, *, *)
          .returning(TtppEnvelope(error.asLeft[TtpInformSuccessfulResponse]))

        val result = service.informTtp(ttpInformRequest)

        result.value.futureValue shouldBe error.asLeft
      }
    }
  }
}
