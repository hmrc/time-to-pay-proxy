/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.{ JsNull, JsObject, JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.affordablequotes.{ AffordableQuoteResponse, AffordableQuotesRequest }
import uk.gov.hmrc.timetopayproxy.support.IntegrationBaseSpec

import java.time.{ LocalDate, LocalDateTime }
import scala.concurrent.ExecutionContext

class TimeToPayProxyControllerItSpec extends IntegrationBaseSpec {

  "TimeToPayProxyController" - {
    ".getAffordableQuotes" - {
      "should return a 200 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns a valid response" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/affordability/affordable-quotes",
              status = 200,
              responseBody = Json.toJson(ttpResponse).toString()
            )

            val requestForAffordableQuotes: WSRequest = buildRequest("/self-serve/affordable-quotes")

            val response: WSResponse = await(
              requestForAffordableQuotes.post(Json.toJson(affordableQuoteRequest))
            )

            response.json shouldBe Json.toJson(ttpResponse)
            response.status shouldBe 200
          }
        }
      }

      "should return a 400 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns an error response of 400" - {
            "with an empty list of 'failures'" in new TimeToPayProxyControllerTestBase {
              val timeToPayError: TimeToPayError = TimeToPayError(failures = Seq.empty)

              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
              stubPostWithResponseBody(
                url = "/debts/time-to-pay/affordability/affordable-quotes",
                status = 400,
                responseBody = Json.toJson(timeToPayError).toString()
              )

              val requestForAffordableQuotes: WSRequest = buildRequest("/self-serve/affordable-quotes")

              val response: WSResponse = await(
                requestForAffordableQuotes.post(Json.toJson(affordableQuoteRequest))
              )

              val expectedTtppErrorResponse: TtppErrorResponse =
                TtppErrorResponse(statusCode = 400, errorMessage = "An unknown error has occurred")

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }

            "with one item in the list of 'failures'" in new TimeToPayProxyControllerTestBase {
              val timeToPayError: TimeToPayError =
                TimeToPayError(failures = Seq(Error(code = "BAD_REQUEST", reason = "only reason")))

              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
              stubPostWithResponseBody(
                url = "/debts/time-to-pay/affordability/affordable-quotes",
                status = 400,
                responseBody = Json.toJson(timeToPayError).toString()
              )

              val requestForAffordableQuotes: WSRequest = buildRequest("/self-serve/affordable-quotes")

              val response: WSResponse = await(
                requestForAffordableQuotes.post(Json.toJson(affordableQuoteRequest))
              )

              val expectedTtppErrorResponse: TtppErrorResponse =
                TtppErrorResponse(statusCode = 400, errorMessage = "only reason")

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }

            "with multiple items in the list of 'failures', TTP-Proxy should return the first message" in new TimeToPayProxyControllerTestBase {
              val timeToPayError: TimeToPayError =
                TimeToPayError(failures =
                  Seq(
                    Error(code = "BAD_REQUEST", reason = "first reason"),
                    Error(code = "SERVICE_UNAVAILABLE", reason = "second reason"),
                    Error(code = "INTERNAL_SERVER_ERROR", reason = "third reason")
                  )
                )

              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
              stubPostWithResponseBody(
                url = "/debts/time-to-pay/affordability/affordable-quotes",
                status = 400,
                responseBody = Json.toJson(timeToPayError).toString()
              )

              val requestForAffordableQuotes: WSRequest = buildRequest("/self-serve/affordable-quotes")

              val response: WSResponse = await(
                requestForAffordableQuotes.post(Json.toJson(affordableQuoteRequest))
              )

              val expectedTtppErrorResponse: TtppErrorResponse =
                TtppErrorResponse(statusCode = 400, errorMessage = "first reason")

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }
          }
        }

        "when given an invalid json payload" - {
          "with an empty json object" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

            val requestForAffordableQuotes: WSRequest = buildRequest("/self-serve/affordable-quotes")

            val response: WSResponse = await(
              requestForAffordableQuotes.post(JsObject.empty)
            )

            val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage =
                "Invalid AffordableQuotesRequest payload: Payload has a missing field or an invalid format. Field name: debtItemCharges. "
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }

          "with mandatory fields missing" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

            val requestForAffordableQuotes: WSRequest = buildRequest("/self-serve/affordable-quotes")

            val invalidRequestBody: JsValue =
              Json
                .parse("""{
                         |	"channelIdentifier": "eSSTTP",
                         |	"paymentPlanAffordableAmount": 1,
                         |	"paymentPlanFrequency": "Monthly",
                         |	"paymentPlanMaxLength": 6,
                         |	"paymentPlanMinLength": 1,
                         |	"accruedDebtInterest": 1,
                         |	"paymentPlanStartDate": "2022-05-30",
                         |	"initialPaymentDate": "2022-05-30",
                         |	"initialPaymentAmount": 8999,
                         |	"debtItemCharges": [
                         |		{
                         |			"mainTrans": "1545",
                         |			"subTrans": "2000",
                         |			"outstandingDebtAmount": 9000,
                         |			"interestStartDate": "2022-08-02",
                         |			"debtItemOriginalDueDate": "2021-05-22"
                         |		}
                         |	],
                         |	"customerPostcodes": [
                         |		{
                         |			"addressPostcode": "TW3 4QQ",
                         |			"postcodeDate": "2019-07-06"
                         |		}
                         |	]
                         |}""".stripMargin)
                .as[JsObject]

            val response: WSResponse = await(
              requestForAffordableQuotes.post(invalidRequestBody)
            )

            val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage =
                "Invalid AffordableQuotesRequest payload: Payload has a missing field or an invalid format. Field name: debtItemChargeId. "
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }

          "with invalid types" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

            val requestForAffordableQuotes: WSRequest = buildRequest("/self-serve/affordable-quotes")

            val invalidRequestBody: JsValue =
              Json
                .parse("""{
                         |	"channelIdentifier": "eSSTTP",
                         |	"paymentPlanAffordableAmount": 1,
                         |	"paymentPlanFrequency": "Monthly",
                         |	"paymentPlanMaxLength": true,
                         |	"paymentPlanMinLength": 1,
                         |	"accruedDebtInterest": 1,
                         |	"paymentPlanStartDate": "2022-05-30",
                         |	"initialPaymentDate": "2022-05-30",
                         |	"initialPaymentAmount": 8999,
                         |	"debtItemCharges": [
                         |		{
                         |  		"debtItemChargeId": "ChargeRef 0745_1",
                         |			"mainTrans": "1545",
                         |			"subTrans": "2000",
                         |			"outstandingDebtAmount": 9000,
                         |			"interestStartDate": "2022-08-02",
                         |			"debtItemOriginalDueDate": "2021-05-22"
                         |		}
                         |	],
                         |	"customerPostcodes": [
                         |		{
                         |			"addressPostcode": "TW3 4QQ",
                         |			"postcodeDate": "2019-07-06"
                         |		}
                         |	]
                         |}""".stripMargin)
                .as[JsObject]

            val response: WSResponse = await(
              requestForAffordableQuotes.post(invalidRequestBody)
            )

            val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage =
                "Invalid AffordableQuotesRequest payload: Payload has a missing field or an invalid format. Field name: paymentPlanMaxLength. "
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }
        }
      }

      "should return a 503 statusCode" - {
        "when given a valid json payload" - {
          for (responseStatus <- List(200, 400)) s"when TimeToPay returns a $responseStatus response" - {
            "with a null json response from TTP" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
              stubPostWithResponseBody(
                url = "/debts/time-to-pay/affordability/affordable-quotes",
                status = responseStatus,
                responseBody = JsNull.toString()
              )

              val requestForAffordableQuotes: WSRequest = buildRequest("/self-serve/affordable-quotes")

              val response: WSResponse = await(
                requestForAffordableQuotes.post(Json.toJson(affordableQuoteRequest))
              )

              val expectedTtppErrorResponse: TtppErrorResponse =
                TtppErrorResponse(statusCode = 503, errorMessage = "Couldn't parse body from upstream")

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 503
            }
          }
        }
      }
    }
  }

  trait TimeToPayProxyControllerTestBase {
    implicit def ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val ttpResponse: AffordableQuoteResponse =
      AffordableQuoteResponse(LocalDateTime.parse("2025-01-13T10:15:30.975"), Nil)

    val affordableQuoteRequest: AffordableQuotesRequest = AffordableQuotesRequest(
      channelIdentifier = "eSSTTP",
      paymentPlanAffordableAmount = 10,
      paymentPlanFrequency = FrequencyCapitalised.Monthly,
      paymentPlanMinLength = Duration(3),
      paymentPlanMaxLength = Duration(5),
      accruedDebtInterest = 10,
      paymentPlanStartDate = LocalDate.now(),
      initialPaymentDate = None,
      initialPaymentAmount = None,
      debtItemCharges = List(
        DebtItemChargeSelfServe(
          outstandingDebtAmount = BigDecimal(200),
          mainTrans = "1234",
          subTrans = "1234",
          debtItemChargeId = DebtItemChargeId("dici1"),
          interestStartDate = Some(LocalDate.now()),
          debtItemOriginalDueDate = LocalDate.now(),
          isInterestBearingCharge = Some(IsInterestBearingCharge(true)),
          useChargeReference = Some(UseChargeReference(false))
        )
      ),
      customerPostcodes = List()
    )
  }
}
