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

package uk.gov.hmrc.timetopayproxy.support

/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

import com.codahale.metrics.SharedMetricRegistries
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder

trait WireMockUtils extends BeforeAndAfterEach with BeforeAndAfterAll with GuiceOneAppPerSuite {
  self: PlaySpec =>

  val wireMockPort = 11111

  val wireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(wireMockPort))

  override implicit lazy val app = GuiceApplicationBuilder()
    .configure("auditing.enabled" -> "false")
    .build()

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
    SharedMetricRegistries.clear()
    WireMock.configureFor("localhost", wireMockPort)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset()
  }

  def stubGetWithResponseBody(url: String, status: Int, response: String): StubMapping =
    stubFor(
      get(urlMatching(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader("Content-Type", "application/json; charset=utf-8")
        )
    )

  def stubPostWithResponseBody(url: String, status: Int, responseBody: String): StubMapping =
    stubFor(
      post(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)
            .withHeader("Content-Type", "application/json; charset=utf-8")
        )
    )

  def stubPostWithoutResponseBody(url: String, status: Int): StubMapping =
    stubFor(
      post(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json; charset=utf-8")
        )
    )

  def stubPut(url: String, status: Int): StubMapping =
    stubFor(
      put(urlMatching(url))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def stubPutWithResponseBody(url: String, status: Int, responseBody: String): StubMapping =
    stubFor(
      put(urlMatching(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)
            .withHeader("Content-Type", "application/json; charset=utf-8")
        )
    )
}
