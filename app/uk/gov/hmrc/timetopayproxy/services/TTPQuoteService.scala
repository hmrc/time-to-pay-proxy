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

package uk.gov.hmrc.timetopayproxy.services

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpConnector
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope

import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[DefaultTTPQuoteService])
trait TTPQuoteService {
  def generateQuote(timeToPayRequest: GenerateQuoteRequest)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[GenerateQuoteResponse]

  def getExistingPlan(customerReference: CustomerReference, pegaId: PegaPlanId)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[RetrievePlanResponse]

  def updateQuote(updateQuoteRequest: UpdateQuoteRequest)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[UpdateQuoteResponse]
}

@Singleton
class DefaultTTPQuoteService @Inject()(ttpConnector: TtpConnector)
    extends TTPQuoteService {

  def generateQuote(timeToPayRequest: GenerateQuoteRequest)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[GenerateQuoteResponse] =
    ttpConnector.generateQuote(timeToPayRequest)

  override def getExistingPlan(customerReference: CustomerReference, pegaPlanId: PegaPlanId)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[RetrievePlanResponse] = {
    ttpConnector.getExistingQuote(customerReference, pegaPlanId)
  }

  def updateQuote(updateQuoteRequest: UpdateQuoteRequest)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): TtppEnvelope[UpdateQuoteResponse] =
    ttpConnector.updateQuote(updateQuoteRequest)
}
