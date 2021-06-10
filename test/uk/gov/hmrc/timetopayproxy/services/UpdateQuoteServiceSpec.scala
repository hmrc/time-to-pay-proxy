/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate
import java.util.concurrent.TimeUnit

import cats.syntax.either._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpConnector
import uk.gov.hmrc.timetopayproxy.models
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.support.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class UpdateQuoteServiceSpec extends UnitSpec {
  implicit val hc = HeaderCarrier()
  val updateQuoteRequest = UpdateQuoteRequest(
    CustomerReference("customerReference"),
    PlanId("pegaId"),
    UpdateType("updateType"),
    CancellationReason("reason"),
    PaymentMethod("method"),
    PaymentReference("reference"),
    true
  )

  "Update Quote endpoint" should {
    "return a success response" when {
      "connector returns success" in {
        val responseFromTtp = UpdateQuoteResponse(
          CustomerReference("customerReference"),
          PlanId("pegaId"),
          QuoteStatus("quoteStatus"),
          LocalDate.now
        )
        val connector = mock[TtpConnector]
        (
          connector
            .updateQuote(
              _: UpdateQuoteRequest
            )
            (
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(updateQuoteRequest, *, *)
          .returning(TtppEnvelope(responseFromTtp))

        val ttpQuoteService = new DefaultTTPQuoteService(connector)
        await(
          ttpQuoteService.updateQuote(updateQuoteRequest).value,
          5,
          TimeUnit.SECONDS
        ) shouldBe responseFromTtp.asRight[TtppError]
      }
    }
    "return a failure response" when {
      "connector returns failure" in {

        val errorFromTtpConnector = ConnectorError(500, "Internal Service Error")
        val connector = mock[TtpConnector]
        (
          connector
            .updateQuote(
              _: UpdateQuoteRequest
            )
            (
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(updateQuoteRequest, *, *)
          .returning(TtppEnvelope(errorFromTtpConnector.asLeft[UpdateQuoteResponse]))

        val ttpQuoteService = new DefaultTTPQuoteService(connector)
        await(
          ttpQuoteService.updateQuote(updateQuoteRequest).value,
          5,
          TimeUnit.SECONDS
        ) shouldBe errorFromTtpConnector.asLeft[UpdateQuoteResponse]
      }
    }
  }
}
