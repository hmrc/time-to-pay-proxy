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

package uk.gov.hmrc.timetopayproxy.controllers

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.RequestMethod.{GET, POST, PUT}
import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.affordablequotes.AffordableQuotesRequest
import uk.gov.hmrc.timetopayproxy.support.IntegrationBaseSpec

import java.time.LocalDate
import java.util.UUID

class TimeToPayProxyControllerCorrelationIdItSpec extends IntegrationBaseSpec {
  def internalAuthEnabled: Boolean = false
  def enrolmentAuthEnabled: Boolean = false
  def saRelease2Enabled: Boolean = true

  val testCorrelationId: String = UUID.randomUUID().toString
  val uuidRegex: String = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

  ".generateQuote" - {
    val generateQuoteRequest = GenerateQuoteRequest(
      CustomerReference("CustRef1234"),
      ChannelIdentifier.Advisor,
      PlanToGenerateQuote(
        QuoteType.Duration,
        LocalDate.now(),
        LocalDate.now(),
        Some(5),
        Some(FrequencyLowercase.TwoWeekly),
        None,
        Some(1),
        None,
        PaymentPlanType.TimeToPay
      ),
      List.empty,
      List.empty,
      regimeType = None
    )

    "should propagate a correlationId to TTP when it's provided one in the request" in {
      stubRequest(
        httpMethod = POST,
        url = "/debts/time-to-pay/quote",
        status = 200,
        responseBody = ""
      )

      val request: WSRequest = buildRequest("/quote").withHttpHeaders("correlationId" -> testCorrelationId)

      await(
        request.post(Json.toJson(generateQuoteRequest))
      )

      verify(
        postRequestedFor(urlEqualTo("/debts/time-to-pay/quote"))
          .withHeader("correlationId", equalTo(testCorrelationId))
      )
    }

    "should generate a new correlationId to send to TTP when it's not provided in the request" in {
      stubRequest(
        httpMethod = POST,
        url = "/debts/time-to-pay/quote",
        status = 200,
        responseBody = ""
      )

      val request: WSRequest = buildRequest("/quote")

      request.header("correlationId") shouldBe None

      await(
        request.post(Json.toJson(generateQuoteRequest))
      )

      verify(
        postRequestedFor(urlEqualTo("/debts/time-to-pay/quote"))
          .withHeader("correlationId", matching(uuidRegex))
          .withHeader("correlationId", WireMock.not(equalTo(testCorrelationId)))
      )
    }
  }

  ".viewPlan" - {
    val customerReference = "testCustomerReference"
    val planId = "testPlanId"

    "should propagate a correlationId to TTP when it's provided one in the request" in {
      stubRequest(
        httpMethod = GET,
        url = s"/debts/time-to-pay/quote/$customerReference/$planId",
        status = 200,
        responseBody = ""
      )

      val request: WSRequest =
        buildRequest(s"/quote/$customerReference/$planId")
          .withHttpHeaders("correlationId" -> testCorrelationId)

      await(request.get())

      verify(
        getRequestedFor(urlEqualTo(s"/debts/time-to-pay/quote/$customerReference/$planId"))
          .withHeader("correlationId", equalTo(testCorrelationId))
      )
    }

    "should generate a new correlationId to send to TTP when it's not provided in the request" in {
      stubRequest(
        httpMethod = GET,
        url = s"/debts/time-to-pay/quote/$customerReference/$planId",
        status = 200,
        responseBody = ""
      )

      val request: WSRequest = buildRequest(s"/quote/$customerReference/$planId")

      request.header("correlationId") shouldBe None

      await(request.get())

      verify(
        getRequestedFor(urlEqualTo(s"/debts/time-to-pay/quote/$customerReference/$planId"))
          .withHeader("correlationId", matching(uuidRegex))
          .withHeader("correlationId", WireMock.not(equalTo(testCorrelationId)))
      )
    }
  }

  ".updatePlan" - {
    val customerReference = "testCustomerReference"
    val planId = "testPlanId"

    val updatePlanRequest: UpdatePlanRequest =
      UpdatePlanRequest(
        CustomerReference(customerReference),
        PlanId(planId),
        UpdateType("updateType"),
        channelIdentifier = None,
        Some(PlanStatus.Success),
        completeReason = None,
        Some(CancellationReason("reason")),
        thirdPartyBank = Some(true),
        payments = Some(
          List(
            PaymentInformation(PaymentMethod.Bacs, Some(PaymentReference("reference")))
          )
        )
      )

    "should propagate a correlationId to TTP when it's provided one in the request" in {
      stubRequest(
        httpMethod = PUT,
        url = s"/debts/time-to-pay/quote/$customerReference/$planId",
        status = 200,
        responseBody = ""
      )

      val request: WSRequest =
        buildRequest(s"/quote/$customerReference/$planId")
          .withHttpHeaders("correlationId" -> testCorrelationId)

      await(
        request.put(Json.toJson(updatePlanRequest))
      )

      verify(
        putRequestedFor(urlEqualTo(s"/debts/time-to-pay/quote/$customerReference/$planId"))
          .withHeader("correlationId", equalTo(testCorrelationId))
      )
    }

    "should generate a new correlationId to send to TTP when it's not provided in the request" in {
      stubRequest(
        httpMethod = PUT,
        url = s"/debts/time-to-pay/quote/$customerReference/$planId",
        status = 200,
        responseBody = ""
      )

      val request: WSRequest = buildRequest(s"/quote/$customerReference/$planId")

      request.header("correlationId") shouldBe None

      await(
        request.put(Json.toJson(updatePlanRequest))
      )

      verify(
        putRequestedFor(urlEqualTo(s"/debts/time-to-pay/quote/$customerReference/$planId"))
          .withHeader("correlationId", matching(uuidRegex))
          .withHeader("correlationId", WireMock.not(equalTo(testCorrelationId)))
      )
    }
  }

  ".createPlan" - {
    val createPlanRequest: CreatePlanRequest = CreatePlanRequest(
      CustomerReference("customerReference"),
      QuoteReference("quoteReference"),
      ChannelIdentifier.Advisor,
      PlanToCreatePlan(
        QuoteId("quoteId"),
        QuoteType.Duration,
        quoteDate = LocalDate.parse("2010-02-02"),
        instalmentStartDate = LocalDate.parse("2010-02-02"),
        instalmentAmount = Some(100),
        PaymentPlanType.TimeToPay,
        thirdPartyBank = false,
        numberOfInstalments = 2,
        Some(FrequencyLowercase.Single),
        Some(Duration(2)),
        Some(PaymentMethod.Bacs),
        Some(PaymentReference("ref123")),
        initialPaymentDate = Some(LocalDate.parse("2010-02-02")),
        initialPaymentAmount = Some(100),
        totalDebtIncInt = 100,
        totalInterest = 10,
        interestAccrued = 10,
        planInterest = 10
      ),
      List(
        CreatePlanDebtItemCharge(
          DebtItemChargeId("debtItemChargeId"),
          mainTrans = "1525",
          subTrans = "1000",
          originalDebtAmount = 100,
          interestStartDate = Some(LocalDate.parse("2010-02-02")),
          List(Payment(LocalDate.parse("2020-01-01"), 100)),
          dueDate = None,
          chargeSource = None,
          parentChargeReference = None,
          parentMainTrans = None
        )
      ),
      List(PaymentInformation(PaymentMethod.Bacs, Some(PaymentReference("ref123")))),
      List(CustomerPostCode(PostCode("NW1 AB1"), postcodeDate = LocalDate.parse("2010-02-02"))),
      List(
        Instalment(
          DebtItemChargeId("id1"),
          dueDate = LocalDate.parse("2010-02-02"),
          amountDue = 100,
          expectedPayment = 100,
          interestRate = 0.24,
          instalmentNumber = 1,
          instalmentInterestAccrued = 10,
          instalmentBalance = 90
        )
      ),
      regimeType = None
    )

    "should propagate a correlationId to TTP when it's provided one in the request" in {
      stubRequest(
        httpMethod = POST,
        url = "/debts/time-to-pay/quote/arrangement",
        status = 200,
        responseBody = ""
      )

      val request: WSRequest =
        buildRequest("/quote/arrangement")
          .withHttpHeaders("correlationId" -> testCorrelationId)

      await(
        request.post(Json.toJson(createPlanRequest))
      )

      verify(
        postRequestedFor(urlEqualTo("/debts/time-to-pay/quote/arrangement"))
          .withHeader("correlationId", equalTo(testCorrelationId))
      )
    }

    "should generate a new correlationId to send to TTP when it's not provided in the request" in {
      stubRequest(
        httpMethod = POST,
        url = "/debts/time-to-pay/quote/arrangement",
        status = 200,
        responseBody = ""
      )

      val request: WSRequest = buildRequest("/quote/arrangement")

      request.header("correlationId") shouldBe None

      await(
        request.post(Json.toJson(createPlanRequest))
      )

      verify(
        postRequestedFor(urlEqualTo("/debts/time-to-pay/quote/arrangement"))
          .withHeader("correlationId", matching(uuidRegex))
          .withHeader("correlationId", WireMock.not(equalTo(testCorrelationId)))
      )
    }
  }

  ".getAffordableQuotes" - {
    val affordableQuotesRequest: AffordableQuotesRequest = AffordableQuotesRequest(
      channelIdentifier = "eSSTTP",
      paymentPlanAffordableAmount = 500,
      paymentPlanFrequency = FrequencyCapitalised.Monthly,
      paymentPlanMaxLength = Duration(6),
      paymentPlanMinLength = Duration(1),
      accruedDebtInterest = 500,
      paymentPlanStartDate = LocalDate.parse("2022-02-02"),
      initialPaymentDate = Some(LocalDate.parse("2022-02-02")),
      initialPaymentAmount = Some(500),
      debtItemCharges = List(
        DebtItemChargeSelfServe(
          outstandingDebtAmount = 100000,
          mainTrans = "1525",
          subTrans = "1000",
          DebtItemChargeId("ChargeRef 0903_2"),
          interestStartDate = Some(LocalDate.parse("2021-09-03")),
          debtItemOriginalDueDate = LocalDate.parse("2010-02-02"),
          IsInterestBearingCharge(true),
          UseChargeReference(false)
        )
      ),
      customerPostcodes = List(
        CustomerPostCode(
          PostCode("some postcode"),
          LocalDate.parse("2022-03-09")
        )
      ),
      regimeType = Some(SsttpRegimeType.SA)
    )

    "should propagate a correlationId to TTP when it's provided one in the request" in {
      stubRequest(
        httpMethod = POST,
        url = "/debts/time-to-pay/affordability/affordable-quotes",
        status = 200,
        responseBody = ""
      )

      val request: WSRequest =
        buildRequest("/self-serve/affordable-quotes")
          .withHttpHeaders("correlationId" -> testCorrelationId)

      await(
        request.post(Json.toJson(affordableQuotesRequest))
      )

      verify(
        postRequestedFor(urlEqualTo("/debts/time-to-pay/affordability/affordable-quotes"))
          .withHeader("correlationId", equalTo(testCorrelationId))
      )
    }

    "should generate a new correlationId to send to TTP when it's not provided in the request" in {
      stubRequest(
        httpMethod = POST,
        url = "/debts/time-to-pay/affordability/affordable-quotes",
        status = 200,
        responseBody = ""
      )

      val request: WSRequest = buildRequest("/self-serve/affordable-quotes")

      request.header("correlationId") shouldBe None

      await(
        request.post(Json.toJson(affordableQuotesRequest))
      )

      verify(
        postRequestedFor(urlEqualTo("/debts/time-to-pay/affordability/affordable-quotes"))
          .withHeader("correlationId", matching(uuidRegex))
          .withHeader("correlationId", WireMock.not(equalTo(testCorrelationId)))
      )
    }
  }

  ".AAAAAAA" - {
    "should propagate a correlationId to TTP when it's provided one in the request" in {

    }

    "should generate a new correlationId to send to TTP when it's not provided in the request" in {

    }
  }
}
