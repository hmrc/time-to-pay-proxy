/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.config

import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.timetopayproxy.support.IntegrationBaseSpec

class DocumentationControllerSpec extends IntegrationBaseSpec {

  val apiDefinitionJson: JsValue = Json.parse(
    """
      |{
      |  "scopes": [
      |    {
      |      "key": "read:time-to-pay-proxy",
      |      "name": "Time to pay proxy",
      |      "description": "Allow Read access to time to pay proxy"
      |    }
      |  ],
      |  "api": {
      |    "name": "Time To Pay Proxy",
      |    "description": "A API for Time To Pay Proxy.",
      |    "context": "individuals/time-to-pay-proxy",
      |    "versions": [
      |      {
      |        "version": "1.0",
      |        "access": {
      |          "type": "PRIVATE",
      |          "whitelistedApplicationIds": []
      |        },
      |        "status": "BETA",
      |        "endpointsEnabled": true
      |      }
      |    ]
      |  }
      |}
      |
    """.stripMargin
  )

  "GET /api/definition" should {
    "return a 200 with the correct response body" in {
      val response: WSResponse = await(buildRequest("/api/definition").get())
      response.status shouldBe Status.OK
      Json.parse(response.body) shouldBe apiDefinitionJson
    }
  }

  "a documentation request" must {
    "return the documentation" in {
      val response: WSResponse = await(buildRequest("/api/conf/1.0/application.raml").get())
      response.status shouldBe Status.OK
      response.body[String] should startWith("#%RAML 1.0")
    }
  }

}