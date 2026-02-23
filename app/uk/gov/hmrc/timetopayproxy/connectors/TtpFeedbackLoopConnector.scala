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
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, StringContextOps }
import uk.gov.hmrc.timetopayproxy.config.{ AppConfig, FeatureSwitch }
import uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder.HttpReadsBuilder
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger
import uk.gov.hmrc.timetopayproxy.models.TimeToPayError
import uk.gov.hmrc.timetopayproxy.models.error.ProxyEnvelopeError
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel.{ TtpCancelInformativeError, TtpCancelRequest, TtpCancelRequestR2, TtpCancelSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpfullamend.{ TtpFullAmendInformativeError, TtpFullAmendRequest, TtpFullAmendSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform.{ InformRequest, TtpInformInformativeError, TtpInformSuccessfulResponse }

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

/** Feedback Loop Connector for CDCS -> TTP communication.
  *
  * The "feedback loop" refers to CDCS notifying TTP service about customer plan lifecycle events
  * (cancel, amend, inform) so TTP can update downstream HoDs accordingly.
  */
@Singleton
class TtpFeedbackLoopConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  featureSwitch: FeatureSwitch
) {

  private val logger: RequestAwareLogger = new RequestAwareLogger(classOf[TtpFeedbackLoopConnector])

  private val httpReadsBuilderForCancel: HttpReadsBuilder[ProxyEnvelopeError, TtpCancelSuccessfulResponse] =
    HttpReadsBuilder
      .withDefault503ConnectorError[ProxyEnvelopeError, TtpCancelSuccessfulResponse](this.getClass)
      .handleSuccess[TtpCancelSuccessfulResponse](200)
      .handleError[TtpCancelInformativeError](500)
      .handleErrorTransformed[TimeToPayError](400, ttpError => ttpError.toConnectorError(status = 400))
      .handleErrorTransformed[TimeToPayError](401, ttpError => ttpError.toConnectorError(status = 401))

  private val httpReadsBuilderForInform: HttpReadsBuilder[ProxyEnvelopeError, TtpInformSuccessfulResponse] =
    HttpReadsBuilder
      .withDefault503ConnectorError[ProxyEnvelopeError, TtpInformSuccessfulResponse](this.getClass)
      .handleSuccess[TtpInformSuccessfulResponse](200)
      .handleError[TtpInformInformativeError](500)
      .handleErrorTransformed[TimeToPayError](400, ttpError => ttpError.toConnectorError(status = 400))
      .handleErrorTransformed[TimeToPayError](401, ttpError => ttpError.toConnectorError(status = 401))

  private val httpReadsBuilderForFullAmend: HttpReadsBuilder[ProxyEnvelopeError, TtpFullAmendSuccessfulResponse] =
    HttpReadsBuilder
      .withDefault503ConnectorError[ProxyEnvelopeError, TtpFullAmendSuccessfulResponse](this.getClass)
      .handleSuccess[TtpFullAmendSuccessfulResponse](200)
      .handleError[TtpFullAmendInformativeError](500)
      .handleErrorTransformed[TimeToPayError](400, ttpError => ttpError.toConnectorError(status = 400))
      .handleErrorTransformed[TimeToPayError](401, ttpError => ttpError.toConnectorError(status = 401))

  def cancelTtp(
    ttppRequest: TtpCancelRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpCancelSuccessfulResponse] = {

    implicit def httpReads: HttpReads[Either[ProxyEnvelopeError, TtpCancelSuccessfulResponse]] =
      httpReadsBuilderForCancel.httpReads(logger, makeErrorSafeToLogInProd = _.toStringSafeToLogInProd)

    val path = "/debts/time-to-pay/cancel"

    val url = url"${appConfig.ttpBaseUrl + path}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(ttppRequest))
        .setHeader(requestHeaders: _*)
        .execute[Either[ProxyEnvelopeError, TtpCancelSuccessfulResponse]]
    )
  }

  def cancelTtpR2(
    ttppRequestR2: TtpCancelRequestR2
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpCancelSuccessfulResponse] = {

    implicit def httpReads: HttpReads[Either[ProxyEnvelopeError, TtpCancelSuccessfulResponse]] =
      httpReadsBuilderForCancel.httpReads(logger, makeErrorSafeToLogInProd = _.toStringSafeToLogInProd)

    val path = "/debts/time-to-pay/cancel"

    val url = url"${appConfig.ttpBaseUrl + path}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(ttppRequestR2))
        .setHeader(requestHeaders: _*)
        .execute[Either[ProxyEnvelopeError, TtpCancelSuccessfulResponse]]
    )
  }

  def fullAmendTtp(
    request: TtpFullAmendRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpFullAmendSuccessfulResponse] = {

    implicit def httpReads: HttpReads[Either[ProxyEnvelopeError, TtpFullAmendSuccessfulResponse]] =
      httpReadsBuilderForFullAmend.httpReads(logger, _.toStringSafeToLogInProd)

    val path = "/debts/time-to-pay/full-amend"

    val url = url"${appConfig.ttpBaseUrl + path}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(request))
        .setHeader(requestHeaders: _*)
        .execute[Either[ProxyEnvelopeError, TtpFullAmendSuccessfulResponse]]
    )
  }

  def informTtp(
    ttppInformRequest: InformRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpInformSuccessfulResponse] = {

    implicit def httpReads: HttpReads[Either[ProxyEnvelopeError, TtpInformSuccessfulResponse]] =
      httpReadsBuilderForInform.httpReads(logger, _.toStringSafeToLogInProd)

    val path = "/debts/time-to-pay/inform"

    val url = url"${appConfig.ttpBaseUrl + path}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(ttppInformRequest)(InformRequest.format(featureSwitch)))
        .setHeader(requestHeaders: _*)
        .execute[Either[ProxyEnvelopeError, TtpInformSuccessfulResponse]]
    )
  }

  private def requestHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val correlationId: String = getOrGenerateCorrelationId

    if (featureSwitch.internalAuthEnabled.enabled) {
      Seq(
        "Authorization" -> appConfig.internalAuthToken,
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
