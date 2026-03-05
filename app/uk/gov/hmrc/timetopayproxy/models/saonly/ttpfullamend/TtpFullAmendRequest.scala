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
import play.api.libs.json.{ Format, Json, OFormat, OWrites, Reads }
import uk.gov.hmrc.timetopayproxy.config.FeatureSwitch
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.utils.json.CatsNonEmptyListJson

sealed trait FullAmendRequest

object FullAmendRequest {
  private def reads(featureSwitch: FeatureSwitch): Reads[FullAmendRequest] = Reads { jsValue =>
    if (featureSwitch.saRelease2Enabled.enabled)
      TtpFullAmendRequestR2.format.reads(jsValue)
    else
      TtpFullAmendRequest.format.reads(jsValue)
  }

  private val writes: OWrites[FullAmendRequest] = OWrites {
    case r1: TtpFullAmendRequest   => TtpFullAmendRequest.format.writes(r1)
    case r2: TtpFullAmendRequestR2 => TtpFullAmendRequestR2.format.writes(r2)
  }

  def format(featureSwitch: FeatureSwitch): OFormat[FullAmendRequest] = OFormat(reads(featureSwitch), writes)
}

final case class TtpFullAmendRequest(
  identifications: NonEmptyList[Identification],
  paymentPlan: SaOnlyPaymentPlan,
  instalments: NonEmptyList[SaOnlyInstalment],
  channelIdentifier: ChannelIdentifier,
  transitioned: TransitionedIndicator
) extends FullAmendRequest

object TtpFullAmendRequest {
  implicit val format: OFormat[TtpFullAmendRequest] = {
    implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat[T]

    Json.format[TtpFullAmendRequest]
  }
}

case class TtpFullAmendRequestR2(
  identifications: NonEmptyList[Identification],
  originalPaymentPlan: OriginalPaymentPlan,
  newPaymentPlan: NewPaymentPlan,
  instalments: NonEmptyList[SaOnlyInstalment],
  channelIdentifier: ChannelIdentifier,
  transitioned: TransitionedIndicator
) extends FullAmendRequest

object TtpFullAmendRequestR2 {
  implicit val format: OFormat[TtpFullAmendRequestR2] = {
    implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat[T]

    Json.format[TtpFullAmendRequestR2]
  }
}
