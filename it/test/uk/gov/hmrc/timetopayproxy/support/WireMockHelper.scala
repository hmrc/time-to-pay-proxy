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

package uk.gov.hmrc.timetopayproxy.support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{ MappingBuilder, WireMock }
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.matching.{ StringValuePattern, UrlPattern }
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }

object WireMockHelper extends Eventually with IntegrationPatience {

  val wireMockPort: Int = 11111
  val host: String = "localhost"
}

trait WireMockHelper {
  import WireMockHelper._

  lazy val wireMockConf: WireMockConfiguration = wireMockConfig.port(wireMockPort).notifier(new ConsoleNotifier(true))
  lazy val wireMockServer: WireMockServer = new WireMockServer(wireMockConf)

  def startWireMock(): Unit = {
    wireMockServer.start()
    WireMock
      .configureFor(host, wireMockPort)
  }

  def stopWireMock(): Unit = wireMockServer.stop()

  def resetWireMock(): Unit = WireMock.reset()

  def stubRequest(
    url: String,
    status: Int,
    responseHeaderContaining: Option[Seq[(String, String)]] = None,
    responseBody: String,
    requestHeaderContaining: Option[Seq[(String, StringValuePattern)]] = None,
    requestBodyContaining: Option[String] = None,
    urlToMappingBuilder: UrlPattern => MappingBuilder
  ): StubMapping =
    stubFor {
      val mapping = urlToMappingBuilder(urlEqualTo(url))

      val response = aResponse()
        .withStatus(status)
        .withBody(responseBody)
        .withHeader("Content-Type", "application/json; charset=utf-8")

      val beforeCheckingRequest = requestHeaderContaining
        .fold(mapping) { headers =>
          headers.foldLeft(mapping) { case (mapping, (key, value)) =>
            mapping.withHeader(key, value)
          }
        }
        .willReturn(
          responseHeaderContaining.fold(response) { headers =>
            headers.foldLeft(response) { case (response, (key, value)) =>
              response.withHeader(key, value)
            }
          }
        )

      requestBodyContaining match {
        case Some(value) => beforeCheckingRequest.withRequestBody(containing(value))
        case None        => beforeCheckingRequest
      }
    }

}
