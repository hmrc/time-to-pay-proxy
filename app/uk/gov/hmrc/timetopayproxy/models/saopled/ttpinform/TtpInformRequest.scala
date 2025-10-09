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

package uk.gov.hmrc.timetopayproxy.models.saopled.ttpinform

import cats.data.NonEmptyList
import play.api.libs.json.{ Format, Json, OFormat }
import uk.gov.hmrc.timetopayproxy.models.{ ChannelIdentifier, Identification }
import uk.gov.hmrc.timetopayproxy.models.saopled.common.{ SaOpLedInstalment, TransitionedIndicator }
import uk.gov.hmrc.timetopayproxy.utils.json.CatsNonEmptyListJson

final case class TtpInformRequest(
  identifications: NonEmptyList[Identification],
  paymentPlan: TtpInformPaymentPlan,
  instalments: NonEmptyList[SaOpLedInstalment],
  channelIdentifier: ChannelIdentifier,
  transitioned: Option[TransitionedIndicator]
)

object TtpInformRequest {
  implicit val format: OFormat[TtpInformRequest] = {
    implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat

    Json.format[TtpInformRequest]
  }
}
