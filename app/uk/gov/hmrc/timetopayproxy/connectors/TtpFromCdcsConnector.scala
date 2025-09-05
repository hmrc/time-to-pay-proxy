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

package uk.gov.hmrc.timetopayproxy.connectors

import cats.data.EitherT
import com.google.inject.ImplementedBy
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, StringContextOps }
import uk.gov.hmrc.timetopayproxy.config.AppConfig
import uk.gov.hmrc.timetopayproxy.connectors.util.HttpReadsWithLoggingBuilder
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger
import uk.gov.hmrc.timetopayproxy.models.error.{ ConnectorError, InternalTtppError }
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.saopled.ttpcancel.{ TtpCancelGeneralFailureResponse, TtpCancelInformativeError, TtpCancelInformativeResponse, TtpCancelRequest }

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[DefaultTtpFromCdcsConnector])
trait TtpFromCdcsConnector {
  def cancelTtp(
    ttppRequest: TtpCancelRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpCancelInformativeResponse]
}

@Singleton
class DefaultTtpFromCdcsConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)
    extends TtpFromCdcsConnector {

  private val logger: RequestAwareLogger = new RequestAwareLogger(classOf[DefaultTtpFromCdcsConnector])

  def cancelTtp(
    ttppRequest: TtpCancelRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpCancelInformativeResponse] = {
    implicit def httpReads: HttpReads[Either[InternalTtppError, TtpCancelInformativeResponse]] =
      HttpReadsWithLoggingBuilder[InternalTtppError, TtpCancelInformativeResponse](logger)
        .orSuccess[TtpCancelInformativeResponse](200)
        .orErrorTransformed[TtpCancelInformativeResponse](500, TtpCancelInformativeError(_))
        .orErrorTransformed[TtpCancelGeneralFailureResponse](400, _.toConnectorError(400))
        .orErrorTransformed[play.api.libs.json.JsObject](
          404,
          _ => ConnectorError(404, "Unexpected response from upstream")
        )
        .httpReads

    val path = if (appConfig.useIf) "/individuals/debts/time-to-pay/cancel" else "/debts/time-to-pay/cancel"

    val url = url"${appConfig.ttpBaseUrl + path}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(ttppRequest))
        .setHeader(requestHeaders: _*)
        .execute[Either[InternalTtppError, TtpCancelInformativeResponse]]
    )
  }

  private def requestHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val correlationId: String = getOrGenerateCorrelationId

    if (appConfig.useIf) {
      Seq(
        "Authorization" -> s"Bearer ${appConfig.ttpToken: String}",
        "CorrelationId" -> correlationId
      )
    } else {
      Seq(
        "CorrelationId" -> correlationId
      )
    }
  }

  private def getOrGenerateCorrelationId(implicit hc: HeaderCarrier): String =
    (hc.headers(Seq("CorrelationId")) ++ hc.extraHeaders)
      .toMap[String, String]
      .getOrElse("CorrelationId", UUID.randomUUID().toString)

}
