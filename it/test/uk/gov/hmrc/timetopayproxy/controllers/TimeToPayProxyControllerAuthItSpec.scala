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
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import play.api.libs.json.Json
import play.api.libs.ws.{ WSRequest, WSResponse }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.error.TtppErrorResponse
import uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi._
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.support.IntegrationBaseSpec
import uk.gov.hmrc.timetopayproxy.testutils.TestOnlyJsonFormats._

import scala.concurrent.ExecutionContext

class TimeToPayProxyControllerAuthItSpec extends IntegrationBaseSpec {
  implicit def ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val invalidAuthToken: String = "invalid-auth-token"

  override def servicesConfig: Map[String, Any] =
    Map(
      "microservice.services.auth.host"    -> mockHost,
      "microservice.services.auth.port"    -> mockPort,
      "microservice.services.ttpe.host"    -> mockHost,
      "microservice.services.ttpe.port"    -> mockPort,
      "metrics.enabled"                    -> false,
      "auditing.enabled"                   -> false,
      "feature-switch.internalAuthEnabled" -> true,
      "internal-auth.token"                -> invalidAuthToken
    )

  "TimeToPayProxyController" - {
    ".checkChargeInfo" - {
      val chargeInfoRequest: ChargeInfoRequest = ChargeInfoRequest(
        channelIdentifier = ChargeInfoChannelIdentifier("Channel Identifier"),
        identifications = NonEmptyList.of(
          Identification(idType = IdType("id type 1"), idValue = IdValue("id value 1")),
          Identification(idType = IdType("id type 2"), idValue = IdValue("id value 2"))
        ),
        regimeType = SaOnlyRegimeType.SA
      )

      "should return a 401" - {
        "when an invalid authorisation token is used" in {
          val unauthorisedError: TimeToPayEligibilityError =
            TimeToPayEligibilityError(code = "401", reason = "Unauthorized")

          stubPostWithResponseBody(url = "/auth/authorise", status = 200, responseBody = "null")
          stubPostWithResponseBody(
            url = "/debts/time-to-pay/charge-info",
            status = 401,
            responseBody = Json.toJson(unauthorisedError).toString(),
            requestHeaderContaining = Some(Seq("Authorization" -> equalTo("invalid-auth-token")))
          )

          val requestForChargeInfo: WSRequest = buildRequest("/charge-info")
          val response: WSResponse = await(
            requestForChargeInfo.post(Json.toJson(chargeInfoRequest))
          )

          val expectedTtppErrorResponse: TtppErrorResponse =
            TtppErrorResponse(statusCode = 401, errorMessage = "Unauthorized")

          response.json shouldBe Json.toJson(expectedTtppErrorResponse)
          response.status shouldBe 401
        }
      }
    }
  }
}
