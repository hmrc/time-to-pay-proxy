/*
 * Copyright 2025 HM Revenue & Customs
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
import com.google.inject.ImplementedBy
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, StringContextOps }
import uk.gov.hmrc.timetopayproxy.config.{ AppConfig, FeatureSwitch }
import uk.gov.hmrc.timetopayproxy.connectors.util.HttpReadsWithLoggingBuilder
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger
import uk.gov.hmrc.timetopayproxy.models.TimeToPayEligibilityError
import uk.gov.hmrc.timetopayproxy.models.error.ProxyEnvelopeError
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi.{ ChargeInfoRequest, ChargeInfoResponse }

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[DefaultTtpeConnector])
trait TtpeConnector {
  def checkChargeInfo(
    chargeInfoRequest: ChargeInfoRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[ChargeInfoResponse]
}

@Singleton
class DefaultTtpeConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2, featureSwitch: FeatureSwitch)
    extends TtpeConnector {

  private val logger: RequestAwareLogger = new RequestAwareLogger(classOf[DefaultTtpConnector])

  private val httpReadsBuilderForChargeInfo: HttpReadsWithLoggingBuilder[ProxyEnvelopeError, ChargeInfoResponse] =
    HttpReadsWithLoggingBuilder
      .empty[ProxyEnvelopeError, ChargeInfoResponse]
      .orSuccess[ChargeInfoResponse](200)
      .orErrorTransformed[TimeToPayEligibilityError](400, ttpError => ttpError.toConnectorError(status = 400))

  private val authorizationHeader: Seq[(String, String)] =
    if (featureSwitch.internalAuthEnabled.enabled)
      Seq("Authorization" -> appConfig.internalAuthToken)
    else Seq.empty

  val headers: String => Seq[(String, String)] = (guid: String) =>
    Seq("CorrelationId" -> s"$guid") ++ authorizationHeader

  private def getOrGenerateCorrelationId(implicit hc: HeaderCarrier): String =
    (hc.headers(Seq("CorrelationId")) ++ hc.extraHeaders)
      .toMap[String, String]
      .getOrElse("CorrelationId", UUID.randomUUID().toString)

  def checkChargeInfo(chargeInfoRequest: ChargeInfoRequest)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[ChargeInfoResponse] = {

    implicit def httpReads: HttpReads[Either[ProxyEnvelopeError, ChargeInfoResponse]] =
      httpReadsBuilderForChargeInfo.httpReads(logger)

    val path = "/debts/time-to-pay/charge-info"

    val url = url"${appConfig.ttpeBaseUrl + path}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(chargeInfoRequest))
        .setHeader(headers(getOrGenerateCorrelationId): _*)
        .execute[Either[ProxyEnvelopeError, ChargeInfoResponse]]
    )
  }
}
