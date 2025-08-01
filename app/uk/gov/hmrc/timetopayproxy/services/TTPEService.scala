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

package uk.gov.hmrc.timetopayproxy.services

import com.google.inject.ImplementedBy
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpeConnector
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.chargeInfoApi.{ ChargeInfoRequest, ChargeInfoResponse }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[DefaultTTPEService])
trait TTPEService {
  def checkChargeInfo(
    chargeInfoRequest: ChargeInfoRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[ChargeInfoResponse]
}

@Singleton
class DefaultTTPEService @Inject() (ttpeConnector: TtpeConnector) extends TTPEService {
  def checkChargeInfo(
    chargeInfoRequest: ChargeInfoRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[ChargeInfoResponse] =
    ttpeConnector.checkChargeInfo(chargeInfoRequest)
}
