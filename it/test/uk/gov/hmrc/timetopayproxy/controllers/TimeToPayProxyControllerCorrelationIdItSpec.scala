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
import com.github.tomakehurst.wiremock.http.RequestMethod.POST
import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.timetopayproxy.models._
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
}
