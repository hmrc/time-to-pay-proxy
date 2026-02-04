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

package uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel

import cats.data.NonEmptyList
import play.api.libs.json.{ Format, Json, OFormat }
import uk.gov.hmrc.timetopayproxy.models.saonly.common.{ SaOnlyInstalment, TransitionedIndicator }
import uk.gov.hmrc.timetopayproxy.models.{ ChannelIdentifier, Identification }
import uk.gov.hmrc.timetopayproxy.utils.json.CatsNonEmptyListJson

final case class TtpCancelRequest(
  identifications: NonEmptyList[Identification],
  paymentPlan: TtpCancelPaymentPlan,
  instalments: NonEmptyList[SaOnlyInstalment],
  channelIdentifier: ChannelIdentifier,
  // Unlike the FullAmend request, CDCS requested this field to be optional due to delivery constraints.
  transitioned: Option[TransitionedIndicator]
)

object TtpCancelRequest {
  implicit val format: OFormat[TtpCancelRequest] = {
    implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat[T]

    Json.format[TtpCancelRequest]
  }
}

// TODO DTD-4258: These R2 classes should replace the non R2 classes when the feature switch is removed
case class TtpCancelRequestR2(
  identifications: NonEmptyList[Identification],
  paymentPlan: TtpCancelPaymentPlanR2,
  instalments: NonEmptyList[SaOnlyInstalment],
  channelIdentifier: ChannelIdentifier,
  // Unlike the FullAmend request, CDCS requested this field to be optional due to delivery constraints.
  transitioned: Option[TransitionedIndicator]
)

object TtpCancelRequestR2 {
  implicit val format: OFormat[TtpCancelRequestR2] = {
    implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat[T]

    Json.format[TtpCancelRequestR2]
  }
}
