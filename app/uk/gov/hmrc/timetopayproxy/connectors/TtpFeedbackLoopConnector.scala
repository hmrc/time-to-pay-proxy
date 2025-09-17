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
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.error.{ ConnectorError, TtppSpecificError }
import uk.gov.hmrc.timetopayproxy.models.saopled.ttpcancel.{ TtpCancelGeneralFailureResponse, TtpCancelInformativeError, TtpCancelInformativeResponse, TtpCancelRequest }

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

/** Feedback Loop Connector for CDCS -> TTP communication.
  *
  * The "feedback loop" refers to CDCS notifying TTP service about customer plan lifecycle events
  * (cancel, amend, inform) so TTP can update downstream HoDs accordingly.
  */
@ImplementedBy(classOf[DefaultTtpFeedbackLoopConnector])
trait TtpFeedbackLoopConnector {
  def cancelTtp(
    ttppRequest: TtpCancelRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpCancelInformativeResponse]
}

@Singleton
class DefaultTtpFeedbackLoopConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)
    extends TtpFeedbackLoopConnector {

  private val logger: RequestAwareLogger = new RequestAwareLogger(classOf[DefaultTtpFeedbackLoopConnector])

  private val httpReadsBuilder: HttpReadsWithLoggingBuilder[TtppSpecificError, TtpCancelInformativeResponse] =
    HttpReadsWithLoggingBuilder[TtppSpecificError, TtpCancelInformativeResponse]
      .orSuccess[TtpCancelInformativeResponse](200)
      // TODO DTD-3785: Read `TtpCancelInformativeError` directly and remove the transformation.
      .orErrorTransformed[TtpCancelInformativeResponse](500, TtpCancelInformativeError(_))
      .orErrorTransformed[TtpCancelGeneralFailureResponse](400, error => ConnectorError(400, error.details))

  def cancelTtp(
    ttppRequest: TtpCancelRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpCancelInformativeResponse] = {

    implicit def httpReads: HttpReads[Either[TtppSpecificError, TtpCancelInformativeResponse]] =
      httpReadsBuilder.httpReads(logger)

    val path = if (appConfig.useIf) "/individuals/debts/time-to-pay/cancel" else "/debts/time-to-pay/cancel"

    val url = url"${appConfig.ttpBaseUrl + path}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(ttppRequest))
        .setHeader(requestHeaders: _*)
        .execute[Either[TtppSpecificError, TtpCancelInformativeResponse]]
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
