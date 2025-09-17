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
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpFeedbackLoopConnector
import uk.gov.hmrc.timetopayproxy.models.error.{ ConnectorError, TtppEnvelope }
import uk.gov.hmrc.timetopayproxy.models.saopled.ttpcancel.{ CancellationDate, TtpCancelPaymentPlan, TtpCancelRequest, TtpCancelSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.saopled.common.apistatus.{ ApiName, ApiStatus, ApiStatusCode }
import uk.gov.hmrc.timetopayproxy.models.saopled.common.{ ArrangementAgreedDate, InitialPaymentDate, ProcessingDateTimeInstant, SaOpLedInstalment, TransitionedIndicator, TtpEndDate }
import uk.gov.hmrc.timetopayproxy.models.{ ChannelIdentifier, FrequencyLowercase, IdType, IdValue, Identification, InstalmentDueDate }
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPoundsUnchecked

import java.time.{ Instant, LocalDate }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext

class TtpFeedbackLoopServiceSpec extends AnyWordSpec with MockFactory with ScalaFutures {

  private val mockConnector = mock[TtpFeedbackLoopConnector]
  private val service = new DefaultTtpFeedbackLoopService(mockConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val ttpCancelRequest = TtpCancelRequest(
    identifications = NonEmptyList.of(
      Identification(idType = IdType("NINO"), idValue = IdValue("AB123456C"))
    ),
    paymentPlan = TtpCancelPaymentPlan(
      arrangementAgreedDate = ArrangementAgreedDate(LocalDate.of(2025, 1, 1)),
      ttpEndDate = TtpEndDate(LocalDate.of(2025, 2, 1)),
      frequency = FrequencyLowercase.Monthly,
      cancellationDate = CancellationDate(LocalDate.of(2025, 1, 15)),
      initialPaymentDate = Some(InitialPaymentDate(LocalDate.of(2025, 1, 5))),
      initialPaymentAmount = Some(GbpPoundsUnchecked(100.00))
    ),
    instalments = NonEmptyList.of(
      SaOpLedInstalment(
        InstalmentDueDate(LocalDate.of(2024, 2, 1)),
        GbpPoundsUnchecked(500.25)
      )
    ),
    channelIdentifier = ChannelIdentifier.Advisor,
    transitioned = Some(TransitionedIndicator(true))
  )

  private val ttpCancelResponse = TtpCancelSuccessfulResponse(
    apisCalled = List(
      ApiStatus(
        name = ApiName("TTP_PROXY"),
        statusCode = ApiStatusCode("SUCCESS"),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2024-01-15T10:30:00Z")),
        errorResponse = None
      )
    ),
    processingDateTime = ProcessingDateTimeInstant(Instant.parse("2024-01-15T10:30:00Z"))
  )

  "TtpFeedbackLoopService" should {
    "cancelTtp" should {
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
  }
}
