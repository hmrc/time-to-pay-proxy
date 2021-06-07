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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpConnector
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.support.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import cats.syntax.either._

class GenerateQuoteServiceSpec extends UnitSpec {
  implicit val hc = HeaderCarrier()
  val timeToPayRequest = TimeToPayRequest(
    "customerReference",
    10,
    List(Customer("quoteType", "2021-01-01", 1, "", "", 1, LocalDate.now(), "paymentPlanType")),
    List()
  )

  "Generate Quote endpoint" should {
    "return a success response" when {
      "connector returns success" in {
        val responseFromTtp = TimeToPayResponse(
          "quoteReference",
          "customerReference",
          "quoteStatus",
          "quoteType",
          List(Payment("2021-01-01", 1)),
          1,
          "",
          0.1,
          1
        )
        val connector = mock[TtpConnector]
        (
          connector
            .generateQuote(
              _: TimeToPayRequest
            )
            (
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(timeToPayRequest, *, *)
          .returning(TtppEnvelope(responseFromTtp))

        val quoteService = new DefaultGenerateQuoteService(connector)
        await(
          quoteService.generateQuote(timeToPayRequest).value,
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
            .generateQuote(
              _: TimeToPayRequest
            )
            (
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(timeToPayRequest, *, *)
          .returning(TtppEnvelope(errorFromTtpConnector.asLeft[TimeToPayResponse]))

        val quoteService = new DefaultGenerateQuoteService(connector)
        await(
          quoteService.generateQuote(timeToPayRequest).value,
          5,
          TimeUnit.SECONDS
        ) shouldBe errorFromTtpConnector.asLeft[TimeToPayResponse]
      }
    }
  }
}
