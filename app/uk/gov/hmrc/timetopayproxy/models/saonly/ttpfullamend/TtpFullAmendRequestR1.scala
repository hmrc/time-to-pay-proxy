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

package uk.gov.hmrc.timetopayproxy.models.saonly.ttpfullamend

import cats.data.NonEmptyList
import play.api.libs.json.{ Format, Json, OFormat }
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.utils.json.CatsNonEmptyListJson

final case class TtpFullAmendRequestR1(
  identifications: NonEmptyList[Identification],
  paymentPlan: SaOnlyPaymentPlan,
  instalments: NonEmptyList[SaOnlyInstalment],
  channelIdentifier: ChannelIdentifier,
  transitioned: TransitionedIndicator
)
object TtpFullAmendRequestR1 {
  implicit val format: OFormat[TtpFullAmendRequestR1] = {
    implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat[T]

    Json.format[TtpFullAmendRequestR1]
  }
}
case class TtpFullAmendRequestR2(
  identifications: NonEmptyList[Identification],
  originalPaymentPlan: OriginalPaymentPlan,
  newPaymentPlan: NewPaymentPlan,
  instalments: NonEmptyList[SaOnlyInstalment],
  channelIdentifier: ChannelIdentifier,
  transitioned: TransitionedIndicator
)

object TtpFullAmendRequestR2 {
  implicit val format: OFormat[TtpFullAmendRequestR2] = {
    implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat[T]

    Json.format[TtpFullAmendRequestR2]
  }
}
