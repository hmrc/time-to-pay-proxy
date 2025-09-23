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

import cats.data.NonEmptyList
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{ postRequestedFor, urlPathEqualTo }
import play.api.libs.json.{ JsNull, JsObject, JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.models.affordablequotes.{ AffordableQuoteResponse, AffordableQuotesRequest }
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.error.TtppErrorResponse
import uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi._
import uk.gov.hmrc.timetopayproxy.models.saonly.common.apistatus.{ ApiErrorResponse, ApiName, ApiStatus, ApiStatusCode }
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel.{ CancellationDate, TtpCancelPaymentPlan, TtpCancelRequest, TtpCancelSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.support.IntegrationBaseSpec
import uk.gov.hmrc.timetopayproxy.testutils.TestOnlyJsonFormats._

import java.time.{ LocalDate, LocalDateTime }
import scala.concurrent.ExecutionContext

class TimeToPayProxyControllerItSpec extends IntegrationBaseSpec {
  def internalAuthEnabled: Boolean = false

  "TimeToPayProxyController" - {
    ".getAffordableQuotes" - {
      val getAffordableQuotesPath: String = "/self-serve/affordable-quotes" // Extracted to prevent copy-paste errors.

      "should return a 200 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns a valid response" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/affordability/affordable-quotes",
              status = 200,
              responseBody = Json.toJson(ttpResponse).toString()
            )

            val requestForAffordableQuotes: WSRequest = buildRequest(getAffordableQuotesPath)

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

              val requestForAffordableQuotes: WSRequest = buildRequest(getAffordableQuotesPath)

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
                TimeToPayError(failures = Seq(TimeToPayInnerError(code = "BAD_REQUEST", reason = "only reason")))

              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
              stubPostWithResponseBody(
                url = "/debts/time-to-pay/affordability/affordable-quotes",
                status = 400,
                responseBody = Json.toJson(timeToPayError).toString()
              )

              val requestForAffordableQuotes: WSRequest = buildRequest(getAffordableQuotesPath)

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
                    TimeToPayInnerError(code = "BAD_REQUEST", reason = "first reason"),
                    TimeToPayInnerError(code = "SERVICE_UNAVAILABLE", reason = "second reason"),
                    TimeToPayInnerError(code = "INTERNAL_SERVER_ERROR", reason = "third reason")
                  )
                )

              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
              stubPostWithResponseBody(
                url = "/debts/time-to-pay/affordability/affordable-quotes",
                status = 400,
                responseBody = Json.toJson(timeToPayError).toString()
              )

              val requestForAffordableQuotes: WSRequest = buildRequest(getAffordableQuotesPath)

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

            val requestForAffordableQuotes: WSRequest = buildRequest(getAffordableQuotesPath)

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

            val requestForAffordableQuotes: WSRequest = buildRequest(getAffordableQuotesPath)

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
                         |			"debtItemOriginalDueDate": "2021-05-22",
                         |      "isInterestBearingCharge": true,
                         |      "useChargeReference": false
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

            val requestForAffordableQuotes: WSRequest = buildRequest(getAffordableQuotesPath)

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

              val requestForAffordableQuotes: WSRequest = buildRequest(getAffordableQuotesPath)

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

    ".checkChargeInfo" - {
      val chargeInfoPath: String = "/charge-info" // Extracted to prevent copy-paste errors.

      "should return a 200 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPayEligibility returns a valid response" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/charge-info",
              status = 200,
              responseBody = Json.toJson(ttpeResponse).toString()
            )

            val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

            val response: WSResponse = await(
              requestForChargeInfo.post(Json.toJson(chargeInfoRequest))
            )

            WireMock.verify(1, postRequestedFor(urlPathEqualTo("/debts/time-to-pay/charge-info")))

            response.json shouldBe Json.toJson(ttpeResponse)
            response.status shouldBe 200
          }
        }
      }

      "should return a 400 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPayEligibility returns an error response of 400" in new TimeToPayProxyControllerTestBase {
            val timeToPayEligibilityError: TimeToPayEligibilityError =
              TimeToPayEligibilityError(code = "BAD_REQUEST", reason = "only reason")

            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/charge-info",
              status = 400,
              responseBody = Json.toJson(timeToPayEligibilityError).toString()
            )

            val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

            val response: WSResponse = await(
              requestForChargeInfo.post(Json.toJson(chargeInfoRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(statusCode = 400, errorMessage = "only reason")

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }
        }

        "when given an invalid json payload" - {
          "with an empty json object" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

            val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

            val response: WSResponse = await(
              requestForChargeInfo.post(JsObject.empty)
            )

            val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage =
                "Invalid ChargeInfoRequest payload: Payload has a missing field or an invalid format. Field name: regimeType. "
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }

          "with mandatory fields missing" - {
            "when 'channelIdentifier' is missing" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

              val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

              val invalidRequestBody: JsValue = Json.parse(
                """{
                  |  "identifications": [
                  |    {
                  |      "idType": "id type 1",
                  |      "idValue": "id value 1"
                  |    },
                  |    {
                  |      "idType": "id type 2",
                  |      "idValue": "id value 2"
                  |    }
                  |  ],
                  |  "regimeType": "SA"
                  |}
                  |""".stripMargin
              )

              val response: WSResponse = await(
                requestForChargeInfo.post(invalidRequestBody)
              )

              val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
                statusCode = 400,
                errorMessage =
                  "Invalid ChargeInfoRequest payload: Payload has a missing field or an invalid format. Field name: channelIdentifier. "
              )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }

            "when 'identifications' is missing" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

              val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

              val invalidRequestBody: JsValue = Json.parse(
                """{
                  |  "channelIdentifier": "Channel Identifier",
                  |  "regimeType": "SA"
                  |}
                  |""".stripMargin
              )

              val response: WSResponse = await(
                requestForChargeInfo.post(invalidRequestBody)
              )

              val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
                statusCode = 400,
                errorMessage =
                  "Invalid ChargeInfoRequest payload: Payload has a missing field or an invalid format. Field name: identifications. "
              )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }

            "when 'regimeType' is missing" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

              val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

              val invalidRequestBody: JsValue = Json.parse(
                """{
                  |  "channelIdentifier": "Channel Identifier",
                  |  "identifications": [
                  |    {
                  |      "idType": "id type 1",
                  |      "idValue": "id value 1"
                  |    },
                  |    {
                  |      "idType": "id type 2",
                  |      "idValue": "id value 2"
                  |    }
                  |  ]
                  |}
                  |""".stripMargin
              )

              val response: WSResponse = await(
                requestForChargeInfo.post(invalidRequestBody)
              )

              val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
                statusCode = 400,
                errorMessage =
                  "Invalid ChargeInfoRequest payload: Payload has a missing field or an invalid format. Field name: regimeType. "
              )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }
          }

          "with invalid types" - {
            "when 'channelIdentifier' is invalid" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

              val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

              val invalidRequestBody: JsValue = Json.parse(
                """{
                  |  "channelIdentifier": 10,
                  |  "identifications": [
                  |    {
                  |      "idType": "id type 1",
                  |      "idValue": "id value 1"
                  |    },
                  |    {
                  |      "idType": "id type 2",
                  |      "idValue": "id value 2"
                  |    }
                  |  ],
                  |  "regimeType": "SA"
                  |}
                  |""".stripMargin
              )

              val response: WSResponse = await(
                requestForChargeInfo.post(invalidRequestBody)
              )

              val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
                statusCode = 400,
                errorMessage =
                  "Invalid ChargeInfoRequest payload: Payload has a missing field or an invalid format. Field name: channelIdentifier. "
              )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }

            "when 'identifications' is invalid" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

              val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

              val invalidRequestBody: JsValue = Json.parse(
                """{
                  |  "channelIdentifier": "Channel Identifier",
                  |  "identifications": "identifications",
                  |  "regimeType": "SA"
                  |}
                  |""".stripMargin
              )

              val response: WSResponse = await(
                requestForChargeInfo.post(invalidRequestBody)
              )

              val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
                statusCode = 400,
                errorMessage =
                  "Invalid ChargeInfoRequest payload: Payload has a missing field or an invalid format. Field name: identifications. "
              )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }

            "when 'regimeType' is invalid" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

              val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

              val invalidRequestBody: JsValue = Json.parse(
                """{
                  |  "channelIdentifier": 10,
                  |  "identifications": [
                  |    {
                  |      "idType": "id type 1",
                  |      "idValue": "id value 1"
                  |    },
                  |    {
                  |      "idType": "id type 2",
                  |      "idValue": "id value 2"
                  |    }
                  |  ],
                  |  "regimeType": true
                  |}
                  |""".stripMargin
              )

              val response: WSResponse = await(
                requestForChargeInfo.post(invalidRequestBody)
              )

              val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
                statusCode = 400,
                errorMessage =
                  "Invalid ChargeInfoRequest payload: Payload has a missing field or an invalid format. Field name: regimeType. "
              )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }
          }
        }
      }

      "should return a 503 statusCode" - {
        "when given a valid json payload" - {
          for (responseStatus <- List(200, 400)) s"when TimeToPayEligibility returns a $responseStatus response" - {
            "with a null json response from TTPE" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
              stubPostWithResponseBody(
                url = "/debts/time-to-pay/charge-info",
                status = responseStatus,
                responseBody = JsNull.toString()
              )

              val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

              val response: WSResponse = await(
                requestForChargeInfo.post(Json.toJson(chargeInfoRequest))
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

    ".cancelTtp" - {
      "should return a 200 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns a successful response" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/cancel",
              status = 200,
              responseBody = Json.toJson(cancelResponse).toString()
            )

            val requestForCancelPlan: WSRequest = buildRequest("/cancel")

            val response: WSResponse = await(
              requestForCancelPlan.post(Json.toJson(cancelRequest))
            )

            response.json shouldBe Json.toJson(cancelResponse)
            response.status shouldBe 200
          }
        }
      }

      "should return a 500 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns an error response with 500" in new TimeToPayProxyControllerTestBase {
            val errorResponse = TtpCancelSuccessfulResponse(
              apisCalled = List(
                ApiStatus(
                  name = ApiName("CESA"),
                  statusCode = ApiStatusCode("400"),
                  processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:30:00Z")),
                  errorResponse = Some(ApiErrorResponse("Invalid cancellationDate"))
                )
              ),
              processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:31:00Z"))
            )

            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/cancel",
              status = 500,
              responseBody = Json.toJson(errorResponse).toString()
            )

            val requestForCancelPlan: WSRequest = buildRequest("/cancel")

            val response: WSResponse = await(
              requestForCancelPlan.post(Json.toJson(cancelRequest))
            )

            response.json shouldBe Json.toJson(errorResponse)
            response.status shouldBe 500
          }
        }
      }

      "should return a 400 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns an error response of 400" in new TimeToPayProxyControllerTestBase {
            val upstreamErrorResponse = TimeToPayError(
              List(
                TimeToPayInnerError(
                  code = "400",
                  reason = "Invalid request payload: missing identifications or cancellationDate"
                )
              )
            )

            val expectedTtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage = "Invalid request payload: missing identifications or cancellationDate"
            )

            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/cancel",
              status = 400,
              responseBody = Json.toJson(upstreamErrorResponse).toString()
            )

            val requestForCancelPlan: WSRequest = buildRequest("/cancel")

            val response: WSResponse = await(
              requestForCancelPlan.post(Json.toJson(cancelRequest))
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }
        }

        "when given an invalid json payload" - {
          "with an empty json object" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

            val requestForCancelPlan: WSRequest = buildRequest("/cancel")

            val response: WSResponse = await(
              requestForCancelPlan.post(JsObject.empty)
            )

            val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage =
                "Invalid TtpCancelRequest payload: Payload has a missing field or an invalid format. Field name: identifications. "
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }

          "with mandatory fields missing" - {
            "when 'identifications' is missing" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

              val requestForCancelPlan: WSRequest = buildRequest("/cancel")

              val invalidRequestBody: JsValue = Json.parse(
                """{
                  |  "paymentPlan": {
                  |    "planSelection": 1,
                  |    "paymentDay": 28,
                  |    "upfrontPaymentAmount": 123.45,
                  |    "startDate": "2025-10-15"
                  |  },
                  |  "channelIdentifier": "eSSTTP"
                  |}
                  |""".stripMargin
              )

              val response: WSResponse = await(
                requestForCancelPlan.post(invalidRequestBody)
              )

              val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
                statusCode = 400,
                errorMessage =
                  "Invalid TtpCancelRequest payload: Payload has a missing field or an invalid format. Field name: cancellationDate. "
              )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }
          }
        }
      }

      "should return a 503 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns an expected 200 response" - {
            "with a null json response from TTP" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
              stubPostWithResponseBody(
                url = "/debts/time-to-pay/cancel",
                status = 200,
                responseBody = JsNull.toString()
              )

              val requestForCancelPlan: WSRequest = buildRequest("/cancel")

              val response: WSResponse = await(
                requestForCancelPlan.post(Json.toJson(cancelRequest))
              )

              val expectedTtppErrorResponse: TtppErrorResponse =
                TtppErrorResponse(
                  statusCode = 503,
                  errorMessage = "JSON structure is not valid in received successful HTTP response."
                )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 503
            }
          }

          "when TimeToPay returns an expected error response" - {
            for (responseStatus <- List(400, 500))
              s"<$responseStatus>" - {
                "with a null json response from TTP" in new TimeToPayProxyControllerTestBase {
                  stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
                  stubPostWithResponseBody(
                    url = "/debts/time-to-pay/cancel",
                    status = responseStatus,
                    responseBody = JsNull.toString()
                  )

                  val requestForCancelPlan: WSRequest = buildRequest("/cancel")

                  val response: WSResponse = await(
                    requestForCancelPlan.post(Json.toJson(cancelRequest))
                  )

                  val expectedTtppErrorResponse: TtppErrorResponse =
                    TtppErrorResponse(
                      statusCode = 503,
                      errorMessage = "JSON structure is not valid in received error HTTP response."
                    )

                  response.json shouldBe Json.toJson(expectedTtppErrorResponse)
                  response.status shouldBe 503
                }
              }
          }

          "when TimeToPay returns unexpected success status" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/cancel",
              status = 201,
              responseBody = Json.obj().toString()
            )

            val requestForCancelPlan: WSRequest = buildRequest("/cancel")

            val response: WSResponse = await(
              requestForCancelPlan.post(Json.toJson(cancelRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(statusCode = 503, errorMessage = "HTTP status is unexpected in received HTTP response.")

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }

          "when TimeToPay returns unexpected error status" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/cancel",
              status = 403,
              responseBody = Json.obj().toString()
            )

            val requestForCancelPlan: WSRequest = buildRequest("/cancel")

            val response: WSResponse = await(
              requestForCancelPlan.post(Json.toJson(cancelRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(statusCode = 503, errorMessage = "HTTP status is unexpected in received HTTP response.")

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }
        }
      }
    }

    ".informTtp" - {
      "should return a 200 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns a successful response" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/inform",
              status = 200,
              responseBody = Json.toJson(informResponse).toString()
            )

            val requestForInformTtp: WSRequest = buildRequest("/inform")

            val response: WSResponse = await(
              requestForInformTtp.post(Json.toJson(informRequest))
            )

            response.json shouldBe Json.toJson(informResponse)
            response.status shouldBe 200
          }
        }
      }

      "should return a 500 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns an error response with 500" in new TimeToPayProxyControllerTestBase {
            val errorResponse = TtpInformInformativeError(
              apisCalled = List(
                ApiStatus(
                  name = ApiName("CESA"),
                  statusCode = ApiStatusCode("400"),
                  processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:30:00Z")),
                  errorResponse = Some(ApiErrorResponse("Invalid cancellationDate"))
                )
              ),
              processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:31:00Z"))
            )

            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/inform",
              status = 500,
              responseBody = Json.toJson(errorResponse).toString()
            )

            val requestForInformTtp: WSRequest = buildRequest("/inform")

            val response: WSResponse = await(
              requestForInformTtp.post(Json.toJson(informRequest))
            )

            response.json shouldBe Json.toJson(errorResponse)
            response.status shouldBe 500
          }
        }
      }

      "should return a 400 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns an error response of 400" in new TimeToPayProxyControllerTestBase {
            val upstreamErrorResponse = TtpInformGeneralFailureResponse(
              code = 400,
              details = "Invalid request payload: missing identifications or cancellationDate"
            )

            val expectedTtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage = "Invalid request payload: missing identifications or cancellationDate"
            )

            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/inform",
              status = 400,
              responseBody = Json.toJson(upstreamErrorResponse).toString()
            )

            val requestForInformTtp: WSRequest = buildRequest("/inform")

            val response: WSResponse = await(
              requestForInformTtp.post(Json.toJson(informRequest))
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }
        }

        "when given an invalid json payload" - {
          "with an empty json object" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

            val requestForInformTtp: WSRequest = buildRequest("/inform")

            val response: WSResponse = await(
              requestForInformTtp.post(JsObject.empty)
            )

            val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage =
                "Invalid TtpInformRequest payload: Payload has a missing field or an invalid format. Field name: identifications. "
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }

          "with mandatory fields missing" - {
            "when 'identifications' is missing" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")

              val requestForInformTtp: WSRequest = buildRequest("/inform")

              val invalidRequestBody: JsValue = Json.parse(
                """{
                  |  "paymentPlan": {
                  |    "planSelection": 1,
                  |    "paymentDay": 28,
                  |    "upfrontPaymentAmount": 123.45,
                  |    "startDate": "2025-10-15"
                  |  },
                  |  "channelIdentifier": "eSSTTP"
                  |}
                  |""".stripMargin
              )

              val response: WSResponse = await(
                requestForInformTtp.post(invalidRequestBody)
              )

              val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
                statusCode = 400,
                errorMessage =
                  "Invalid TtpInformRequest payload: Payload has a missing field or an invalid format. Field name: identifications. "
              )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }
          }
        }
      }

      "should return a 503 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns an expected 200 response" - {
            "with a null json response from TTP" in new TimeToPayProxyControllerTestBase {
              stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
              stubPostWithResponseBody(
                url = "/debts/time-to-pay/inform",
                status = 200,
                responseBody = JsNull.toString()
              )

              val requestForInformTtp: WSRequest = buildRequest("/inform")

              val response: WSResponse = await(
                requestForInformTtp.post(Json.toJson(informRequest))
              )

              val expectedTtppErrorResponse: TtppErrorResponse =
                TtppErrorResponse(
                  statusCode = 503,
                  errorMessage = "JSON structure is not valid in successful upstream response."
                )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 503
            }
          }

          "when TimeToPay returns an expected error response" - {
            for (responseStatus <- List(400, 500))
              s"<$responseStatus>" - {
                "with a null json response from TTP" in new TimeToPayProxyControllerTestBase {
                  stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
                  stubPostWithResponseBody(
                    url = "/debts/time-to-pay/inform",
                    status = responseStatus,
                    responseBody = JsNull.toString()
                  )

                  val requestForInformTtp: WSRequest = buildRequest("/inform")

                  val response: WSResponse = await(
                    requestForInformTtp.post(Json.toJson(informRequest))
                  )

                  val expectedTtppErrorResponse: TtppErrorResponse =
                    TtppErrorResponse(
                      statusCode = 503,
                      errorMessage = "JSON structure is not valid in error upstream response."
                    )

                  response.json shouldBe Json.toJson(expectedTtppErrorResponse)
                  response.status shouldBe 503
                }
              }
          }

          "when TimeToPay returns unexpected success status" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/inform",
              status = 201,
              responseBody = Json.obj().toString()
            )

            val requestForInformTtp: WSRequest = buildRequest("/inform")

            val response: WSResponse = await(
              requestForInformTtp.post(Json.toJson(informRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(statusCode = 503, errorMessage = "Upstream response status is unexpected.")

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }

          "when TimeToPay returns unexpected error status" in new TimeToPayProxyControllerTestBase {
            stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
            stubPostWithResponseBody(
              url = "/debts/time-to-pay/inform",
              status = 403,
              responseBody = Json.obj().toString()
            )

            val requestForInformTtp: WSRequest = buildRequest("/inform")

            val response: WSResponse = await(
              requestForInformTtp.post(Json.toJson(informRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(statusCode = 503, errorMessage = "Upstream response status is unexpected.")

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
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
          isInterestBearingCharge = IsInterestBearingCharge(true),
          useChargeReference = UseChargeReference(false)
        )
      ),
      customerPostcodes = List(),
      regimeType = Some(SsttpRegimeType.SA)
    )

    val ttpeResponse: ChargeInfoResponse = ChargeInfoResponse(
      processingDateTime = LocalDateTime.parse("2025-07-02T15:00:41.689"),
      identification = List(
        Identification(idType = IdType("ID_TYPE"), idValue = IdValue("ID_VALUE"))
      ),
      individualDetails = IndividualDetails(
        title = Some(Title("Mr")),
        firstName = Some(FirstName("John")),
        lastName = Some(LastName("Doe")),
        dateOfBirth = Some(DateOfBirth(LocalDate.parse("1980-01-01"))),
        districtNumber = Some(DistrictNumber("1234")),
        customerType = CustomerType.ItsaMigtrated,
        transitionToCDCS = TransitionToCdcs(value = true)
      ),
      addresses = List(
        Address(
          addressType = AddressType("Address Type"),
          addressLine1 = AddressLine1("Address Line 1"),
          addressLine2 = Some(AddressLine2("Address Line 2")),
          addressLine3 = Some(AddressLine3("Address Line 3")),
          addressLine4 = Some(AddressLine4("Address Line 4")),
          rls = Some(Rls(true)),
          contactDetails = Some(
            ContactDetails(
              telephoneNumber = Some(TelephoneNumber("telephone-number")),
              fax = Some(Fax("fax-number")),
              mobile = Some(Mobile("mobile-number")),
              emailAddress = Some(Email("email address")),
              emailSource = Some(EmailSource("email source"))
            )
          ),
          postCode = Some(ChargeInfoPostCode("AB12 3CD")),
          postcodeHistory = List(
            PostCodeInfo(addressPostcode = ChargeInfoPostCode("AB12 3CD"), postcodeDate = LocalDate.parse("2020-01-01"))
          )
        )
      ),
      chargeTypeAssessment = List(
        ChargeTypeAssessment(
          debtTotalAmount = BigInt(1000),
          chargeReference = ChargeReference("CHARGE REFERENCE"),
          parentChargeReference = Some(ChargeInfoParentChargeReference("PARENT CHARGE REF")),
          mainTrans = MainTrans("2000"),
          charges = List(
            Charge(
              taxPeriodFrom = TaxPeriodFrom(LocalDate.parse("2020-01-02")),
              taxPeriodTo = TaxPeriodTo(LocalDate.parse("2020-12-31")),
              chargeType = ChargeType("charge type"),
              mainType = MainType("main type"),
              subTrans = SubTrans("1000"),
              outstandingAmount = OutstandingAmount(BigInt(500)),
              dueDate = DueDate(LocalDate.parse("2021-01-31")),
              isInterestBearingCharge = Some(ChargeInfoIsInterestBearingCharge(true)),
              interestStartDate = Some(InterestStartDate(LocalDate.parse("2020-01-03"))),
              accruedInterest = AccruedInterest(BigInt(50)),
              chargeSource = ChargeInfoChargeSource("Source"),
              parentMainTrans = Some(ChargeInfoParentMainTrans("Parent Main Transaction")),
              originalCreationDate = Some(OriginalCreationDate(LocalDate.parse("2025-07-02"))),
              tieBreaker = Some(TieBreaker("Tie Breaker")),
              originalTieBreaker = Some(OriginalTieBreaker("Original Tie Breaker")),
              saTaxYearEnd = Some(SaTaxYearEnd(LocalDate.parse("2020-04-05"))),
              creationDate = Some(CreationDate(LocalDate.parse("2025-07-02"))),
              originalChargeType = Some(OriginalChargeType("Original Charge Type"))
            )
          )
        )
      )
    )

    val chargeInfoRequest: ChargeInfoRequest = ChargeInfoRequest(
      channelIdentifier = ChargeInfoChannelIdentifier("Channel Identifier"),
      identifications = NonEmptyList.of(
        Identification(idType = IdType("id type 1"), idValue = IdValue("id value 1")),
        Identification(idType = IdType("id type 2"), idValue = IdValue("id value 2"))
      ),
      regimeType = SaOnlyRegimeType.SA
    )

    val cancelResponse: TtpCancelSuccessfulResponse = TtpCancelSuccessfulResponse(
      apisCalled = List(
        ApiStatus(
          name = ApiName("CESA"),
          statusCode = ApiStatusCode("200"),
          processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-05-01T14:30:00Z")),
          errorResponse = None
        ),
        ApiStatus(
          name = ApiName("ETMP"),
          statusCode = ApiStatusCode("201"),
          processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-05-01T14:31:00Z")),
          errorResponse = None
        )
      ),
      processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:31:00Z"))
    )

    val cancelRequest: TtpCancelRequest = TtpCancelRequest(
      identifications = NonEmptyList.of(
        Identification(idType = IdType("NINO"), idValue = IdValue("AA000000A")),
        Identification(idType = IdType("MTDITID"), idValue = IdValue("XAIT00000000054"))
      ),
      paymentPlan = TtpCancelPaymentPlan(
        arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2025-01-01")),
        ttpEndDate = TtpEndDate(LocalDate.parse("2025-12-31")),
        frequency = FrequencyLowercase.Monthly,
        cancellationDate = CancellationDate(LocalDate.parse("2025-10-15")),
        initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2025-02-01"))),
        initialPaymentAmount = Some(GbpPounds.createOrThrow(BigDecimal("123.45")))
      ),
      instalments = NonEmptyList.of(
        SaOnlyInstalment(
          dueDate = InstalmentDueDate(LocalDate.parse("2025-11-28")),
          amountDue = GbpPounds.createOrThrow(BigDecimal("100.00"))
        )
      ),
      channelIdentifier = ChannelIdentifier.SelfService,
      transitioned = Some(TransitionedIndicator(true))
    )

    val informRequest: TtpInformRequest = TtpInformRequest(
      identifications = NonEmptyList.of(
        Identification(idType = IdType("NINO"), idValue = IdValue("AB123456C"))
      ),
      paymentPlan = TtpInformPaymentPlan(
        arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2025-01-01")),
        ttpEndDate = TtpEndDate(LocalDate.parse("2025-02-01")),
        frequency = FrequencyLowercase.Monthly,
        initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2025-01-05"))),
        initialPaymentAmount = Some(GbpPoundsUnchecked(100.00)),
        ddiReference = Some(DdiReference("TestDDIReference"))
      ),
      instalments = NonEmptyList.of(
        SaOnlyInstalment(
          dueDate = InstalmentDueDate(LocalDate.parse("2025-01-31")),
          amountDue = GbpPoundsUnchecked(500.00)
        )
      ),
      channelIdentifier = ChannelIdentifier.Advisor,
      transitioned = Some(TransitionedIndicator(true))
    )

    val informResponse: TtpInformSuccessfulResponse = TtpInformSuccessfulResponse(
      apisCalled = List(
        ApiStatus(
          name = ApiName("CESA"),
          statusCode = ApiStatusCode("200"),
          processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-05-01T14:30:00Z")),
          errorResponse = None
        ),
        ApiStatus(
          name = ApiName("ETMP"),
          statusCode = ApiStatusCode("201"),
          processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-05-01T14:31:00Z")),
          errorResponse = None
        )
      ),
      processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:31:00Z"))
    )
  }
}
