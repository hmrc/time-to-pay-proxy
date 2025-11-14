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
import uk.gov.hmrc.timetopayproxy.connectors.util.HttpReadsWithLoggingBuilder
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, StringContextOps }
import uk.gov.hmrc.timetopayproxy.config.{ AppConfig, FeatureSwitch }
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.affordablequotes.{ AffordableQuoteResponse, AffordableQuotesRequest }
import uk.gov.hmrc.timetopayproxy.models.error.ProxyEnvelopeError
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope

import java.net.URLEncoder
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[DefaultTtpConnector])
trait TtpConnector {
  def generateQuote(
    ttppRequest: GenerateQuoteRequest,
    queryParams: Seq[(String, String)] = Seq.empty
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[GenerateQuoteResponse]

  def getExistingQuote(customerReference: CustomerReference, planId: PlanId)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[ViewPlanResponse]

  def updatePlan(
    updatePlanRequest: UpdatePlanRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[UpdatePlanResponse]

  def createPlan(
    createPlanRequest: CreatePlanRequest,
    queryParams: Seq[(String, String)] = Seq.empty
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[CreatePlanResponse]

  def getAffordableQuotes(
    affordableQuotesRequest: AffordableQuotesRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[AffordableQuoteResponse]
}

@Singleton
class DefaultTtpConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2, featureSwitch: FeatureSwitch)
    extends TtpConnector {

  private val logger: RequestAwareLogger = new RequestAwareLogger(classOf[DefaultTtpConnector])

  private val httpReadsBuilderForGenerateQuote: HttpReadsWithLoggingBuilder[ProxyEnvelopeError, GenerateQuoteResponse] =
    HttpReadsWithLoggingBuilder
      .empty[ProxyEnvelopeError, GenerateQuoteResponse]
      .orSuccess[GenerateQuoteResponse](201)
      .orErrorTransformed[TimeToPayError](400, ttpError => ttpError.toConnectorError(status = 400))

  private val httpReadsBuilderForViewPlan: HttpReadsWithLoggingBuilder[ProxyEnvelopeError, ViewPlanResponse] =
    HttpReadsWithLoggingBuilder
      .empty[ProxyEnvelopeError, ViewPlanResponse]
      .orSuccess[ViewPlanResponse](200)
      .orErrorTransformed[TimeToPayError](400, ttpError => ttpError.toConnectorError(status = 400))

  private val httpReadsBuilderForUpdatePlan: HttpReadsWithLoggingBuilder[ProxyEnvelopeError, UpdatePlanResponse] =
    HttpReadsWithLoggingBuilder
      .empty[ProxyEnvelopeError, UpdatePlanResponse]
      .orSuccess[UpdatePlanResponse](200)
      .orErrorTransformed[TimeToPayError](400, ttpError => ttpError.toConnectorError(status = 400))
      .orErrorTransformed[TimeToPayError](409, ttpError => ttpError.toConnectorError(status = 409))

  private val httpReadsBuilderForCreatePlan: HttpReadsWithLoggingBuilder[ProxyEnvelopeError, CreatePlanResponse] =
    HttpReadsWithLoggingBuilder
      .empty[ProxyEnvelopeError, CreatePlanResponse]
      .orSuccess[CreatePlanResponse](201)
      .orErrorTransformed[TimeToPayError](400, ttpError => ttpError.toConnectorError(status = 400))

  private val httpReadsBuilderForAffordableQuotes
    : HttpReadsWithLoggingBuilder[ProxyEnvelopeError, AffordableQuoteResponse] =
    HttpReadsWithLoggingBuilder
      .empty[ProxyEnvelopeError, AffordableQuoteResponse]
      .orSuccess[AffordableQuoteResponse](200)
      .orErrorTransformed[TimeToPayError](400, ttpError => ttpError.toConnectorError(status = 400))

  val headers: String => Seq[(String, String)] = (guid: String) =>
    if (featureSwitch.internalAuthEnabled.enabled) {
      Seq(
        "Authorization" -> s"$appConfig.internalAuthToken",
        "CorrelationId" -> s"$guid"
      )
    } else {
      Seq()
    }

  private def getOrGenerateCorrelationId(implicit hc: HeaderCarrier): String =
    (hc.headers(Seq("CorrelationId")) ++ hc.extraHeaders)
      .toMap[String, String]
      .getOrElse("CorrelationId", UUID.randomUUID().toString)

  def generateQuote(
    ttppRequest: GenerateQuoteRequest,
    queryParams: Seq[(String, String)] = Seq.empty
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[GenerateQuoteResponse] = {

    implicit def httpReads: HttpReads[Either[ProxyEnvelopeError, GenerateQuoteResponse]] =
      httpReadsBuilderForGenerateQuote.httpReads(logger)

    val path = if (appConfig.useIf) "/individuals/debts/time-to-pay/quote" else "/debts/time-to-pay/quote"

    val pathWithQueryParameters = path + makeQueryString(queryParams)

    val url = url"${appConfig.ttpBaseUrl + pathWithQueryParameters}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(ttppRequest))
        .setHeader(headers(getOrGenerateCorrelationId): _*)
        .execute[Either[ProxyEnvelopeError, GenerateQuoteResponse]]
    )
  }

  override def getExistingQuote(customerReference: CustomerReference, planId: PlanId)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[ViewPlanResponse] = {

    implicit def httpReads: HttpReads[Either[ProxyEnvelopeError, ViewPlanResponse]] =
      httpReadsBuilderForViewPlan.httpReads(logger)

    val path =
      if (appConfig.useIf)
        s"/individuals/time-to-pay/quote/${customerReference.value}/${planId.value}"
      else
        s"/debts/time-to-pay/quote/${customerReference.value}/${planId.value}"

    val url = url"${appConfig.ttpBaseUrl + path}"

    EitherT(
      httpClient
        .get(url)
        .execute[Either[ProxyEnvelopeError, ViewPlanResponse]]
    )
  }

  def updatePlan(
    updatePlanRequest: UpdatePlanRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[UpdatePlanResponse] = {

    implicit def httpReads: HttpReads[Either[ProxyEnvelopeError, UpdatePlanResponse]] =
      httpReadsBuilderForUpdatePlan.httpReads(logger)

    val path = if (appConfig.useIf) "individuals/time-to-pay/quote" else "debts/time-to-pay/quote"

    val urlAsString = List(
      appConfig.ttpBaseUrl,
      path,
      updatePlanRequest.customerReference.value,
      updatePlanRequest.planId.value
    ).mkString("/")

    val url = url"$urlAsString"

    EitherT(
      httpClient
        .put(url)
        .withBody(Json.toJson(updatePlanRequest))
        .execute[Either[ProxyEnvelopeError, UpdatePlanResponse]]
    )
  }

  override def createPlan(
    createPlanRequest: CreatePlanRequest,
    queryParams: Seq[(String, String)] = Seq.empty
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[CreatePlanResponse] = {
    logger.info(s"Create plan instalments: \n${Json.toJson(createPlanRequest.instalments)}")

    implicit def httpReads: HttpReads[Either[ProxyEnvelopeError, CreatePlanResponse]] =
      httpReadsBuilderForCreatePlan.httpReads(logger)

    val path =
      if (appConfig.useIf) "/individuals/debts/time-to-pay/quote/arrangement"
      else "/debts/time-to-pay/quote/arrangement"

    val pathWithQueryParameters = path + makeQueryString(queryParams)

    val url = url"${appConfig.ttpBaseUrl + pathWithQueryParameters}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(createPlanRequest))
        .setHeader(headers(getOrGenerateCorrelationId): _*)
        .execute[Either[ProxyEnvelopeError, CreatePlanResponse]]
    )
  }

  def getAffordableQuotes(
    affordableQuotesRequest: AffordableQuotesRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[AffordableQuoteResponse] = {

    implicit def httpReads: HttpReads[Either[ProxyEnvelopeError, AffordableQuoteResponse]] =
      httpReadsBuilderForAffordableQuotes.httpReads(logger)

    val path =
      if (appConfig.useIf) "/individuals/time-to-pay/affordability/affordable-quotes"
      else "/debts/time-to-pay/affordability/affordable-quotes"

    val url = url"${appConfig.ttpBaseUrl + path}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(affordableQuotesRequest))
        .execute[Either[ProxyEnvelopeError, AffordableQuoteResponse]]
    )
  }

  private def makeQueryString(queryParams: Seq[(String, String)]): String = {
    val paramPairs = queryParams.map { case (k, v) => s"$k=${URLEncoder.encode(v, "utf-8")}" }
    if (paramPairs.isEmpty) "" else paramPairs.mkString("?", "&", "")
  }
}
