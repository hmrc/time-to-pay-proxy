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
import play.api.libs.json.{ Json, Reads }
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{ HeaderCarrier, StringContextOps }
import uk.gov.hmrc.timetopayproxy.config.AppConfig
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.affordablequotes.{ AffordableQuoteResponse, AffordableQuotesRequest }

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
class DefaultTtpConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)
    extends TtpConnector with HttpParser[TimeToPayError] {

  private val logger: RequestAwareLogger = new RequestAwareLogger(classOf[DefaultTtpConnector])

  val headers: String => Seq[(String, String)] = (guid: String) =>
    if (appConfig.useIf) {
      Seq(
        "Authorization" -> s"Bearer ${appConfig.ttpToken}",
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
    val path = if (appConfig.useIf) "/individuals/debts/time-to-pay/quote" else "/debts/time-to-pay/quote"

    val pathWithQueryParameters = path + makeQueryString(queryParams)

    val url = url"${appConfig.ttpBaseUrl + pathWithQueryParameters}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(ttppRequest))
        .setHeader(headers(getOrGenerateCorrelationId): _*)
        .execute[Either[TtppError, GenerateQuoteResponse]]
    )
  }

  override def getExistingQuote(customerReference: CustomerReference, planId: PlanId)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[ViewPlanResponse] =
    getExistingQuoteG[ViewPlanResponse](customerReference, planId)

  private def getExistingQuoteG[Response](customerReference: CustomerReference, planId: PlanId)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier,
    reads: Reads[Response]
  ): TtppEnvelope[Response] = {
    val path =
      if (appConfig.useIf)
        s"/individuals/time-to-pay/quote/${customerReference.value}/${planId.value}"
      else
        s"/debts/time-to-pay/quote/${customerReference.value}/${planId.value}"

    val url = url"${appConfig.ttpBaseUrl + path}"

    EitherT(
      httpClient
        .get(url)
        .execute[Either[TtppError, Response]]
    )
  }

  def updatePlan(
    updatePlanRequest: UpdatePlanRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[UpdatePlanResponse] = {
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
        .execute[Either[TtppError, UpdatePlanResponse]]
    )
  }

  override def createPlan(
    createPlanRequest: CreatePlanRequest,
    queryParams: Seq[(String, String)] = Seq.empty
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[CreatePlanResponse] = {
    logger.info(s"Create plan instalments: \n${Json.toJson(createPlanRequest.instalments)}")

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
        .execute[Either[TtppError, CreatePlanResponse]]
    )
  }

  def getAffordableQuotes(
    affordableQuotesRequest: AffordableQuotesRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[AffordableQuoteResponse] = {
    val path =
      if (appConfig.useIf) "/individuals/time-to-pay/affordability/affordable-quotes"
      else "/debts/time-to-pay/affordability/affordable-quotes"

    val url = url"${appConfig.ttpBaseUrl + path}"

    EitherT(
      httpClient
        .post(url)
        .withBody(Json.toJson(affordableQuotesRequest))
        .execute[Either[TtppError, AffordableQuoteResponse]]
    )
  }

  private def makeQueryString(queryParams: Seq[(String, String)]): String = {
    val paramPairs = queryParams.map { case (k, v) => s"$k=${URLEncoder.encode(v, "utf-8")}" }
    if (paramPairs.isEmpty) "" else paramPairs.mkString("?", "&", "")
  }
}
