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

import cats.data.NonEmptyList
import com.github.tomakehurst.wiremock.http.RequestMethod.POST
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.error.TtppErrorResponse
import uk.gov.hmrc.timetopayproxy.models.saonly.common.{ArrangementAgreedDate, InitialPaymentDate, ProcessingDateTimeInstant, SaOnlyInstalment, TransitionedIndicator, TtpEndDate}
import uk.gov.hmrc.timetopayproxy.models.saonly.common.apistatus.{ApiErrorResponse, ApiName, ApiStatus, ApiStatusCode}
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel.{CancellationDate, TtpCancelInformativeError, TtpCancelInternalError, TtpCancelPaymentPlan, TtpCancelRequest, TtpCancelSuccessfulResponse}
import uk.gov.hmrc.timetopayproxy.models.{ChannelIdentifier, FrequencyLowercase, IdType, IdValue, Identification, InstalmentDueDate, TimeToPayError, TimeToPayInnerError}
import uk.gov.hmrc.timetopayproxy.support.IntegrationBaseSpec
import uk.gov.hmrc.timetopayproxy.testutils.TestOnlyJsonFormats._


import java.time.LocalDate
import scala.concurrent.ExecutionContext

class TimeToPayProxyControllerSaRelease2DisabledItSpec extends IntegrationBaseSpec {
  def internalAuthEnabled: Boolean = false
  def enrolmentAuthEnabled: Boolean = false
  def saRelease2Enabled: Boolean = false

  ".cancelTtp" - {
    "should return a 200 statusCode" - {
      "when given a valid json payload" - {
        "when TimeToPay returns a successful response" in new TimeToPayProxyControllerSaRelease2DisabledTestBase {
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
        "when TimeToPay returns an error response with 500" in new TimeToPayProxyControllerSaRelease2DisabledTestBase {
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
        "when TimeToPay returns an error response of 400" in new TimeToPayProxyControllerSaRelease2DisabledTestBase {
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
        "with an empty json object" in {

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

        "when 'cancellationDate' is invalid" in {

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
          "when 'identifications' is missing" in {

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
        "when TimeToPay returns an expected 200 response" - new TimeToPayProxyControllerSaRelease2DisabledTestBase {
          "with a null json response from TTP" in {
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
                  """For status code 200 for request to POST http://localhost:11111/debts/time-to-pay/cancel: JSON structure is not valid in received HTTP response. Originally expected to turn response into a Right."""
              )

            response.json shouldBe Json.toJson(expectedTtppErrorResponse)
            response.status shouldBe 503
          }
        }

        "when TimeToPay returns an expected error response" - {
          for (responseStatus <- List(400, 500))
            s"<$responseStatus>" - {
              "with a null json response from TTP" in new TimeToPayProxyControllerSaRelease2DisabledTestBase {
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
                      s"""For status code ${responseStatus: Int} for request to POST http://localhost:11111/debts/time-to-pay/cancel: JSON structure is not valid in received HTTP response. Originally expected to turn response into a Left.
                         |Detail: Validation errors:
                         |    - For path  , errors: [error.expected.jsobject].""".stripMargin
                  )

                response.json shouldBe Json.toJson(expectedTtppErrorResponse)
                response.status shouldBe 503
              }
            }
        }

        "when TimeToPay returns unexpected success status" in new TimeToPayProxyControllerSaRelease2DisabledTestBase {
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
              errorMessage =
                """For status code 201 for request to POST http://localhost:11111/debts/time-to-pay/cancel: HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left."""
            )

          response.json shouldBe Json.toJson(expectedTtppErrorResponse)
          response.status shouldBe 503
        }

        "when TimeToPay returns unexpected error status" in new TimeToPayProxyControllerSaRelease2DisabledTestBase {
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
              errorMessage =
                """For status code 403 for request to POST http://localhost:11111/debts/time-to-pay/cancel: HTTP status is unexpected in received HTTP response. Originally expected to turn response into a Left."""
            )

          response.json shouldBe Json.toJson(expectedTtppErrorResponse)
          response.status shouldBe 503
        }
      }
    }
  }
  
  trait TimeToPayProxyControllerSaRelease2DisabledTestBase {
    implicit def ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    implicit val hc: HeaderCarrier = HeaderCarrier()

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
    
  }

}
