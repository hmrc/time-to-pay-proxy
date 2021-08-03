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
  private val timeToPayRequest = GenerateQuoteRequest(
    CustomerReference("customerReference"),
    ChannelIdentifier("channelIdentifier"),
    Plan(
      QuoteType("quoteType"),
      LocalDate.of(2021, 1, 1),
      LocalDate.of(2021, 1, 1),
      1,
      Frequency("some frequency"),
      Duration(12),
      1,
      LocalDate.now(),
      PaymentPlanType("paymentPlanType")
    ),
    List(),
    List()
  )

  private val generateQuoteResponse = GenerateQuoteResponse(
    QuoteReference("quoteReference"),
    CustomerReference("customerReference"),
    QuoteType("quoteType"),
    LocalDate.now(),
    1,
    1.5,
    100,
    0.1,
    List(
      Instalment(
        DebtItemChargeId("dutyId"),
        DebtItemId("debtId"),
        LocalDate.parse("2022-01-01"),
        100,
        100,
        0.1,
        1,
        0.5,
        50
      )
    )
  )

  private val retrievePlanResponse = ViewPlanResponse(
    CustomerReference("someCustomerRef"),
    PlanId("somePlanId"),
    QuoteType("someQuoateStatus"),
    "xyz",
    "ref",
    Nil,
    "2",
    100,
    0.26
  )

  private val updatePlanRequest =
    UpdatePlanRequest(
      CustomerReference("customerReference"),
      PlanId("planId"),
      UpdateType("updateType"),
      CancellationReason("reason"),
      PaymentMethod("method"),
      PaymentReference("reference"),
      true,
    )

  private val updatePlanResponse = UpdatePlanResponse(
    CustomerReference("customerReference"),
    PlanId("planId"),
    QuoteStatus("quoteStatus"),
    LocalDate.now
  )

  private val createPlanRequest =
    CreatePlanRequest(
      CustomerReference("customerReference"),
      PlanId("planId"),
      "xyz",
      "paymentRed",
      false,
      Nil,
      "2",
      10000,
      0.26
    )

  private val createPlanResponse = CreatePlanResponse(
    CustomerReference("customerReference"),
    PlanId("planId"),
    "xyz"
  )

  "Generate Quote endpoint" should {
    "return a success response" when {
      "connector returns success" in {
        val connector = mock[TtpConnector]
        (
          connector
            .generateQuote(_: GenerateQuoteRequest)(
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

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")
        val connector = mock[TtpConnector]
        (
          connector
            .generateQuote(_: GenerateQuoteRequest)(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(timeToPayRequest, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[GenerateQuoteResponse])
          )

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

      val connectorStub = new TtpConnectorStub(
        Right(generateQuoteResponse),
        Right(retrievePlanResponse),
        Right(updatePlanResponse),
        Right(createPlanResponse)
      )
      val quoteService = new DefaultTTPQuoteService(connectorStub)

      await(
        quoteService
          .getExistingPlan(
            CustomerReference("someCustomer"),
            PlanId("somePlanId")
          )
          .value
      ) shouldBe retrievePlanResponse
        .asRight[TtppError]
    }

    "return a error if the service does not return a successful response" in {
      val connectorStub = new TtpConnectorStub(
        Right(generateQuoteResponse),
        Left(ConnectorError(500, "Internal server error")),
        Right(updatePlanResponse),
        Right(createPlanResponse)
      )
      val quoteService = new DefaultTTPQuoteService(connectorStub)

      await(
        quoteService
          .getExistingPlan(
            CustomerReference("someCustomer"),
            PlanId("somePlanId")
          )
          .value
      ) shouldBe ConnectorError(500, "Internal server error")
        .asLeft[ViewPlanResponse]

    }
  }

  "Update Quote endpoint" should {

    "return a success response" when {
      "connector returns success" in {

        val connector = mock[TtpConnector]
        (
          connector
            .updatePlan(_: UpdatePlanRequest)(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(updatePlanRequest, *, *)
          .returning(TtppEnvelope(updatePlanResponse))

        val ttpQuoteService = new DefaultTTPQuoteService(connector)
        await(
          ttpQuoteService.updatePlan(updatePlanRequest).value,
          5,
          TimeUnit.SECONDS
        ) shouldBe updatePlanResponse.asRight[TtppError]
      }
    }
    "return a failure response" when {
      "connector returns failure" in {

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")
        val connector = mock[TtpConnector]
        (
          connector
            .updatePlan(_: UpdatePlanRequest)(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(updatePlanRequest, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[UpdatePlanResponse])
          )

        val ttpQuoteService = new DefaultTTPQuoteService(connector)
        await(
          ttpQuoteService.updatePlan(updatePlanRequest).value,
          5,
          TimeUnit.SECONDS
        ) shouldBe errorFromTtpConnector.asLeft[UpdatePlanResponse]
      }
    }
  }

  "Create Plan" should {
    "return a created plan when the service returns a successful response" in {

      val connectorStub = new TtpConnectorStub(
        Right(generateQuoteResponse),
        Right(retrievePlanResponse),
        Right(updatePlanResponse),
        Right(createPlanResponse)
      )
      val quoteService = new DefaultTTPQuoteService(connectorStub)

      await(quoteService.createPlan(createPlanRequest).value) shouldBe createPlanResponse
        .asRight[TtppError]
    }

    "return a error if the service does not return a successful response" in {
      val connectorStub = new TtpConnectorStub(
        Right(generateQuoteResponse),
        Right(retrievePlanResponse),
        Right(updatePlanResponse),
        Left(ConnectorError(500, "Internal server error"))
      )
      val quoteService = new DefaultTTPQuoteService(connectorStub)

      await(quoteService.createPlan(createPlanRequest).value) shouldBe ConnectorError(
        500,
        "Internal server error"
      ).asLeft[CreatePlanResponse]

    }
  }
}

class TtpConnectorStub(
  generateQuoteResponse: Either[TtppError, GenerateQuoteResponse],
  retrieveQuoteResponse: Either[TtppError, ViewPlanResponse],
  updatePlanResponse: Either[TtppError, UpdatePlanResponse],
  createPlanResponse: Either[TtppError, CreatePlanResponse]
)(implicit ec: ExecutionContext)
    extends TtpConnector {
  override def generateQuote(ttppRequest: GenerateQuoteRequest)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[GenerateQuoteResponse] =
    TtppEnvelope(Future successful generateQuoteResponse)

  override def getExistingQuote(customerReference: CustomerReference,
                                planId: PlanId)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[ViewPlanResponse] =
    TtppEnvelope(Future successful retrieveQuoteResponse)

  override def updatePlan(updatePlanRequest: UpdatePlanRequest)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[UpdatePlanResponse] =
    TtppEnvelope(Future successful updatePlanResponse)

  override def createPlan(createPlanRequest: CreatePlanRequest)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[CreatePlanResponse] =
    TtppEnvelope(Future successful createPlanResponse)
}
