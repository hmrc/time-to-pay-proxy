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

import cats.data.EitherT
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, StringContextOps }
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.timetopayproxy.config.{ AppConfig, FeatureSwitch }
import uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder.HttpReadsBuilder
import uk.gov.hmrc.timetopayproxy.logging.{ RequestAwareLogger, StatusLogger }
import uk.gov.hmrc.timetopayproxy.models.TimeToPayError
import uk.gov.hmrc.timetopayproxy.models.cdcs.chargemigration.{ ChargeMigrationRequest, ChargeMigrationResponse }
import uk.gov.hmrc.timetopayproxy.models.error.ProxyEnvelopeError
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ChargeMigrationConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  featureSwitch: FeatureSwitch
) {

  private val logger: RequestAwareLogger = new RequestAwareLogger(classOf[ChargeMigrationConnector])

  private val httpReadsBuilderForChargeMigration: HttpReadsBuilder[ProxyEnvelopeError, ChargeMigrationResponse] =
    HttpReadsBuilder
      .withDefault503ConnectorError[ProxyEnvelopeError, ChargeMigrationResponse](this.getClass)
      .handleSuccess[ChargeMigrationResponse](200)
      .handleErrorTransformed[TimeToPayError](400, ttpError => ttpError.toConnectorError(status = 400))
      .handleErrorTransformed[TimeToPayError](401, ttpError => ttpError.toConnectorError(status = 401))

  def chargeMigration(
    request: ChargeMigrationRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[ChargeMigrationResponse] = {

    implicit def httpReads: HttpReads[Either[ProxyEnvelopeError, ChargeMigrationResponse]] =
      httpReadsBuilderForChargeMigration.httpReads(logger, _.toStringSafeToLogInProd)

    val path = "/debts/time-to-pay/charge-migration"

    val url = url"${appConfig.ttpBaseUrl + path}"

    StatusLogger(
      EitherT(
        httpClient
          .post(url)
          .withBody(Json.toJson(request))
          .setHeader(requestHeaders*)
          .execute[Either[ProxyEnvelopeError, ChargeMigrationResponse]]
      )
    ).logBasedOnStatusCode(logger)
  }

  private def requestHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val combinedPreviousHeaders: Seq[(String, String)] = hc.headers(List("correlationId")) ++ hc.extraHeaders

    if (featureSwitch.internalAuthEnabled.enabled) {
      ("Authorization" -> appConfig.internalAuthToken) +: combinedPreviousHeaders
    } else {
      combinedPreviousHeaders
    }
  }
}
