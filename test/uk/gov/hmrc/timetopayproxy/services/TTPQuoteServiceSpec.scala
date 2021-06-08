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

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import cats.syntax.either._
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope

class TTPQuoteServiceSpec extends UnitSpec {
  implicit val hc = HeaderCarrier()
  val timeToPayRequest = GenerateQuoteRequest(
    "customerReference",
    10,
    List(Customer("quoteType", "2021-01-01", 1, "", "", 1, LocalDate.now(), "paymentPlanType")),
    List()
  )

  val generateQuoteResponse = GenerateQuoteResponse(
    "quoteReference",
    "customerReference",
    "quoteStatus",
    "quoteType",
    List(Payment(LocalDate.parse("2021-01-01"), 1)),
    1,
    "",
    0.1,
    1
  )

  val retrievePlanResponse = RetrievePlanResponse(
    "someCustomerRef",
    "somePegaId",
    "someQuoateStatus",
    "xyz",
    "ref",
    "info",
    "info",
    Nil,
    Nil,
    "2",
    100,
    0.26)

  "Generate Quote endpoint" should {
    "return a success response" when {
      "connector returns success" in {
        val connector = mock[TtpConnector]
        (
          connector
            .generateQuote(
              _: GenerateQuoteRequest
            )
            (
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(timeToPayRequest, *, *)
          .returning(TtppEnvelope(generateQuoteResponse))

        val quoteService = new DefaultTTPQuoteService(connector)
        await(
          quoteService.generateQuote(timeToPayRequest).value,
          5,
          TimeUnit.SECONDS
        ) shouldBe generateQuoteResponse.asRight[TtppError]
      }
    }
    "return a failure response" when {
      "connector returns failure" in {

        val errorFromTtpConnector = ConnectorError(500, "Internal Service Error")
        val connector = mock[TtpConnector]
        (
          connector
            .generateQuote(
              _: GenerateQuoteRequest
            )
            (
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(timeToPayRequest, *, *)
          .returning(TtppEnvelope(errorFromTtpConnector.asLeft[GenerateQuoteResponse]))

        val quoteService = new DefaultTTPQuoteService(connector)
        await(
          quoteService.generateQuote(timeToPayRequest).value,
          5,
          TimeUnit.SECONDS
        ) shouldBe errorFromTtpConnector.asLeft[GenerateQuoteResponse]
      }
    }
  }

  "Retrieve Existing Quote" should {
    "return a quote when the service returns a successful response" in {

      val connectorStub = new TtpConnectorStub(Right(generateQuoteResponse), Right(retrievePlanResponse))
      val quoteService = new DefaultTTPQuoteService(connectorStub)

      await(quoteService.getExistingPlan("someCustomer", "somePegaId").value) shouldBe retrievePlanResponse.asRight[TtppError]
    }

    "return a error if the service does not return a successful response" in {
      val connectorStub = new TtpConnectorStub(Right(generateQuoteResponse), Left(ConnectorError(500, "Internal server error")))
      val quoteService = new DefaultTTPQuoteService(connectorStub)

      await(quoteService.getExistingPlan("someCustomer", "somePegaId").value) shouldBe ConnectorError(500, "Internal server error").asLeft[RetrievePlanResponse]

    }
  }

}

class TtpConnectorStub(generateQuoteResponse: Either[TtppError, GenerateQuoteResponse],
                       retrieveQuoteResponse: Either[TtppError, RetrievePlanResponse]
                      )(implicit ec: ExecutionContext) extends TtpConnector {
  override def generateQuote(ttppRequest: GenerateQuoteRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[GenerateQuoteResponse] = TtppEnvelope(Future successful generateQuoteResponse)

  override def getExistingQuote(customerReference: String, pegaId: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[RetrievePlanResponse] = TtppEnvelope(Future successful retrieveQuoteResponse)
}