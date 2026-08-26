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

package uk.gov.hmrc.timetopayproxy.connectors

import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ Json, Writes }
import play.api.{ ConfigLoader, Configuration }
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.timetopayproxy.config.{ AppConfig, FeatureSwitch }
import uk.gov.hmrc.timetopayproxy.connectors.{ ChargeMigrationConnector, MockAppConfig }
import uk.gov.hmrc.timetopayproxy.models.ChannelIdentifier
import uk.gov.hmrc.timetopayproxy.models.cdcs.chargemigration.{ ChargeMigration, ChargeMigrationRequest, ChargeMigrationResponse, ReplacementCharge }
import uk.gov.hmrc.timetopayproxy.models.error.ConnectorError
import uk.gov.hmrc.timetopayproxy.models.featureSwitches.InternalAuthEnabled
import uk.gov.hmrc.timetopayproxy.support.WireMockUtils

import java.time.{ Instant, LocalDate }
import scala.concurrent.ExecutionContext

final class ChargeMigrationConnectorSpec
    extends PlaySpec with DefaultAwaitTimeout with FutureAwaits with MockFactory with WireMockUtils {

  private val config = mock[Configuration]
  private val servicesConfig = mock[ServicesConfig]
  private val featureSwitch = mock[FeatureSwitch]

  val httpClient: HttpClientV2 =
    app.injector.instanceOf[HttpClientV2]

  private class Setup(internalAuthEnabled: Boolean = false) {
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val hc: HeaderCarrier = HeaderCarrier()

    (servicesConfig
      .baseUrl(_: String))
      .expects("auth")
      .once()
      .returns("http://localhost:11111")

    (servicesConfig
      .baseUrl(_: String))
      .expects("ttp")
      .once()
      .returns("http://localhost:11111")

    (servicesConfig
      .baseUrl(_: String))
      .expects("ttpe")
      .once()
      .returns("unused")

    (servicesConfig
      .baseUrl(_: String))
      .expects("stub")
      .once()
      .returns("http://localhost:11111")

    (config
      .get(_: String)(_: ConfigLoader[String]))
      .expects("microservice.services.ttp.token", *)
      .once()
      .returns("TOKEN")

    (config
      .get(_: String)(_: ConfigLoader[Boolean]))
      .expects("auditing.enabled", *)
      .once()
      .returns(false)

    (config
      .get(_: String)(_: ConfigLoader[String]))
      .expects("microservice.metrics.graphite.host", *)
      .once()
      .returns("http://localhost:11111")

    (config
      .getOptional(_: String)(_: ConfigLoader[Option[Configuration]]))
      .expects("feature-switch", *)
      .once()
      .returns(None)

    (config
      .get(_: String)(_: ConfigLoader[String]))
      .expects("internal-auth.token", *)
      .returns("valid-auth-token")

    (() => featureSwitch.internalAuthEnabled)
      .expects()
      .returning(InternalAuthEnabled(internalAuthEnabled))

    val mockConfiguration: AppConfig = new MockAppConfig(config, servicesConfig, internalAuthEnabled)

    val connector: ChargeMigrationConnector =
      new ChargeMigrationConnector(
        mockConfiguration,
        httpClient,
        featureSwitch
      )
  }

  "ChargeMigrationConnector" should {
    ".chargeMigration" should {

      val chargeMigrationRequest: ChargeMigrationRequest =
        ChargeMigrationRequest(
          planId = "planId",
          migratedAt = Instant.parse("2026-07-08T13:49:51.123Z"),
          planCreationChannel = ChannelIdentifier.Advisor,
          chargeMigrations = List(
            ChargeMigration(
              originalChargeId = "chargeId01",
              replacementDebtItemChargeId = "chargeId02",
              replacementCharges = List(
                ReplacementCharge(
                  parentMainTrans = Some("5330"),
                  mainTrans = "5330",
                  subTrans = "7006",
                  originalDebtAmount = BigInt(5000),
                  interestStartDate = Some(LocalDate.parse("2026-06-30")),
                  paymentHistory = None
                )
              )
            )
          )
        )

      val chargeMigrationResponse: ChargeMigrationResponse =
        ChargeMigrationResponse(
          planId = "planId",
          processingDateTime = Instant.parse("2026-07-08T13:50:00Z")
        )

      "return a successful response" in new Setup() {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/charge-migration",
          Json.toJson(chargeMigrationRequest).toString(),
          200,
          Json.toJson(chargeMigrationResponse).toString()
        )

        private val result = connector.chargeMigration(chargeMigrationRequest)

        await(result.value) mustBe Right(chargeMigrationResponse)
      }

      "parse a 400 error response from TTP" in new Setup() {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/charge-migration",
          Json.toJson(chargeMigrationRequest).toString(),
          400,
          """{"failures": [{"code": "400", "reason": "Bad request"}]}"""
        )

        private val result = connector.chargeMigration(chargeMigrationRequest)

        await(result.value) mustBe Left(ConnectorError(400, "Bad request"))
      }

      "parse a 401 error response from TTP" in new Setup() {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/charge-migration",
          Json.toJson(chargeMigrationRequest).toString(),
          401,
          """{"failures": [{"code": "401", "reason": "Unauthorized"}]}"""
        )

        private val result = connector.chargeMigration(chargeMigrationRequest)

        await(result.value) mustBe Left(ConnectorError(401, "Unauthorized"))
      }

      "return the default connector error for a 503 response" in new Setup() {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/charge-migration",
          Json.toJson(chargeMigrationRequest).toString(),
          503,
          Json
            .obj(
              "statusCode"   -> 503,
              "errorMessage" -> "Service unavailable"
            )
            .toString()
        )

        private val result = connector.chargeMigration(chargeMigrationRequest)

        await(result.value).isLeft mustBe true
      }
    }
  }
}
