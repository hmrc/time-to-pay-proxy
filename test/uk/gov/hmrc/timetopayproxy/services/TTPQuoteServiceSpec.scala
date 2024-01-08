/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import cats.syntax.either._
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope

class TTPQuoteServiceSpec extends UnitSpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val timeToPayRequest = GenerateQuoteRequest(
    CustomerReference("customerReference"),
    ChannelIdentifier.Advisor,
    PlanToGenerateQuote(
      QuoteType.InstalmentAmount,
      LocalDate.of(2021, 1, 1),
      LocalDate.of(2021, 1, 1),
      Some(1),
      Some(Frequency.TwoWeekly),
      Some(Duration(12)),
      Some(1),
      Some(LocalDate.now()),
      PaymentPlanType.TimeToPay
    ),
    List(),
    List()
  )

  private val generateQuoteResponse = GenerateQuoteResponse(
    QuoteReference("quoteReference"),
    CustomerReference("customerReference"),
    QuoteType.InstalmentAmount,
    LocalDate.now(),
    1,
    1.5,
    100,
    0.1,
    0.1,
    List(
      Instalment(
        DebtItemChargeId("dutyId"),
        LocalDate.parse("2022-01-01"),
        100,
        100,
        0.1,
        1,
        0.5,
        50
      )
    ),
    Collections(
      Some(InitialCollection(LocalDate.now(), 1)),
      List(RegularCollection(LocalDate.parse("2022-01-01"), 100))
    )
  )

  private val retrievePlanResponse = ViewPlanResponse(
    CustomerReference("customerRef1234"),
    ChannelIdentifier.Advisor,
    ViewPlanResponsePlan(
      PlanId("planId123"),
      CaseId("caseId123"),
      QuoteId("quoteId"),
      LocalDate.now(),
      QuoteType.InstalmentAmount,
      PaymentPlanType.TimeToPay,
      thirdPartyBank = true,
      0,
      None,
      None,
      0,
      0.0,
      0,
      0.0
    ),
    Seq(
      DebtItemCharge(
        DebtItemChargeId("debtItemChargeId1"),
        "1546",
        "1090",
        100,
        Some(LocalDate.parse("2021-05-13")),
        List(Payment(LocalDate.parse("2021-05-13"), 100))
      )
    ),
    Seq.empty[PaymentInformation],
    Seq.empty[CustomerPostCode],
    Seq(
      Instalment(
        DebtItemChargeId("debtItemChargeId"),
        LocalDate.parse("2021-05-01"),
        100,
        100,
        0.26,
        1,
        10.20,
        100
      ),
      Instalment(
        debtItemChargeId = DebtItemChargeId("debtItemChargeId"),
        dueDate = LocalDate.parse("2021-06-01"),
        amountDue = 100,
        expectedPayment = 100,
        interestRate = 0.26,
        instalmentNumber = 2,
        instalmentInterestAccrued = 10.20,
        instalmentBalance = 100
      )
    ),
    collections = Collections(
      None,
      List(
        RegularCollection(dueDate = LocalDate.parse("2021-05-01"), amountDue = 100),
        RegularCollection(dueDate = LocalDate.parse("2021-06-01"), amountDue = 100)
      )
    )
  )

  private val retrievePlanResponseDropTwo: ViewPlanResponseDropTwo = ViewPlanResponseDropTwo(
    customerReference = CustomerReference("customerRef1234"),
    channelIdentifier = ChannelIdentifier.Advisor,
    caseId = CaseId("caseId123"),
    plan = ViewPlanResponsePlanDropTwo(
      planId = PlanId("planId123"),
      quoteId = QuoteId("quoteId"),
      quoteDate = LocalDate.now(),
      quoteType = QuoteType.InstalmentAmount,
      paymentPlanType = PaymentPlanType.TimeToPay,
      thirdPartyBank = true,
      numberOfInstalments = 0,
      totalDebtIncInt = 0,
      totalInterest = 0.0,
      interestAccrued = 0,
      planInterest = 0.0
    ),
    debtItemCharges = Seq(
      DebtItemCharge(
        DebtItemChargeId("debtItemChargeId1"),
        "1546",
        "1090",
        100,
        Some(LocalDate.parse("2021-05-13")),
        List(Payment(LocalDate.parse("2021-05-13"), 100))
      )
    ),
    payments = Seq.empty[PaymentInformation],
    customerPostCodes = Seq.empty[CustomerPostCode],
    instalments = Seq(
      Instalment(
        DebtItemChargeId("debtItemChargeId"),
        LocalDate.parse("2021-05-01"),
        100,
        100,
        0.26,
        1,
        10.20,
        100
      ),
      Instalment(
        DebtItemChargeId("debtItemChargeId"),
        LocalDate.parse("2021-06-01"),
        100,
        100,
        0.26,
        2,
        10.20,
        100
      )
    ),
    collections = Collections(
      None,
      List(
        RegularCollection(dueDate = LocalDate.parse("2021-05-01"), amountDue = 100),
        RegularCollection(dueDate = LocalDate.parse("2021-06-01"), amountDue = 100)
      )
    )
  )

  private val updatePlanRequest =
    UpdatePlanRequest(
      CustomerReference("customerReference"),
      PlanId("planId"),
      UpdateType("updateType"),
      Some(PlanStatus.ResolvedCompleted),
      Some(CompleteReason.PaymentInFull),
      None,
      Some(true),
      Some(
        List(
          PaymentInformation(PaymentMethod.Bacs, Some(PaymentReference("reference")))
        )
      )
    )

  private val updatePlanResponse = UpdatePlanResponse(
    CustomerReference("customerReference"),
    PlanId("planId"),
    PlanStatus.Success,
    LocalDate.now
  )

  private val createPlanRequest =
    CreatePlanRequest(
      CustomerReference("customerReference"),
      QuoteReference("quoteReference"),
      ChannelIdentifier.Advisor,
      PlanToCreatePlan(
        QuoteId("quoteId"),
        QuoteType.Duration,
        LocalDate.now(),
        LocalDate.now(),
        Some(100),
        PaymentPlanType.TimeToPay,
        false,
        2,
        Some(Frequency.Single),
        Some(Duration(2)),
        Some(PaymentMethod.Bacs),
        Some(PaymentReference("ref123")),
        Some(LocalDate.now()),
        Some(100),
        100,
        10,
        10,
        10
      ),
      List(
        DebtItemCharge(
          DebtItemChargeId("debtItemChargeId"),
          "1525",
          "1000",
          100,
          Some(LocalDate.now()),
          List(Payment(LocalDate.parse("2020-01-01"), 100))
        )
      ),
      List(PaymentInformation(PaymentMethod.Bacs, Some(PaymentReference("ref123")))),
      List(CustomerPostCode(PostCode("NW1 AB1"), LocalDate.now())),
      List(
        Instalment(
          DebtItemChargeId("id1"),
          LocalDate.now(),
          100,
          100,
          0.24,
          1,
          10,
          90
        )
      )
    )

  private val createPlanResponse = CreatePlanResponse(
    CustomerReference("customerReference"),
    PlanId("planId"),
    CaseId("caseId"),
    PlanStatus.Success
  )

  "Generate Quote endpoint" should {
    "return a success response" when {
      "connector returns success" in {
        val connector = mock[TtpConnector]
        (
          connector
            .generateQuote(_: GenerateQuoteRequest, _: Seq[(String, String)])(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(timeToPayRequest, *, *, *)
          .returning(TtppEnvelope(generateQuoteResponse))

        val quoteService = new DefaultTTPQuoteService(connector)
        await(
          quoteService.generateQuote(timeToPayRequest, Map.empty).value,
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
            .generateQuote(_: GenerateQuoteRequest, _: Seq[(String, String)])(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(timeToPayRequest, *, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[GenerateQuoteResponse])
          )

        val quoteService = new DefaultTTPQuoteService(connector)
        await(
          quoteService.generateQuote(timeToPayRequest, Map.empty).value,
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
        Right(retrievePlanResponseDropTwo),
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
        Right(retrievePlanResponseDropTwo),
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
        Right(retrievePlanResponseDropTwo),
        Right(updatePlanResponse),
        Right(createPlanResponse)
      )
      val quoteService = new DefaultTTPQuoteService(connectorStub)

      await(quoteService.createPlan(createPlanRequest, Map.empty).value) shouldBe createPlanResponse
        .asRight[TtppError]
    }

    "return a error if the service does not return a successful response" in {
      val connectorStub = new TtpConnectorStub(
        Right(generateQuoteResponse),
        Right(retrievePlanResponse),
        Right(retrievePlanResponseDropTwo),
        Right(updatePlanResponse),
        Left(ConnectorError(500, "Internal server error"))
      )
      val quoteService = new DefaultTTPQuoteService(connectorStub)

      await(quoteService.createPlan(createPlanRequest, Map.empty).value) shouldBe ConnectorError(
        500,
        "Internal server error"
      ).asLeft[CreatePlanResponse]

    }
  }
}

class TtpConnectorStub(
  generateQuoteResponse: Either[TtppError, GenerateQuoteResponse],
  retrieveQuoteResponse: Either[TtppError, ViewPlanResponse],
  retrieveQuoteResponseDropTwo: Either[TtppError, ViewPlanResponseDropTwo],
  updatePlanResponse: Either[TtppError, UpdatePlanResponse],
  createPlanResponse: Either[TtppError, CreatePlanResponse]
) extends TtpConnector {
  override def generateQuote(
    ttppRequest: GenerateQuoteRequest,
    queryParams: Seq[(String, String)] = Seq.empty
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[GenerateQuoteResponse] =
    TtppEnvelope(Future successful generateQuoteResponse)

  override def getExistingQuote(customerReference: CustomerReference, planId: PlanId)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[ViewPlanResponse] =
    TtppEnvelope(Future successful retrieveQuoteResponse)

  override def getExistingQuoteDropTwo(customerReference: CustomerReference, planId: PlanId)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[ViewPlanResponseDropTwo] =
    TtppEnvelope(Future successful retrieveQuoteResponseDropTwo)

  override def updatePlan(
    updatePlanRequest: UpdatePlanRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[UpdatePlanResponse] =
    TtppEnvelope(Future successful updatePlanResponse)

  override def createPlan(
    createPlanRequest: CreatePlanRequest,
    queryParams: Seq[(String, String)] = Seq.empty
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[CreatePlanResponse] =
    TtppEnvelope(Future successful createPlanResponse)
}
