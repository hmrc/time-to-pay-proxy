/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.services

import com.google.inject.ImplementedBy
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpConnector
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.affordablequotes.{ AffordableQuoteResponse, AffordableQuotesRequest }
import uk.gov.hmrc.timetopayproxy.models.saopledttp.{ CancelRequest, CancelResponse }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[DefaultTTPQuoteService])
trait TTPQuoteService {
  def generateQuote(
    timeToPayRequest: GenerateQuoteRequest,
    requestQuery: Map[String, Seq[String]]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[GenerateQuoteResponse]

  def getExistingPlan(customerReference: CustomerReference, planId: PlanId)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[ViewPlanResponse]

  def updatePlan(
    updatePlanRequest: UpdatePlanRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[UpdatePlanResponse]

  def createPlan(
    createPlanRequest: CreatePlanRequest,
    requestQuery: Map[String, Seq[String]]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[CreatePlanResponse]

  def getAffordableQuotes(
    affordableQuotesRequest: AffordableQuotesRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[AffordableQuoteResponse]

  def cancelPlan(
    cancelRequest: CancelRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[CancelResponse]
}

@Singleton
class DefaultTTPQuoteService @Inject() (ttpConnector: TtpConnector) extends TTPQuoteService {

  def generateQuote(timeToPayRequest: GenerateQuoteRequest, requestQuery: Map[String, Seq[String]])(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[GenerateQuoteResponse] =
    ttpConnector.generateQuote(timeToPayRequest, requestQuery.view.mapValues(_.head).toSeq)

  override def getExistingPlan(customerReference: CustomerReference, planId: PlanId)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[ViewPlanResponse] =
    ttpConnector.getExistingQuote(customerReference, planId)

  def updatePlan(
    updatePlanRequest: UpdatePlanRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[UpdatePlanResponse] =
    ttpConnector.updatePlan(updatePlanRequest)

  override def createPlan(
    createPlanRequest: CreatePlanRequest,
    requestQuery: Map[String, Seq[String]]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[CreatePlanResponse] =
    ttpConnector.createPlan(createPlanRequest, requestQuery.view.mapValues(_.head).toSeq)

  def getAffordableQuotes(
    affordableQuotesRequest: AffordableQuotesRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[AffordableQuoteResponse] =
    ttpConnector.getAffordableQuotes(affordableQuotesRequest)

  def cancelPlan(
    cancelRequest: CancelRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[CancelResponse] =
    ttpConnector.cancelPlan(cancelRequest)
}
