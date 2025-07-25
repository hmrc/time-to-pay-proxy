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

import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, EitherValues }
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.{ WSClient, WSRequest, WSResponse }
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }
import play.api.{ Application, Environment, Mode }

trait IntegrationBaseSpec
    extends AnyFreeSpec with MockFactory with EitherValues with Matchers with FutureAwaits with DefaultAwaitTimeout
    with WireMockHelper with GuiceOneServerPerSuite with BeforeAndAfterEach with BeforeAndAfterAll {

  val mockHost: String = WireMockHelper.host
  val mockPort: String = WireMockHelper.wireMockPort.toString

  lazy val client: WSClient = app.injector.instanceOf[WSClient]

  def servicesConfig: Map[String, Any] =
    Map(
      "microservice.services.auth.host" -> mockHost,
      "microservice.services.auth.port" -> mockPort,
      "microservice.services.ttp.host"  -> mockHost,
      "microservice.services.ttp.port"  -> mockPort,
      "microservice.services.ttpe.host" -> mockHost,
      "microservice.services.ttpe.port" -> mockPort,
      "microservice.services.ttp.token" -> "dummyToken",
      "microservice.services.ttp.useIf" -> false,
      "microservice.services.stub.host" -> mockHost,
      "microservice.services.stub.port" -> mockPort,
      "metrics.enabled"                 -> false,
      "auditing.enabled"                -> false
    )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(servicesConfig)
    .build()

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWireMock()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }

  def buildRequest(path: String): WSRequest =
    client.url(s"http://localhost:$port$path").withFollowRedirects(false).withHttpHeaders("Authorization" -> "dummy")

  def document(response: WSResponse): JsValue = Json.parse(response.body)
}
