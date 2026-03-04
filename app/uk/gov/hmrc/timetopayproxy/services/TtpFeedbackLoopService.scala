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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpFeedbackLoopConnector
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel.{ TtpCancelRequest, TtpCancelRequestR2, TtpCancelSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpfullamend.{ TtpFullAmendRequest, TtpFullAmendSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform.{ InformRequest, TtpInformSuccessfulResponse }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class TtpFeedbackLoopService @Inject() (ttppConnector: TtpFeedbackLoopConnector) {
  def cancelTtp(
    ttppRequest: TtpCancelRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpCancelSuccessfulResponse] =
    ttppConnector.cancelTtp(ttppRequest)

  def cancelTtpR2(
    ttppRequestR2: TtpCancelRequestR2
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpCancelSuccessfulResponse] =
    ttppConnector.cancelTtpR2(ttppRequestR2)

  def informTtp(
    ttppInformRequest: InformRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpInformSuccessfulResponse] =
    ttppConnector.informTtp(ttppInformRequest)

  def fullAmendTtp(
    request: TtpFullAmendRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[TtpFullAmendSuccessfulResponse] =
    ttppConnector.fullAmendTtp(request)
}
