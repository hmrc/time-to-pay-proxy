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
import com.github.tomakehurst.wiremock.client.WireMock.{ post, postRequestedFor, put, urlPathEqualTo }
import com.github.tomakehurst.wiremock.http.RequestMethod.{ POST, PUT }
import play.api.libs.json.{ JsNull, JsObject, JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.affordablequotes.{ AffordableQuoteResponse, AffordableQuotesRequest }
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.error.TtppErrorResponse
import uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi._
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.models.saonly.common.apistatus._
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel._
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpfullamend.{ TtpFullAmendInformativeError, TtpFullAmendInternalError, TtpFullAmendRequest, TtpFullAmendSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform._
import uk.gov.hmrc.timetopayproxy.support.IntegrationBaseSpec
import uk.gov.hmrc.timetopayproxy.testutils.TestOnlyJsonFormats._

import java.time.{ LocalDate, LocalDateTime }
import scala.concurrent.ExecutionContext

class TimeToPayProxyControllerItSpec extends IntegrationBaseSpec {
  def internalAuthEnabled: Boolean = false
  def enrolmentAuthEnabled: Boolean = false

  "TimeToPayProxyController" - {
    ".getAffordableQuotes" - {
      val getAffordableQuotesPath: String = "/self-serve/affordable-quotes" // Extracted to prevent copy-paste errors.

      "should return a 200 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns a valid response" in new TimeToPayProxyControllerTestBase {
            stubRequest(
              httpMethod = POST,
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

              stubRequest(
                httpMethod = POST,
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

              stubRequest(
                httpMethod = POST,
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

              stubRequest(
                httpMethod = POST,
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
          "when TimeToPay returns a 200 response" - {
            "with a null json response from TTP" in new TimeToPayProxyControllerTestBase {
              stubRequest(
                httpMethod = POST,
                url = "/debts/time-to-pay/affordability/affordable-quotes",
                status = 200,
                responseBody = JsNull.toString()
              )

              val requestForAffordableQuotes: WSRequest = buildRequest(getAffordableQuotesPath)

              val response: WSResponse = await(
                requestForAffordableQuotes.post(Json.toJson(affordableQuoteRequest))
              )

              val expectedTtppErrorResponse: TtppErrorResponse =
                TtppErrorResponse(
                  statusCode = 503,
                  errorMessage =
                    "Received status code 200 with incorrect JSON body for request: POST http://localhost:11111/debts/time-to-pay/affordability/affordable-quotes"
                )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 503
            }
          }

          "when TimeToPay returns a 400 response" - {
            "with a null json response from TTP" in new TimeToPayProxyControllerTestBase {
              stubRequest(
                httpMethod = POST,
                url = "/debts/time-to-pay/affordability/affordable-quotes",
                status = 400,
                responseBody = JsNull.toString()
              )

              val requestForAffordableQuotes: WSRequest = buildRequest(getAffordableQuotesPath)

              val response: WSResponse = await(
                requestForAffordableQuotes.post(Json.toJson(affordableQuoteRequest))
              )

              val expectedTtppErrorResponse: TtppErrorResponse =
                TtppErrorResponse(
                  statusCode = 503,
                  errorMessage = """Status code 400: JSON structure is not valid in received error HTTP response.
                                   |  Validation errors:
                                   |    - For path  , errors: [error.expected.jsobject].""".stripMargin
                )

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
            stubRequest(
              httpMethod = POST,
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

            stubRequest(
              httpMethod = POST,
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
        "when given a valid json payload with response code 200" - {
          "with a null json response from TTPE" in new TimeToPayProxyControllerTestBase {
            stubRequest(
              httpMethod = POST,
              url = "/debts/time-to-pay/charge-info",
              status = 200,
              responseBody = JsNull.toString()
            )

            val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

            val response: WSResponse = await(
              requestForChargeInfo.post(Json.toJson(chargeInfoRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 503,
                errorMessage =
                  "Received status code 200 with incorrect JSON body for request: POST http://localhost:11111/debts/time-to-pay/charge-info"
              )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }

        }
        "when given a valid json payload with response code 400" - {
          "with a null json response from TTPE" in new TimeToPayProxyControllerTestBase {
            stubRequest(
              httpMethod = POST,
              url = "/debts/time-to-pay/charge-info",
              status = 400,
              responseBody = JsNull.toString()
            )

            val requestForChargeInfo: WSRequest = buildRequest(chargeInfoPath)

            val response: WSResponse = await(
              requestForChargeInfo.post(Json.toJson(chargeInfoRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 503,
                errorMessage =
                  "Status code 400: JSON structure is not valid in received error HTTP response.\n  Validation errors:\n    - For path  , errors: [error.expected.jsobject]."
              )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }

        }
      }
    }

    ".cancelTtp" - {
      "should return a 200 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns a successful response" in new TimeToPayProxyControllerTestBase {
            stubRequest(
              httpMethod = POST,
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
            val errorResponse: TtpCancelInformativeError = TtpCancelInformativeError(
              apisCalled = Some(
                List(
                  ApiStatus(
                    name = ApiName("CESA"),
                    statusCode = ApiStatusCode(400),
                    processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:30:00Z")),
                    errorResponse = Some(ApiErrorResponse("Invalid cancellationDate"))
                  )
                )
              ),
              internalErrors = List(
                TtpCancelInternalError("some error that ttp is responsible for"),
                TtpCancelInternalError("another error that ttp is responsible for")
              ),
              processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:31:00Z"))
            )

            stubRequest(
              httpMethod = POST,
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

            stubRequest(
              httpMethod = POST,
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

          "when 'cancellationDate' is invalid" in new TimeToPayProxyControllerTestBase {

            val requestForCancelPlan: WSRequest = buildRequest("/cancel")

            val invalidRequestBody: JsValue = Json.parse(
              """{
                |  "identifications": [
                |    {
                |      "idType": "NINO",
                |      "idValue": "AA000000A"
                |    },
                |    {
                |      "idType": "MTDITID",
                |      "idValue": "XAIT00000000054"
                |    }
                |  ],
                |  "paymentPlan": {
                |    "arrangementAgreedDate": "2025-01-01",
                |    "ttpEndDate": "2025-12-31",
                |    "frequency": "monthly",
                |    "cancellationDate": "invalid",
                |    "initialPaymentDate": "2025-02-01",
                |    "initialPaymentAmount": 123.45
                |  },
                |  "instalments": [
                |    {
                |      "dueDate": "2025-11-28",
                |      "amountDue": 100
                |    }
                |  ],
                |  "channelIdentifier": "selfService",
                |  "transitioned": true
                |}
                |""".stripMargin
            )

            val response: WSResponse = await(
              requestForCancelPlan.post(invalidRequestBody)
            )

            val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage =
                "Invalid TtpCancelRequest payload: Payload has a missing field or an invalid format. Field name: cancellationDate. Date format should be correctly provided"
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }

          "with mandatory fields missing" - {
            "when 'identifications' is missing" in new TimeToPayProxyControllerTestBase {

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
              stubRequest(
                httpMethod = POST,
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
                  errorMessage =
                    "Received status code 200 with incorrect JSON body for request: POST http://localhost:11111/debts/time-to-pay/cancel"
                )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 503
            }
          }

          "when TimeToPay returns an expected error response" - {
            for (responseStatus <- List(400, 500))
              s"<$responseStatus>" - {
                "with a null json response from TTP" in new TimeToPayProxyControllerTestBase {
                  stubRequest(
                    httpMethod = POST,
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
                      errorMessage =
                        s"""Status code $responseStatus: JSON structure is not valid in received error HTTP response.
                           |  Validation errors:
                           |    - For path  , errors: [error.expected.jsobject].""".stripMargin
                    )

                  response.json shouldBe Json.toJson(expectedTtppErrorResponse)
                  response.status shouldBe 503
                }
              }
          }

          "when TimeToPay returns unexpected success status" in new TimeToPayProxyControllerTestBase {
            stubRequest(
              httpMethod = POST,
              url = "/debts/time-to-pay/cancel",
              status = 201,
              responseBody = Json.obj().toString()
            )

            val requestForCancelPlan: WSRequest = buildRequest("/cancel")

            val response: WSResponse = await(
              requestForCancelPlan.post(Json.toJson(cancelRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 503,
                errorMessage = "Status code 201: HTTP status is unexpected in received HTTP response."
              )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }

          "when TimeToPay returns unexpected error status" in new TimeToPayProxyControllerTestBase {
            stubRequest(
              httpMethod = POST,
              url = "/debts/time-to-pay/cancel",
              status = 403,
              responseBody = Json.obj().toString()
            )

            val requestForCancelPlan: WSRequest = buildRequest("/cancel")

            val response: WSResponse = await(
              requestForCancelPlan.post(Json.toJson(cancelRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 503,
                errorMessage = "Status code 403: HTTP status is unexpected in received HTTP response."
              )

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
            stubRequest(
              httpMethod = POST,
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
            val errorResponse: TtpInformInformativeError = TtpInformInformativeError(
              apisCalled = Some(
                List(
                  ApiStatus(
                    name = ApiName("CESA"),
                    statusCode = ApiStatusCode(400),
                    processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:30:00Z")),
                    errorResponse = Some(ApiErrorResponse("Invalid cancellationDate"))
                  )
                )
              ),
              internalErrors = List(
                TtpInformInternalError("some error that ttp is responsible for"),
                TtpInformInternalError("another error that ttp is responsible for")
              ),
              processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:31:00Z"))
            )

            stubRequest(
              httpMethod = POST,
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

            stubRequest(
              httpMethod = POST,
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
              stubRequest(
                httpMethod = POST,
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
                  errorMessage =
                    "Received status code 200 with incorrect JSON body for request: POST http://localhost:11111/debts/time-to-pay/inform"
                )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 503
            }
          }

          "when TimeToPay returns an expected error response" - {
            for (responseStatus <- List(400, 500))
              s"<$responseStatus>" - {
                "with a null json response from TTP" in new TimeToPayProxyControllerTestBase {
                  stubRequest(
                    httpMethod = POST,
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
                      errorMessage =
                        s"""Status code $responseStatus: JSON structure is not valid in received error HTTP response.
                           |  Validation errors:
                           |    - For path  , errors: [error.expected.jsobject].""".stripMargin
                    )

                  response.json shouldBe Json.toJson(expectedTtppErrorResponse)
                  response.status shouldBe 503
                }
              }
          }

          "when TimeToPay returns unexpected success status" in new TimeToPayProxyControllerTestBase {
            stubRequest(
              httpMethod = POST,
              url = "/debts/time-to-pay/inform",
              status = 201,
              responseBody = Json.obj().toString()
            )

            val requestForInformTtp: WSRequest = buildRequest("/inform")

            val response: WSResponse = await(
              requestForInformTtp.post(Json.toJson(informRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 503,
                errorMessage = "Status code 201: HTTP status is unexpected in received HTTP response."
              )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }

          "when TimeToPay returns unexpected error status" in new TimeToPayProxyControllerTestBase {
            stubRequest(
              httpMethod = POST,
              url = "/debts/time-to-pay/inform",
              status = 403,
              responseBody = Json.obj().toString()
            )

            val requestForInformTtp: WSRequest = buildRequest("/inform")

            val response: WSResponse = await(
              requestForInformTtp.post(Json.toJson(informRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 503,
                errorMessage = "Status code 403: HTTP status is unexpected in received HTTP response."
              )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }
        }
      }
    }

    ".fullAmendTtp" - {
      "should return a 200 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns a successful response" in new TimeToPayProxyControllerTestBase {
            val jsonResponse = Json.toJson(fullAmendResponse)
            stubRequest(
              httpMethod = POST,
              url = "/debts/time-to-pay/full-amend",
              status = 200,
              responseBody = jsonResponse.toString()
            )

            val requestForFullAmend: WSRequest = buildRequest("/full-amend")

            val response: WSResponse = await(
              requestForFullAmend.post(Json.toJson(fullAmendRequest))
            )

            response.json shouldBe jsonResponse
            response.status shouldBe 200
          }
        }
      }

      "should return a 500 statusCode" - {
        "when given a valid json payload" - {
          "when TimeToPay returns an error response with 500" in new TimeToPayProxyControllerTestBase {
            val errorResponse = TtpFullAmendInformativeError(
              apisCalled = Some(
                List(
                  ApiStatus(
                    name = ApiName("CESA"),
                    statusCode = ApiStatusCode(400),
                    processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:30:00Z")),
                    errorResponse = Some(ApiErrorResponse("Invalid arrangementAgreedDate"))
                  )
                )
              ),
              internalErrors = List(TtpFullAmendInternalError("something happened")),
              processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:31:00Z"))
            )
            val jsonResponse = Json.toJson(errorResponse)

            stubRequest(
              httpMethod = POST,
              url = "/debts/time-to-pay/full-amend",
              status = 500,
              responseBody = jsonResponse.toString()
            )

            val requestForFullAmend: WSRequest = buildRequest("/full-amend")

            val response: WSResponse = await(
              requestForFullAmend.post(Json.toJson(fullAmendRequest))
            )

            response.json shouldBe jsonResponse
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
                  reason = "Invalid request payload: missing identifications"
                )
              )
            )

            val expectedTtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage = "Invalid request payload: missing identifications"
            )

            stubRequest(
              httpMethod = POST,
              url = "/debts/time-to-pay/full-amend",
              status = 400,
              responseBody = Json.toJson(upstreamErrorResponse).toString()
            )

            val requestForFullAmend: WSRequest = buildRequest("/full-amend")

            val response: WSResponse = await(
              requestForFullAmend.post(Json.toJson(fullAmendRequest))
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }
        }

        "when given an invalid json payload" - {
          "with an empty json object" in new TimeToPayProxyControllerTestBase {

            val requestForFullAmend: WSRequest = buildRequest("/full-amend")

            val response: WSResponse = await(
              requestForFullAmend.post(JsObject.empty)
            )

            val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage =
                "Invalid TtpFullAmendRequest payload: Payload has a missing field or an invalid format. Field name: identifications. "
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }

          "when 'arrangementAgreedDate' is invalid" in new TimeToPayProxyControllerTestBase {

            val requestForFullAmend: WSRequest = buildRequest("/full-amend")

            val invalidRequestBody: JsValue = Json.parse(
              """{
                |  "identifications": [
                |    {
                |      "idType": "UTR",
                |      "idValue": "id value 1"
                |    },
                |    {
                |      "idType": "NINO",
                |      "idValue": "id value 2"
                |    }
                |  ],
                |  "instalments": [
                |    {
                |      "dueDate": "2025-05-01",
                |      "amountDue": 840.72
                |    }
                |  ],
                |  "paymentPlan": {
                |    "arrangementAgreedDate": "date invalid",
                |    "ttpEndDate": "2025-03-22",
                |    "initialPaymentDate": "2025-05-22",
                |    "initialPaymentAmount": 40.7,
                |    "frequency": "monthly",
                |    "ddiReference": "ddi ref"
                |  },
                |  "channelIdentifier": "selfService",
                |  "transitioned": false
                |}
                |""".stripMargin
            )

            val response: WSResponse = await(
              requestForFullAmend.post(invalidRequestBody)
            )

            val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
              statusCode = 400,
              errorMessage =
                "Invalid TtpFullAmendRequest payload: Payload has a missing field or an invalid format. Field name: arrangementAgreedDate. Date format should be correctly provided"
            )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }

          "with mandatory fields missing" - {
            "when 'identifications' is missing" in new TimeToPayProxyControllerTestBase {

              val requestForFullAmend: WSRequest = buildRequest("/full-amend")

              val invalidRequestBody: JsValue = Json.parse(
                """{
                  |  "paymentPlan": {
                  |    "arrangementAgreedDate": "2025-05-01",
                  |    "ttpEndDate": "2025-03-22",
                  |    "initialPaymentDate": "2025-05-22",
                  |    "initialPaymentAmount": 40.7,
                  |    "frequency": "monthly",
                  |    "ddiReference": "ddi ref"
                  |  },
                  |  "channelIdentifier": "selfService",
                  |  "transitioned": false
                  |}
                  |""".stripMargin
              )

              val response: WSResponse = await(
                requestForFullAmend.post(invalidRequestBody)
              )

              val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
                statusCode = 400,
                errorMessage =
                  "Invalid TtpFullAmendRequest payload: Payload has a missing field or an invalid format. Field name: identifications. "
              )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 400
            }

            "when 'instalments' is missing" in new TimeToPayProxyControllerTestBase {

              val requestForFullAmend: WSRequest = buildRequest("/full-amend")

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
                  |  "paymentPlan": {
                  |    "arrangementAgreedDate": "2025-05-01",
                  |    "ttpEndDate": "2025-03-22",
                  |    "initialPaymentDate": "2025-05-22",
                  |    "initialPaymentAmount": 40.7,
                  |    "frequency": "monthly",
                  |    "ddiReference": "ddi ref"
                  |  },
                  |  "channelIdentifier": "selfService",
                  |  "transitioned": false
                  |}
                  |""".stripMargin
              )

              val response: WSResponse = await(
                requestForFullAmend.post(invalidRequestBody)
              )

              val expectedTtppErrorResponse: TtppErrorResponse = TtppErrorResponse(
                statusCode = 400,
                errorMessage =
                  "Invalid TtpFullAmendRequest payload: Payload has a missing field or an invalid format. Field name: instalments. "
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
              stubRequest(
                httpMethod = POST,
                url = "/debts/time-to-pay/full-amend",
                status = 200,
                responseBody = JsNull.toString()
              )

              val requestForFullAmend: WSRequest = buildRequest("/full-amend")

              val response: WSResponse = await(
                requestForFullAmend.post(Json.toJson(fullAmendRequest))
              )

              val expectedTtppErrorResponse: TtppErrorResponse =
                TtppErrorResponse(
                  statusCode = 503,
                  errorMessage =
                    "Received status code 200 with incorrect JSON body for request: POST http://localhost:11111/debts/time-to-pay/full-amend"
                )

              response.json shouldBe Json.toJson(expectedTtppErrorResponse)
              response.status shouldBe 503
            }
          }

          "when TimeToPay returns an expected error response" - {
            for (responseStatus <- List(400, 500))
              s"<$responseStatus>" - {
                "with a null json response from TTP" in new TimeToPayProxyControllerTestBase {
                  stubRequest(
                    httpMethod = POST,
                    url = "/debts/time-to-pay/full-amend",
                    status = responseStatus,
                    responseBody = JsNull.toString()
                  )

                  val requestForFullAmend: WSRequest = buildRequest("/full-amend")

                  val response: WSResponse = await(
                    requestForFullAmend.post(Json.toJson(fullAmendRequest))
                  )

                  val expectedTtppErrorResponse: TtppErrorResponse =
                    TtppErrorResponse(
                      statusCode = 503,
                      errorMessage =
                        s"""Status code $responseStatus: JSON structure is not valid in received error HTTP response.
                           |  Validation errors:
                           |    - For path  , errors: [error.expected.jsobject].""".stripMargin
                    )

                  response.json shouldBe Json.toJson(expectedTtppErrorResponse)
                  response.status shouldBe 503
                }
              }
          }

          "when TimeToPay returns unexpected success status" in new TimeToPayProxyControllerTestBase {
            stubRequest(
              httpMethod = POST,
              url = "/debts/time-to-pay/full-amend",
              status = 201,
              responseBody = Json.obj().toString()
            )

            val requestForFullAmend: WSRequest = buildRequest("/full-amend")

            val response: WSResponse = await(
              requestForFullAmend.post(Json.toJson(fullAmendRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 503,
                errorMessage = "Status code 201: HTTP status is unexpected in received HTTP response."
              )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }

          "when TimeToPay returns unexpected error status" in new TimeToPayProxyControllerTestBase {
            stubRequest(
              httpMethod = POST,
              url = "/debts/time-to-pay/full-amend",
              status = 403,
              responseBody = Json.obj().toString()
            )

            val requestForFullAmend: WSRequest = buildRequest("/full-amend")

            val response: WSResponse = await(
              requestForFullAmend.post(Json.toJson(fullAmendRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 503,
                errorMessage = "Status code 403: HTTP status is unexpected in received HTTP response."
              )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }
        }
      }
    }

    ".updatePlan" - {
      "when time-to-pay responds with a 409 http response code" - {
        "when given a valid json payload" - {
          "responds with a 409 status code " in new TimeToPayProxyControllerTestBase {
            // Auth

            // TTP response
            val ttpResponseBody: String =
              """{
                |  "failures": [
                |    {
                |      "code": "CONFLICT",
                |      "reason": "Plan ID exists in both op led and self serve collections"
                |    }
                |  ]
                |}""".stripMargin

            stubRequest(
              httpMethod = PUT,
              url = "/debts/time-to-pay/quote/customerRef1234/95011519-4d29-4e58-95ca-d21d1ec7ba4e",
              status = 409,
              responseBody = ttpResponseBody
            )

            // TTPP request
            val requestForUpdatePlan: WSRequest =
              buildRequest("/quote/customerRef1234/95011519-4d29-4e58-95ca-d21d1ec7ba4e")

            val response: WSResponse = await(
              requestForUpdatePlan.put(Json.toJson(updatePlanRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 409,
                errorMessage = "Plan ID exists in both op led and self serve collections"
              )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 409
          }
        }
      }

      "when time-to-pay responds with a 400 http response code" - {
        "when given a valid json payload" - {
          "responds with a 400 response" in new TimeToPayProxyControllerTestBase {

            val ttpResponseBody: String =
              """{
                |  "failures": [
                |    {
                |      "code": "BAD REQUEST",
                |      "reason": "There was no plan found for 95011519-4d29-4e58-95ca-d21d1ec7ba4e"
                |    }
                |  ]
                |}""".stripMargin

            stubRequest(
              httpMethod = PUT,
              url = "/debts/time-to-pay/quote/customerRef1234/95011519-4d29-4e58-95ca-d21d1ec7ba4e",
              status = 400,
              responseBody = ttpResponseBody
            )

            val requestForUpdatePlan: WSRequest =
              buildRequest("/quote/customerRef1234/95011519-4d29-4e58-95ca-d21d1ec7ba4e")

            val response: WSResponse = await(
              requestForUpdatePlan.put(Json.toJson(updatePlanRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 400,
                errorMessage = "There was no plan found for 95011519-4d29-4e58-95ca-d21d1ec7ba4e"
              )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 400
          }
        }
      }

      "when time-to-pay responds with a 200 http response code" - {
        "when given a valid json payload" - {
          "responds with a 200 status code " in new TimeToPayProxyControllerTestBase {

            val ttpResponseBody: String =
              """{
                |   "customerReference":"customerRef1234",
                |   "planId":"95011519-4d29-4e58-95ca-d21d1ec7ba4e",
                |   "planStatus":"success",
                |   "planUpdatedDate":"1970-01-01"
                |}""".stripMargin

            stubRequest(
              httpMethod = PUT,
              url = "/debts/time-to-pay/quote/customerRef1234/95011519-4d29-4e58-95ca-d21d1ec7ba4e",
              status = 200,
              responseBody = ttpResponseBody
            )

            val requestForUpdatePlan: WSRequest =
              buildRequest("/quote/customerRef1234/95011519-4d29-4e58-95ca-d21d1ec7ba4e")

            val response: WSResponse = await(
              requestForUpdatePlan.put(Json.toJson(updatePlanRequest))
            )

            val expectedResponse: UpdatePlanResponse = UpdatePlanResponse(
              CustomerReference("customerRef1234"),
              PlanId("95011519-4d29-4e58-95ca-d21d1ec7ba4e"),
              PlanStatus.Success,
              LocalDate.EPOCH
            )

            response.json shouldBe Json.toJson(expectedResponse)
            response.status shouldBe 200
          }
        }
      }

      "when time-to-pay responds with a 500 http response code" - {
        "when given a valid json payload" - {
          "responds with a 503 status code because 500 is unexpected" in new TimeToPayProxyControllerTestBase {

            val ttpResponseBody: String =
              """{
                |   "statusCode":999,
                |   "errorMessage":"Internal Service Error"
                |}""".stripMargin

            stubRequest(
              httpMethod = PUT,
              url = "/debts/time-to-pay/quote/customerRef1234/95011519-4d29-4e58-95ca-d21d1ec7ba4e",
              status = 500,
              responseBody = ttpResponseBody
            )

            val requestForUpdatePlan: WSRequest =
              buildRequest("/quote/customerRef1234/95011519-4d29-4e58-95ca-d21d1ec7ba4e")

            val response: WSResponse = await(
              requestForUpdatePlan.put(Json.toJson(updatePlanRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 503,
                errorMessage = "Status code 500: HTTP status is unexpected in received HTTP response."
              )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }
        }
      }

      "when time-to-pay responds with a 503 http response code" - {
        "when given a valid json payload" - {
          "responds with a 503 status code" in new TimeToPayProxyControllerTestBase {

            val ttpResponseBody: String =
              """{
                |   "statusCode":999,
                |   "errorMessage":"HTTP status is unexpected in received HTTP response."
                |}""".stripMargin

            stubRequest(
              httpMethod = PUT,
              url = "/debts/time-to-pay/quote/customerRef1234/95011519-4d29-4e58-95ca-d21d1ec7ba4e",
              status = 503,
              responseBody = ttpResponseBody
            )

            val requestForUpdatePlan: WSRequest =
              buildRequest("/quote/customerRef1234/95011519-4d29-4e58-95ca-d21d1ec7ba4e")

            val response: WSResponse = await(
              requestForUpdatePlan.put(Json.toJson(updatePlanRequest))
            )

            val expectedTtppErrorResponse: TtppErrorResponse =
              TtppErrorResponse(
                statusCode = 503,
                errorMessage = "Status code 503: HTTP status is unexpected in received HTTP response."
              )

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
          statusCode = ApiStatusCode(200),
          processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-05-01T14:30:00Z")),
          errorResponse = None
        ),
        ApiStatus(
          name = ApiName("ETMP"),
          statusCode = ApiStatusCode(201),
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
      paymentPlan = SaOnlyPaymentPlan(
        arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2025-01-01")),
        ttpEndDate = TtpEndDate(LocalDate.parse("2025-02-01")),
        frequency = FrequencyLowercase.Monthly,
        initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2025-01-05"))),
        initialPaymentAmount = Some(GbpPounds.createOrThrow(100.00)),
        ddiReference = Some(DdiReference("TestDDIReference"))
      ),
      instalments = NonEmptyList.of(
        SaOnlyInstalment(
          dueDate = InstalmentDueDate(LocalDate.parse("2025-01-31")),
          amountDue = GbpPounds.createOrThrow(500.00)
        )
      ),
      channelIdentifier = ChannelIdentifier.Advisor,
      transitioned = Some(TransitionedIndicator(true))
    )

    val informResponse: TtpInformSuccessfulResponse = TtpInformSuccessfulResponse(
      apisCalled = List(
        ApiStatus(
          name = ApiName("CESA"),
          statusCode = ApiStatusCode(200),
          processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-05-01T14:30:00Z")),
          errorResponse = None
        ),
        ApiStatus(
          name = ApiName("ETMP"),
          statusCode = ApiStatusCode(201),
          processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-05-01T14:31:00Z")),
          errorResponse = None
        )
      ),
      processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:31:00Z"))
    )

    val fullAmendRequest: TtpFullAmendRequest = TtpFullAmendRequest(
      identifications = NonEmptyList.of(
        Identification(idType = IdType("NINO"), idValue = IdValue("AA000000A")),
        Identification(idType = IdType("MTDITID"), idValue = IdValue("XAIT00000000054")),
        Identification(idType = IdType("UTR"), idValue = IdValue("1234567890"))
      ),
      paymentPlan = SaOnlyPaymentPlan(
        arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2090-06-08")),
        ttpEndDate = TtpEndDate(LocalDate.parse("2025-02-01")),
        frequency = FrequencyLowercase.Monthly,
        initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2025-05-05"))),
        initialPaymentAmount = Some(GbpPounds.createOrThrow(BigDecimal("150.00"))),
        ddiReference = Some(DdiReference("DD123456789"))
      ),
      instalments = NonEmptyList.of(
        SaOnlyInstalment(
          dueDate = InstalmentDueDate(LocalDate.parse("2025-06-01")),
          amountDue = GbpPounds.createOrThrow(BigDecimal("300.00"))
        ),
        SaOnlyInstalment(
          dueDate = InstalmentDueDate(LocalDate.parse("2025-06-01")),
          amountDue = GbpPounds.createOrThrow(BigDecimal("300.00"))
        )
      ),
      channelIdentifier = ChannelIdentifier.Advisor,
      transitioned = TransitionedIndicator(true)
    )

    val fullAmendResponse: TtpFullAmendSuccessfulResponse = TtpFullAmendSuccessfulResponse(
      apisCalled = List(
        ApiStatus(
          name = ApiName("CESA"),
          statusCode = ApiStatusCode(200),
          processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-05-01T14:30:00Z")),
          errorResponse = None
        ),
        ApiStatus(
          name = ApiName("ETMP"),
          statusCode = ApiStatusCode(201),
          processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-05-01T14:31:00Z")),
          errorResponse = None
        )
      ),
      processingDateTime = ProcessingDateTimeInstant(java.time.Instant.parse("2025-10-15T10:31:00Z"))
    )

    val updatePlanRequest: UpdatePlanRequest = UpdatePlanRequest(
      customerReference = CustomerReference("customerRef1234"),
      planId = PlanId("95011519-4d29-4e58-95ca-d21d1ec7ba4e"),
      updateType = UpdateType("paymentDetails"),
      channelIdentifier = Some(ChannelIdentifier.Advisor),
      planStatus = Some(PlanStatus.ResolvedCompleted),
      completeReason = Some(CompleteReason.PaymentInFullCaps),
      cancellationReason = Some(CancellationReason("debt-resolved")),
      thirdPartyBank = Some(true),
      payments = Some(
        List(
          PaymentInformation(
            paymentMethod = PaymentMethod.Cheque,
            paymentReference = Some(PaymentReference("paymentRef"))
          )
        )
      )
    )
  }
}
