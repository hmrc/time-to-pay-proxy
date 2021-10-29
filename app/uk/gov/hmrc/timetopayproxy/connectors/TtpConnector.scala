/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope
import cats.syntax.either._
import com.google.inject.ImplementedBy

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.timetopayproxy.config.AppConfig

import java.util.UUID

@ImplementedBy(classOf[DefaultTtpConnector])
trait TtpConnector {
  def generateQuote(ttppRequest: GenerateQuoteRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[GenerateQuoteResponse]
  def getExistingQuote(customerReference: CustomerReference, planId: PlanId)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[ViewPlanResponse]
  def updatePlan(updatePlanRequest: UpdatePlanRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier ): TtppEnvelope[UpdatePlanResponse]
  def createPlan(createPlanRequest: CreatePlanRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[CreatePlanResponse]
}

@Singleton
class DefaultTtpConnector @Inject()(appConfig: AppConfig, httpClient: HttpClient) extends TtpConnector {

  val ifHeaders = (guid: String) => Seq(
    "Authorization" -> s"Bearer ${appConfig.ttpToken}",
    "CorrelationId" -> s"$guid"
  )

  def generateQuote(ttppRequest: GenerateQuoteRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[GenerateQuoteResponse] = {
    val path = "individuals/debts/time-to-pay/quote"
    val url = s"${appConfig.ttpBaseUrl}/$path"

    TtppEnvelope {
      httpClient
        .POST[GenerateQuoteRequest, GenerateQuoteResponse](url, ttppRequest, ifHeaders(UUID.randomUUID().toString))
        .map(r => r.asRight[ConnectorError])
        .recover {
          case e: HttpException => ConnectorError(e.responseCode, e.message).asLeft[GenerateQuoteResponse]
          case e: UpstreamErrorResponse => ConnectorError(e.statusCode, e.getMessage()).asLeft[GenerateQuoteResponse]
        }
    }
  }


  override def getExistingQuote(customerReference: CustomerReference, planId: PlanId)
                               (implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[ViewPlanResponse] = {
    val path = s"individuals/time-to-pay/quote/${customerReference.value}/${planId.value}"
    val url = s"${appConfig.ttpBaseUrl}/$path"

    TtppEnvelope(
      httpClient.GET[ViewPlanResponse](url)
        .map(r => r.asRight[ConnectorError])
        .recover {
          case e: HttpException => ConnectorError(e.responseCode, e.message).asLeft[ViewPlanResponse]
          case e: UpstreamErrorResponse => ConnectorError(e.statusCode, e.getMessage()).asLeft[ViewPlanResponse]
        }
    )

  }

  def updatePlan(
                   updatePlanRequest: UpdatePlanRequest
                 )
                 (
                   implicit ec: ExecutionContext,
                   hc: HeaderCarrier
                 ): TtppEnvelope[UpdatePlanResponse] = {
    val path = "individuals/time-to-pay/quote"
    val url = s"${appConfig.ttpBaseUrl}/$path/${updatePlanRequest.customerReference.value}/${updatePlanRequest.planId.value}"

    val response: Future[Either[ConnectorError, UpdatePlanResponse]] =
      httpClient
        .PUT[UpdatePlanRequest, UpdatePlanResponse](url, updatePlanRequest)
        .map(r => r.asRight[ConnectorError])
        .recover {
          case e: HttpException => ConnectorError(e.responseCode, e.message).asLeft[UpdatePlanResponse]
          case e: UpstreamErrorResponse => ConnectorError(e.statusCode, e.getMessage()).asLeft[UpdatePlanResponse]
        }

    TtppEnvelope(response)
  }

  override def createPlan(createPlanRequest: CreatePlanRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[CreatePlanResponse] = {
    val path = "individuals/debts/time-to-pay/quote/arrangement"
    val url = s"${appConfig.ttpBaseUrl}/$path"

    TtppEnvelope {
      httpClient
        .POST[CreatePlanRequest, CreatePlanResponse](url, createPlanRequest, ifHeaders(UUID.randomUUID().toString))
        .map(r => r.asRight[ConnectorError])
        .recover {
          case e: HttpException => ConnectorError(e.responseCode, e.message).asLeft[CreatePlanResponse]
          case e: UpstreamErrorResponse => ConnectorError(e.statusCode, e.getMessage()).asLeft[CreatePlanResponse]
        }
    }

  }
}
